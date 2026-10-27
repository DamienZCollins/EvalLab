package com.evallab.infra.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.client.RestClient;

/**
 * Judge 的 ChatClient 装配。
 *
 * <p>直接用 {@link OpenAiApi} + {@link OpenAiChatModel} 手工装配，不走 starter 自动配置：
 * 未配置 {@code evallab.llm.base-url} 时整个 LLM 链路不注册，评测照常进行。
 *
 * <p>判空必须自己来：{@code @ConditionalOnProperty} 只要属性存在就成立，
 * 而 Docker Compose 会把未设置的变量注入为空串，用它会导致默认部署直接启动失败。
 */
@Configuration
@Conditional(LlmConfig.BaseUrlPresent.class)
public class LlmConfig {

    @Bean
    public ChatClient judgeChatClient(LlmProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(properties.timeout())
                .withReadTimeout(properties.timeout());
        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.simple().build(settings));

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(properties.baseUrl())
                .apiKey(properties.apiKey())
                .restClientBuilder(restClientBuilder)
                .build();

        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.model())
                        .temperature(properties.temperature())
                        .build())
                .build();

        return ChatClient.builder(model).build();
    }

    /** 只有 base-url 有实际内容时才装配 LLM。 */
    static class BaseUrlPresent implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String baseUrl = context.getEnvironment().getProperty("evallab.llm.base-url");
            return baseUrl != null && !baseUrl.isBlank();
        }
    }
}
