package com.evallab.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 调用平台门禁接口。只用 JDK HttpClient + Jackson，保持 jar 体积小、无框架依赖。
 */
final class GateClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String server;

    GateClient(String server) {
        this.server = server;
    }

    /**
     * @throws GateCallException 网络失败、非 200/422 响应或响应体无法解析
     */
    GateResult call(CliArgs args) {
        ObjectNode body = JSON.createObjectNode();
        body.put("task_id", args.taskId());
        body.put("baseline_id", args.baselineId());
        ObjectNode thresholds = body.putObject("thresholds");
        putIfPresent(thresholds, "pass_rate_threshold", args.passRate());
        putIfPresent(thresholds, "avg_score_threshold", args.avgScore());
        putIfPresent(thresholds, "p95_duration_threshold", args.p95Duration());

        HttpRequest request = HttpRequest.newBuilder(URI.create(server + "/api/eval/gate"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new GateCallException("无法连接平台 " + server + "（" + describe(e) + "）", e);
        }

        int status = response.statusCode();
        JsonNode json = parse(response.body());
        if (status == 200 || status == 422) {
            return GateResult.from(json, status);
        }
        // 400 / 404 等走统一 Result 包装，取 message 展示
        String message = json == null ? response.body() : json.path("message").asText(response.body());
        throw new GateCallException("平台返回 HTTP " + status + ": " + message);
    }

    private static String describe(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private static void putIfPresent(ObjectNode node, String field, String raw) {        if (raw == null) {
            return;
        }
        // 原样交给服务端解析：非法数字由服务端返回 400，避免两处各写一套校验
        try {
            node.put(field, new java.math.BigDecimal(raw.trim()));
        } catch (NumberFormatException e) {
            throw new CliArgs.InvalidArgumentException(field + " 不是合法数字: " + raw);
        }
    }

    private static JsonNode parse(String body) {
        try {
            return JSON.readTree(body);
        } catch (IOException e) {
            return null;
        }
    }

    static class GateCallException extends RuntimeException {
        GateCallException(String message) {
            super(message);
        }

        GateCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
