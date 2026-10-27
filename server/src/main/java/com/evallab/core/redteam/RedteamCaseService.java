package com.evallab.core.redteam;

import com.evallab.core.testcase.MatchType;
import com.evallab.core.testcase.TestCase;
import com.evallab.core.testcase.TestCaseDraft;
import com.evallab.core.testcase.TestCaseGroup;
import com.evallab.core.testcase.TestCaseService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 红队用例管理。
 *
 * <p>红队用例不是独立的一套执行链路：每条红队用例同时落一条 {@code eval_testcase} 到
 * {@value #GROUP} 分组，判定依据是由禁用标记生成的守卫正则（见 {@link RedteamGuard}）。
 * 这样「发起红队评测」就是普通的 {@code POST /api/eval/task} 带 {@code group=redteam}，
 * 评分、归因、Trace、门禁全部照旧，没有第二套执行器。
 */
@Service
public class RedteamCaseService {

    static final String GROUP = "redteam";

    private final RedteamCaseStore store;
    private final TestCaseService testCaseService;

    public RedteamCaseService(RedteamCaseStore store, TestCaseService testCaseService) {
        this.store = store;
        this.testCaseService = testCaseService;
    }

    public RedteamCase create(RedteamCaseDraft draft) {
        validate(draft);
        TestCase testCase = testCaseService.create(testCaseDraft(redteamGroupId(), draft));
        return store.insert(testCase.testcaseId(), draft);
    }

    public RedteamCase update(long redteamCaseId, RedteamCaseDraft draft) {
        RedteamCase existing = get(redteamCaseId);
        validate(draft);
        // 底层用例的分组不能被这次编辑清空，否则它会掉出 redteam 分组
        Long groupId = testCaseService.get(existing.testcaseId()).groupId();
        // 用例编辑即升版，历史红队报告的判定仍指向当时那版
        testCaseService.update(existing.testcaseId(), testCaseDraft(groupId, draft));
        return store.update(redteamCaseId, draft);
    }

    public void delete(long redteamCaseId) {
        RedteamCase existing = get(redteamCaseId);
        store.delete(redteamCaseId);
        // 底层用例一并删除：留着它会被普通评测任务当成一条「永远通过」的红队用例跑
        testCaseService.delete(existing.testcaseId());
    }

    public RedteamCase get(long redteamCaseId) {
        return store.findById(redteamCaseId).orElseThrow(() -> new RedteamCaseNotFoundException(redteamCaseId));
    }

    public List<RedteamCase> query(RedteamAttackType attackType) {
        return store.find(attackType);
    }

    /** 红队用例所在的评测分组 ID；分组不存在时创建。 */
    public long redteamGroupId() {
        return testCaseService.groups().stream()
                .filter(g -> GROUP.equals(g.groupName()))
                .map(TestCaseGroup::groupId)
                .findFirst()
                .orElseGet(this::createGroupId);
    }

    private long createGroupId() {
        try {
            return testCaseService.createGroup(GROUP).groupId();
        } catch (IllegalArgumentException e) {
            // 并发创建撞唯一键：再查一次
            return testCaseService.groups().stream()
                    .filter(g -> GROUP.equals(g.groupName()))
                    .map(TestCaseGroup::groupId)
                    .findFirst()
                    .orElseThrow();
        }
    }

    private static TestCaseDraft testCaseDraft(Long groupId, RedteamCaseDraft draft) {
        List<String> tags = new ArrayList<>();
        tags.add(GROUP);
        tags.add(draft.attackType().name());
        return new TestCaseDraft(groupId, draft.name(), draft.attackPayload(), MatchType.REGEX,
                RedteamGuard.pattern(draft.forbiddenMarkers()), null, null, tags, draft.enabled());
    }

    private static void validate(RedteamCaseDraft draft) {
        List<String> violations = new ArrayList<>();
        if (draft == null) {
            throw new InvalidRedteamCaseException(List.of("请求体为空"));
        }
        if (draft.name() == null || draft.name().isBlank()) {
            violations.add("name 不能为空");
        }
        if (draft.attackType() == null) {
            violations.add("attack_type 不能为空");
        }
        if (draft.attackPayload() == null || draft.attackPayload().isBlank()) {
            violations.add("attack_payload 不能为空");
        }
        if (draft.expectedDefense() == null || draft.expectedDefense().isBlank()) {
            violations.add("expected_defense 不能为空");
        }
        if (draft.forbiddenMarkers() == null || draft.forbiddenMarkers().isEmpty()) {
            violations.add("forbidden_markers 不能为空：没有禁用标记就无法判定防御是否成功");
        } else if (draft.forbiddenMarkers().stream().anyMatch(m -> m == null || m.isBlank())) {
            violations.add("forbidden_markers 不能包含空标记");
        }
        if (!violations.isEmpty()) {
            throw new InvalidRedteamCaseException(violations);
        }
    }
}
