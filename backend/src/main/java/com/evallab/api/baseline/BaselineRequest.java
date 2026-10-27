package com.evallab.api.baseline;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "创建基线请求")
public record BaselineRequest(
        @NotNull(message = "task_id 不能为空")
        @Schema(description = "快照来源任务，必须是已完成任务")
        Long taskId,

        @Schema(description = "基线名称；留空则自动生成")
        String name
) {
}
