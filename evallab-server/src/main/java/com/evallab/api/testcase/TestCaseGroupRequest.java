package com.evallab.api.testcase;

import jakarta.validation.constraints.NotBlank;

public record TestCaseGroupRequest(@NotBlank String groupName) {
}
