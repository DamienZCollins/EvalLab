package com.evallab.api.baseline;

import com.evallab.api.common.Result;
import com.evallab.core.baseline.BaselineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Baseline", description = "回归基线")
@RestController
@RequestMapping("/api/baseline")
public class BaselineController {

    private final BaselineService baselineService;

    public BaselineController(BaselineService baselineService) {
        this.baselineService = baselineService;
    }

    @Operation(summary = "创建基线", description = "从已完成任务快照三项指标；同一任务只能建一条")
    @PostMapping
    public Result<BaselineResponse> create(@Valid @RequestBody BaselineRequest request) {
        return Result.ok(BaselineResponse.of(baselineService.create(request.taskId(), request.name())));
    }

    @Operation(summary = "查询基线列表", description = "按创建时间倒序")
    @GetMapping
    public Result<List<BaselineResponse>> list() {
        return Result.ok(baselineService.list().stream().map(BaselineResponse::of).toList());
    }
}
