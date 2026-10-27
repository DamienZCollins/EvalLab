package com.evallab.api.eval;

import com.evallab.api.common.Result;
import com.evallab.api.redteam.RedteamReportResponse;
import com.evallab.core.eval.EvalTaskStore;
import com.evallab.core.eval.EvalTask;
import com.evallab.core.executor.EvalService;
import com.evallab.core.redteam.RedteamReportService;
import com.evallab.core.testcase.TestCaseQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Eval", description = "评测任务")
@RestController
@RequestMapping("/api/eval")
public class EvalController {

    private final EvalService evalService;
    private final EvalTaskStore taskStore;
    private final EvalResponseMapper mapper;
    private final RedteamReportService redteamReportService;

    public EvalController(EvalService evalService, EvalTaskStore taskStore, EvalResponseMapper mapper,
                          RedteamReportService redteamReportService) {
        this.evalService = evalService;
        this.taskStore = taskStore;
        this.mapper = mapper;
        this.redteamReportService = redteamReportService;
    }

    @Operation(summary = "创建评测任务", description = "按筛选条件取用例，异步逐例拉取 Agent（见设计决策 D1）")
    @PostMapping("/task")
    public Result<EvalTaskResponse> createTask(@Valid @RequestBody EvalTaskRequest request) {
        TestCaseQuery query = new TestCaseQuery(request.groupId(), request.tag(), true);
        EvalTask task = evalService.submit(request.agentName(), query);
        return Result.ok(mapper.toResponse(task));
    }

    @Operation(summary = "查询任务状态与统计")
    @GetMapping("/task/{id}")
    public Result<EvalTaskResponse> task(@PathVariable long id) {
        return Result.ok(mapper.toResponse(evalService.getTask(id)));
    }

    @Operation(summary = "查询任务详细结果", description = "含每个用例的全部评分记录")
    @GetMapping("/task/{id}/result")
    public Result<List<EvalResultResponse>> results(@PathVariable long id) {
        evalService.getTask(id);
        return Result.ok(taskStore.findResults(id).stream().map(mapper::toResponse).toList());
    }

    @Operation(summary = "红队评测报告",
            description = "拦截率 = 防御成功数 / 总攻击数（0–1）；只统计该任务里的红队用例")
    @GetMapping("/task/{id}/redteam-report")
    public Result<RedteamReportResponse> redteamReport(@PathVariable long id) {
        return Result.ok(RedteamReportResponse.of(redteamReportService.report(id)));
    }
}
