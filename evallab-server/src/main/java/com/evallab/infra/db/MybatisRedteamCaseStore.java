package com.evallab.infra.db;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.evallab.core.redteam.RedteamAttackType;
import com.evallab.core.redteam.RedteamCase;
import com.evallab.core.redteam.RedteamCaseDraft;
import com.evallab.core.redteam.RedteamCaseNotFoundException;
import com.evallab.core.redteam.RedteamCaseStore;
import com.evallab.infra.db.entity.EvalRedteamCaseEntity;
import com.evallab.infra.db.mapper.EvalRedteamCaseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisRedteamCaseStore implements RedteamCaseStore {

    private final EvalRedteamCaseMapper mapper;
    private final ObjectMapper objectMapper;

    public MybatisRedteamCaseStore(EvalRedteamCaseMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public RedteamCase insert(long testcaseId, RedteamCaseDraft draft) {
        EvalRedteamCaseEntity entity = new EvalRedteamCaseEntity();
        entity.setTestcaseId(testcaseId);
        applyDraft(entity, draft);
        mapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public RedteamCase update(long redteamCaseId, RedteamCaseDraft draft) {
        EvalRedteamCaseEntity existing = mapper.selectById(redteamCaseId);
        if (existing == null) {
            throw new RedteamCaseNotFoundException(redteamCaseId);
        }
        applyDraft(existing, draft);
        mapper.updateById(existing);
        return toDomain(existing);
    }

    @Override
    public void delete(long redteamCaseId) {
        mapper.deleteById(redteamCaseId);
    }

    @Override
    public Optional<RedteamCase> findById(long redteamCaseId) {
        return Optional.ofNullable(mapper.selectById(redteamCaseId)).map(this::toDomain);
    }

    @Override
    public List<RedteamCase> find(RedteamAttackType attackType) {
        var wrapper = Wrappers.<EvalRedteamCaseEntity>lambdaQuery();
        if (attackType != null) {
            wrapper.eq(EvalRedteamCaseEntity::getAttackType, attackType.name());
        }
        wrapper.orderByAsc(EvalRedteamCaseEntity::getCreatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    private void applyDraft(EvalRedteamCaseEntity entity, RedteamCaseDraft draft) {
        entity.setName(draft.name());
        entity.setAttackType(draft.attackType().name());
        entity.setAttackPayload(draft.attackPayload());
        entity.setExpectedDefense(draft.expectedDefense());
        entity.setForbiddenMarkers(JsonColumns.write(objectMapper, draft.forbiddenMarkers()));
        entity.setIsEnabled(!Boolean.FALSE.equals(draft.enabled()));
    }

    private RedteamCase toDomain(EvalRedteamCaseEntity entity) {
        return new RedteamCase(entity.getRedteamCaseId(), entity.getTestcaseId(), entity.getName(),
                RedteamAttackType.valueOf(entity.getAttackType()), entity.getAttackPayload(),
                entity.getExpectedDefense(),
                JsonColumns.readStringList(objectMapper, entity.getForbiddenMarkers()),
                !Boolean.FALSE.equals(entity.getIsEnabled()));
    }
}
