package com.evallab.core.scorer;

/**
 * Judge 模型的端口：core 不依赖 Spring AI 与 HTTP 细节。
 */
public interface JudgeModel {

    /**
     * @return 模型给出的评分与理由
     * @throws JudgeUnavailableException 调用失败、超时或响应无法解析
     */
    JudgeVerdict judge(String input, String expectedOutput, String actualOutput);

    /**
     * @param score  0-1
     * @param reason 评分理由
     */
    record JudgeVerdict(java.math.BigDecimal score, String reason) {
    }

    class JudgeUnavailableException extends RuntimeException {
        public JudgeUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
