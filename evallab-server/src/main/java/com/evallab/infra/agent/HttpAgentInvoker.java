package com.evallab.infra.agent;

import com.evallab.contract.trace.ExecuteRequest;
import com.evallab.contract.trace.TraceReport;
import com.evallab.core.eval.FailureCategory;
import com.evallab.core.executor.AgentInvocationException;
import com.evallab.core.executor.AgentInvoker;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;

/**
 * 通过 HTTP 拉取 Agent 执行结果。
 *
 * <p>时长类错误（超时、连不上）归 ENVIRONMENT，响应不合契约归 UNKNOWN（见 D1）。
 */
@Component
public class HttpAgentInvoker implements AgentInvoker {

    private final AgentProperties properties;
    private final RestClient.Builder restClientBuilder;

    public HttpAgentInvoker(AgentProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public TraceReport invoke(String agentName, ExecuteRequest request) {
        AgentProperties.Target target = properties.registry().get(agentName);
        if (target == null || target.baseUrl() == null || target.baseUrl().isBlank()) {
            throw new AgentInvocationException(FailureCategory.UNKNOWN,
                    "未配置 Agent: " + agentName + "（evallab.agent.registry." + agentName + "）");
        }
        try {
            TraceReport report = restClientBuilder.build()
                    .post()
                    .uri(target.baseUrl() + "/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TraceReport.class);
            if (report == null) {
                throw new AgentInvocationException(FailureCategory.UNKNOWN, "Agent 返回空响应");
            }
            return report;
        } catch (ResourceAccessException e) {
            throw new AgentInvocationException(FailureCategory.ENVIRONMENT,
                    isTimeout(e) ? "Agent 调用超时: " + agentName : "Agent 连接失败: " + agentName, e);
        } catch (RestClientResponseException e) {
            throw new AgentInvocationException(FailureCategory.UNKNOWN,
                    "Agent 返回 HTTP " + e.getStatusCode().value() + ": " + abbreviate(e.getResponseBodyAsString()), e);
        } catch (AgentInvocationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AgentInvocationException(FailureCategory.UNKNOWN,
                    "Agent 响应不符合契约: " + e.getMessage(), e);
        }
    }

    private static boolean isTimeout(ResourceAccessException e) {
        return e.getCause() instanceof SocketTimeoutException
                || e.getMessage() != null && e.getMessage().toLowerCase().contains("timed out");
    }

    private static String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 200 ? body : body.substring(0, 200) + "...";
    }

    /** 供配置校验使用：Agent 是否已配置。 */
    public boolean isConfigured(String agentName) {
        AgentProperties.Target target = properties.registry().get(agentName);
        return target != null && target.baseUrl() != null && !target.baseUrl().isBlank();
    }
}
