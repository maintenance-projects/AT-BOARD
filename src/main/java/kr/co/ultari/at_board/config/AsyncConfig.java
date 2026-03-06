package kr.co.ultari.at_board.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    /**
     * 알림 발송 전용 스레드 풀.
     * - corePoolSize 2: 평시 유지 스레드
     * - maxPoolSize 5: 최대 동시 알림 발송 수
     * - queueCapacity 50: 대기 큐 (글 50개까지 순차 처리)
     * - DiscardPolicy: 큐 초과 시 조용히 버림 (서버 안 죽음)
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("notification-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }
}
