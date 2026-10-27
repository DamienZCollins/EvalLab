package com.evallab.core.testcase;

import java.util.List;
import java.util.Optional;

/** 用例持久化端口。 */
public interface TestCaseStore {

    TestCase insert(TestCaseDraft draft);

    TestCase update(long testcaseId, TestCaseDraft draft);

    void delete(long testcaseId);

    Optional<TestCase> findById(long testcaseId);

    List<TestCase> find(TestCaseQuery query);

    List<TestCaseGroup> findGroups();

    TestCaseGroup createGroup(String groupName);

    boolean groupExists(long groupId);
}
