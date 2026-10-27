package com.evallab.api.testcase;

import com.evallab.core.testcase.TestCase;
import com.evallab.core.testcase.TestCaseDraft;
import com.evallab.core.testcase.TestCaseGroup;
import org.springframework.stereotype.Component;

@Component
public class TestCaseMapper {

    public TestCaseDraft toDraft(TestCaseRequest request) {
        return new TestCaseDraft(request.groupId(), request.name(), request.input(),
                request.matchType(), request.expectedOutput(), request.expectedTools(),
                request.faultInjection(), request.tags(), request.enabled());
    }

    public TestCaseResponse toResponse(TestCase testCase) {
        return new TestCaseResponse(testCase.testcaseId(), testCase.groupId(), testCase.name(),
                testCase.input(), testCase.matchType(), testCase.expectedOutput(),
                testCase.expectedTools(), testCase.faultInjection(), testCase.tags(),
                testCase.version(), testCase.enabled());
    }

    public TestCaseGroupResponse toResponse(TestCaseGroup group) {
        return new TestCaseGroupResponse(group.groupId(), group.groupName());
    }
}
