package com.evallab.cli;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * 命令行入口。
 *
 * <pre>
 * java -jar cli.jar --task 123 --baseline 456 --threshold-pass-rate -0.10
 * </pre>
 *
 * <p>退出码：0 通过；1 检测到回归；2 参数错误或请求失败。CI 里直接 {@code || exit 1} 即可拦截。
 */
public final class EvalCli {

    static final int EXIT_PASS = 0;
    static final int EXIT_REGRESSION = 1;
    static final int EXIT_ERROR = 2;

    private EvalCli() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] argv, PrintStream out, PrintStream err) {
        // Windows 控制台默认 GBK，中文输出会乱码；固定 UTF-8，CI 日志也一致
        PrintStream stdout = utf8(out);
        PrintStream stderr = utf8(err);

        CliArgs args;
        try {
            args = CliArgs.parse(argv);
        } catch (CliArgs.InvalidArgumentException e) {
            stderr.println(e.getMessage());
            return EXIT_ERROR;
        }

        GateResult result;
        try {
            result = new GateClient(args.server()).call(args);
        } catch (RuntimeException e) {
            stderr.println(describe(e));
            return EXIT_ERROR;
        }

        GateReport.print(result, args, stdout);
        return result.passed() ? EXIT_PASS : EXIT_REGRESSION;
    }

    private static PrintStream utf8(PrintStream stream) {
        return new PrintStream(stream, true, StandardCharsets.UTF_8);
    }

    /** ConnectException 之类的 message 可能为 null，退化成类名，别输出「: null」。 */
    private static String describe(RuntimeException e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}
