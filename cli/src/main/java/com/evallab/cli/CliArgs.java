package com.evallab.cli;

/**
 * 解析后的启动参数。
 *
 * @param taskId       必填，待判定的评测任务
 * @param baselineId   必填，对比基线
 * @param passRate     {@code --threshold-pass-rate}，缺省 null 交给服务端默认值
 * @param avgScore     {@code --threshold-avg-score}
 * @param p95Duration  {@code --threshold-p95-duration}
 * @param server       {@code --server}，默认 http://localhost:8080
 */
public record CliArgs(
        Long taskId,
        Long baselineId,
        String passRate,
        String avgScore,
        String p95Duration,
        String server
) {

    public static final String USAGE = """
            用法：java -jar cli.jar --task <task_id> --baseline <baseline_id> [选项]

            必填：
              --task <id>                 待判定的评测任务 ID
              --baseline <id>             对比基线 ID

            可选：
              --threshold-pass-rate <v>   通过率允许的最大跌幅，如 -0.10
              --threshold-avg-score <v>   平均分允许的最大跌幅，如 -0.15
              --threshold-p95-duration <v> P95 耗时允许的最大倍率，如 1.5
              --server <url>              平台地址，默认 http://localhost:8080

            退出码：0 通过；1 回归；2 参数错误或请求失败
            """;

    /** 参数不合法，由调用方映射为退出码 2。 */
    public static class InvalidArgumentException extends RuntimeException {
        public InvalidArgumentException(String message) {
            super(message);
        }
    }

    public static CliArgs parse(String[] argv) {
        Long task = null;
        Long baseline = null;
        String passRate = null;
        String avgScore = null;
        String p95 = null;
        String server = "http://localhost:8080";

        for (int i = 0; i < argv.length; i++) {
            String raw = argv[i];
            String name = raw;
            String inline = null;
            int eq = raw.indexOf('=');
            if (raw.startsWith("--") && eq > 0) {
                name = raw.substring(0, eq);
                inline = raw.substring(eq + 1);
            }
            switch (name) {
                case "-h", "--help" -> throw new InvalidArgumentException(USAGE);
                case "--task" -> task = longValue(name, value(argv, i, inline));
                case "--baseline" -> baseline = longValue(name, value(argv, i, inline));
                case "--threshold-pass-rate" -> passRate = value(argv, i, inline);
                case "--threshold-avg-score" -> avgScore = value(argv, i, inline);
                case "--threshold-p95-duration" -> p95 = value(argv, i, inline);
                case "--server" -> server = value(argv, i, inline);
                default -> throw new InvalidArgumentException("未知参数: " + raw + "\n\n" + USAGE);
            }
            // 内联写法没有消耗下一个 token
            if (inline == null && hasValue(name)) {
                i++;
            }
        }
        if (task == null || baseline == null) {
            throw new InvalidArgumentException("--task 与 --baseline 均为必填\n\n" + USAGE);
        }
        return new CliArgs(task, baseline, passRate, avgScore, p95, stripTrailingSlash(server));
    }

    private static boolean hasValue(String name) {
        return switch (name) {
            case "--task", "--baseline", "--threshold-pass-rate", "--threshold-avg-score",
                 "--threshold-p95-duration", "--server" -> true;
            default -> false;
        };
    }

    private static String value(String[] argv, int index, String inline) {
        if (inline != null) {
            if (inline.isBlank()) {
                throw new InvalidArgumentException(argv[index] + " 缺少取值");
            }
            return inline;
        }
        if (index + 1 >= argv.length) {
            throw new InvalidArgumentException(argv[index] + " 缺少取值");
        }
        // 阈值允许负数，不能因为下一个 token 以 '-' 开头就当成新参数
        return argv[index + 1];
    }

    private static Long longValue(String name, String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException(name + " 需要整数，收到: " + raw);
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
