package com.evallab.infra.db;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.evallab.contract.trace.FaultInjection;
import com.evallab.core.testcase.MatchType;
import com.evallab.core.testcase.TestCase;
import com.evallab.core.testcase.TestCaseDraft;
import com.evallab.core.testcase.TestCaseGroup;
import com.evallab.core.testcase.TestCaseNotFoundException;
import com.evallab.core.testcase.TestCaseQuery;
import com.evallab.core.testcase.TestCaseStore;
import com.evallab.infra.db.entity.EvalTestcaseEntity;
import com.evallab.infra.db.entity.EvalTestcaseGroupEntity;
import com.evallab.infra.db.mapper.EvalTestcaseGroupMapper;
import com.evallab.infra.db.mapper.EvalTestcaseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisTestCaseStore implements TestCaseStore {

    private final EvalTestcaseMapper testcaseMapper;
    private final EvalTestcaseGroupMapper groupMapper;
    private final ObjectMapper objectMapper;

    public MybatisTestCaseStore(EvalTestcaseMapper testcaseMapper, EvalTestcaseGroupMapper groupMapper,
                                ObjectMapper objectMapper) {
        this.testcaseMapper = testcaseMapper;
        this.groupMapper = groupMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public TestCase insert(TestCaseDraft draft) {
        EvalTestcaseEntity entity = new EvalTestcaseEntity();
        applyDraft(entity, draft);
        entity.setVersion(1);
        testcaseMapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public TestCase update(long testcaseId, TestCaseDraft draft) {
        EvalTestcaseEntity existing = testcaseMapper.selectById(testcaseId);
        if (existing == null) {
            throw new TestCaseNotFoundException(testcaseId);
        }
        applyDraft(existing, draft);
        // 编辑即升版，历史评测结果仍指向旧版本快照
        existing.setVersion(existing.getVersion() == null ? 2 : existing.getVersion() + 1);
        testcaseMapper.updateById(existing);
        return toDomain(existing);
    }

    @Override
    public void delete(long testcaseId) {
        testcaseMapper.deleteById(testcaseId);
    }

    @Override
    public Optional<TestCase> findById(long testcaseId) {
        return Optional.ofNullable(testcaseMapper.selectById(testcaseId)).map(this::toDomain);
    }

    @Override
    public List<TestCase> find(TestCaseQuery query) {
        var wrapper = Wrappers.<EvalTestcaseEntity>lambdaQuery();
        if (query.groupId() != null) {
            wrapper.eq(EvalTestcaseEntity::getGroupId, query.groupId());
        }
        if (Boolean.TRUE.equals(query.enabledOnly())) {
            wrapper.eq(EvalTestcaseEntity::getIsEnabled, true);
        }
        wrapper.orderByDesc(EvalTestcaseEntity::getCreatedAt);
        List<EvalTestcaseEntity> entities = testcaseMapper.selectList(wrapper);
        // 标签是 JSON 列，MySQL 端筛选代价高且不可移植，用例量小，在内存中过滤
        return entities.stream()
                .map(this::toDomain)
                .filter(tc -> query.tag() == null || query.tag().isBlank()
                        || (tc.tags() != null && tc.tags().contains(query.tag())))
                .toList();
    }

    @Override
    public List<TestCaseGroup> findGroups() {
        return groupMapper.selectList(Wrappers.<EvalTestcaseGroupEntity>lambdaQuery()
                        .orderByAsc(EvalTestcaseGroupEntity::getGroupName))
                .stream()
                .map(g -> new TestCaseGroup(g.getGroupId(), g.getGroupName()))
                .toList();
    }

    @Override
    public TestCaseGroup createGroup(String groupName) {
        EvalTestcaseGroupEntity entity = new EvalTestcaseGroupEntity();
        entity.setGroupName(groupName);
        try {
            groupMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("分组名已存在: " + groupName);
        }
        return new TestCaseGroup(entity.getGroupId(), entity.getGroupName());
    }

    @Override
    public boolean groupExists(long groupId) {
        return groupMapper.selectById(groupId) != null;
    }

    private void applyDraft(EvalTestcaseEntity entity, TestCaseDraft draft) {
        entity.setGroupId(draft.groupId());
        entity.setName(draft.name());
        entity.setInput(draft.input());
        entity.setMatchType(draft.matchType().name());
        entity.setExpectedOutput(draft.expectedOutput());
        entity.setExpectedTools(JsonColumns.write(objectMapper, draft.expectedTools()));
        entity.setFaultInjection(JsonColumns.write(objectMapper, draft.faultInjection()));
        entity.setTags(JsonColumns.write(objectMapper, draft.tags()));
        entity.setIsEnabled(!Boolean.FALSE.equals(draft.enabled()));
    }

    private TestCase toDomain(EvalTestcaseEntity entity) {
        return new TestCase(
                entity.getTestcaseId(),
                entity.getGroupId(),
                entity.getName(),
                entity.getInput(),
                MatchType.valueOf(entity.getMatchType()),
                entity.getExpectedOutput(),
                JsonColumns.readStringList(objectMapper, entity.getExpectedTools()),
                JsonColumns.read(objectMapper, entity.getFaultInjection(), FaultInjection.class),
                JsonColumns.readStringList(objectMapper, entity.getTags()),
                entity.getVersion() == null ? 1 : entity.getVersion(),
                !Boolean.FALSE.equals(entity.getIsEnabled()));
    }
}
