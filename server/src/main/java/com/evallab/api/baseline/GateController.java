package com.evallab.api.baseline;

import com.evallab.core.baseline.BaselineService;
import com.evallab.core.baseline.GateOutcome;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门禁接口归属 baseline 模块，但路径按总体方案定在 /api/eval/gate。
 */
@Tag(name = "Baseline", description = "回归门禁")
@RestController
@RequestMapping("/api/eval")
public class GateController {

    private final BaselineService baselineService;

    public GateController(BaselineService baselineService) {
        this.baselineService = baselineService;
    }

    @Operation(summary = "回归门禁", description = "通过 200，检测到回归 422；响应体相同")
    @PostMapping("/gate")
    public ResponseEntity<GateResponse> gate(@Valid @RequestBody GateRequest request) {
        GateOutcome outcome = baselineService.gate(request.taskId(), request.baselineId(),
                request.thresholdsOrNull());
        GateResponse body = GateResponse.of(outcome);
        return ResponseEntity.status(body.passed() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY)
                .body(body);
    }
}
