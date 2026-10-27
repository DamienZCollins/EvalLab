package com.evallab.api.testcase;

import com.evallab.api.common.Result;
import com.evallab.core.testcase.TestCase;
import com.evallab.core.testcase.TestCaseDraft;
import com.evallab.core.testcase.TestCaseGroup;
import com.evallab.core.testcase.TestCaseQuery;
import com.evallab.core.testcase.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "TestCase", description = "用例管理")
@RestController
@RequestMapping("/api/testcase")
public class TestCaseController {

    private final TestCaseService testCaseService;
    private final TestCaseMapper mapper;

    public TestCaseController(TestCaseService testCaseService, TestCaseMapper mapper) {
        this.testCaseService = testCaseService;
        this.mapper = mapper;
    }

    // 参数名必须显式声明：全站 JSON 是 snake_case，但 @RequestParam 不做命名策略松弛绑定，
    // 省略 name 时只认 Java 参数名（groupId），前端传的 group_id 会被静默忽略
    @Operation(summary = "查询用例列表", description = "支持按分组、标签筛选；默认只返回启用用例")
    @GetMapping
    public Result<List<TestCaseResponse>> list(@RequestParam(name = "group_id", required = false) Long groupId,
                                               @RequestParam(name = "tag", required = false) String tag,
                                               @RequestParam(name = "enabled_only", defaultValue = "true")
                                               boolean enabledOnly) {
        return Result.ok(testCaseService.query(new TestCaseQuery(groupId, tag, enabledOnly))
                .stream().map(mapper::toResponse).toList());
    }

    @Operation(summary = "查询用例详情")
    @GetMapping("/{id}")
    public Result<TestCaseResponse> detail(@PathVariable long id) {
        return Result.ok(mapper.toResponse(testCaseService.get(id)));
    }

    @Operation(summary = "创建用例", description = "必须声明 expected_output 或 expected_tools 之一（见设计决策 D2/D3）")
    @PostMapping
    public Result<TestCaseResponse> create(@Valid @RequestBody TestCaseRequest request) {
        TestCaseDraft draft = mapper.toDraft(request);
        return Result.ok(mapper.toResponse(testCaseService.create(draft)));
    }

    @Operation(summary = "更新用例", description = "version 自增，历史评测结果仍指向旧版本")
    @PutMapping("/{id}")
    public Result<TestCaseResponse> update(@PathVariable long id, @Valid @RequestBody TestCaseRequest request) {
        TestCaseDraft draft = mapper.toDraft(request);
        return Result.ok(mapper.toResponse(testCaseService.update(id, draft)));
    }

    @Operation(summary = "删除用例")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable long id) {
        testCaseService.delete(id);
        return Result.ok(null);
    }

    @Operation(summary = "查询分组列表")
    @GetMapping("/group")
    public Result<List<TestCaseGroupResponse>> groups() {
        return Result.ok(testCaseService.groups().stream().map(mapper::toResponse).toList());
    }

    @Operation(summary = "创建分组")
    @PostMapping("/group")
    public Result<TestCaseGroupResponse> createGroup(@Valid @RequestBody TestCaseGroupRequest request) {
        return Result.ok(mapper.toResponse(testCaseService.createGroup(request.groupName())));
    }
}
