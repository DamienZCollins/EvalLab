package com.evallab;

import com.evallab.core.scorer.RuleScorer;
import com.evallab.core.scorer.ScoringPipeline;
import com.evallab.core.scorer.ToolScorer;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 应用装配。
 *
 * <p>评测任务用固定线程池异步执行，队列满时拒绝而不是无限堆积——任务状态在数据库里，
 * 被拒绝时调用方能看到明确的错误，而不是任务排队到用户以为卡死。
 */
@Configuration
@ConfigurationPropertiesScan
public class ApiLayerConfig {

    @Bean
    public ScoringPipeline scoringPipeline(RuleScorer ruleScorer, ToolScorer toolScorer) {
        return new ScoringPipeline(ruleScorer, toolScorer);
    }

    @Bean
    public TaskExecutor evalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("eval-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
