package com.evallab.core.testcase;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 用例管理。编辑时 version 自增，供评测结果快照。 */
@Service
public class TestCaseService {

    private final TestCaseStore store;

    public TestCaseService(TestCaseStore store) {
        this.store = store;
    }

    public TestCase create(TestCaseDraft draft) {
        validate(draft);
        return store.insert(draft);
    }

    public TestCase update(long testcaseId, TestCaseDraft draft) {
        store.findById(testcaseId).orElseThrow(() -> new TestCaseNotFoundException(testcaseId));
        validate(draft);
        return store.update(testcaseId, draft);
    }

    public void delete(long testcaseId) {
        store.findById(testcaseId).orElseThrow(() -> new TestCaseNotFoundException(testcaseId));
        store.delete(testcaseId);
    }

    public TestCase get(long testcaseId) {
        return store.findById(testcaseId).orElseThrow(() -> new TestCaseNotFoundException(testcaseId));
    }

    public List<TestCase> query(TestCaseQuery query) {
        return store.find(query == null ? TestCaseQuery.all() : query);
    }

    public List<TestCaseGroup> groups() {
        return store.findGroups();
    }

    public TestCaseGroup createGroup(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            throw new InvalidTestCaseException(List.of("group_name 不能为空"));
        }
        return store.createGroup(groupName.trim());
    }

    private void validate(TestCaseDraft draft) {
        List<String> violations = new ArrayList<>(TestCaseValidator.validate(draft));
        if (draft != null) {
            violations.addAll(TestCaseValidator.validateJudgingScorer(draft.matchType(), draft.expectedTools()));
            if (draft.groupId() != null && !store.groupExists(draft.groupId())) {
                violations.add("分组不存在: " + draft.groupId());
            }
        }
        if (!violations.isEmpty()) {
            throw new InvalidTestCaseException(violations);
        }
    }
}
