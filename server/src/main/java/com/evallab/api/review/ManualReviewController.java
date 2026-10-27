package com.evallab.api.review;

import com.evallab.api.common.Result;
import com.evallab.core.review.ManualReviewDraft;
import com.evallab.core.review.ManualReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ManualReview", description = "人工抽检与准确率")
@RestController
@RequestMapping("/api/manual-review")
public class ManualReviewController {

    private final ManualReviewService service;

    public ManualReviewController(ManualReviewService service) {
        this.service = service;
    }

    @Operation(summary = "提交人工抽检", description = "同一 result_id 重复提交即更新")
    @PostMapping
    public Result<ManualReviewResponse> submit(@Valid @RequestBody ManualReviewRequest request) {
        return Result.ok(ManualReviewResponse.of(service.submit(new ManualReviewDraft(
                request.resultId(), request.humanScore(), request.humanFailureCategory(),
                request.reviewer(), request.remark()))));
    }

    @Operation(summary = "查询准确率", description = "Judge 分差 < 0.1 视为一致；归因按分层是否相同统计")
    @GetMapping("/accuracy")
    public Result<ManualReviewResponse.AccuracyReport> accuracy() {
        ManualReviewService.AccuracyReport report = service.report();
        return Result.ok(new ManualReviewResponse.AccuracyReport(report.metrics(),
                report.comparisons().stream().map(ManualReviewResponse.Comparison::of).toList()));
    }
}
