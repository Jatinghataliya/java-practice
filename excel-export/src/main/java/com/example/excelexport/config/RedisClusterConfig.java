package com.example.excelexport.config;

import com.example.excelexport.dto.JobProgressEvent;
import com.example.excelexport.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@ConditionalOnProperty(name = "app.cluster.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisClusterConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisClusterConfig.class);
    public static final String PROGRESS_TOPIC = "export-progress-topic";
    public static final String TASK_QUEUE_KEY = "export-task-queue";

    @Bean
    public ChannelTopic progressTopic() {
        return new ChannelTopic(PROGRESS_TOPIC);
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
                                                         MessageListenerAdapter listenerAdapter,
                                                         ChannelTopic progressTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, progressTopic);
        log.info("Redis Cluster Pub/Sub listener initialized on channel [{}]", PROGRESS_TOPIC);
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(NotificationService notificationService, ObjectMapper objectMapper) {
        return new MessageListenerAdapter((org.springframework.data.redis.connection.MessageListener) (message, pattern) -> {
            try {
                String body = new String(message.getBody());
                JobProgressEvent event = objectMapper.readValue(body, JobProgressEvent.class);
                notificationService.emitLocalProgress(event);
            } catch (Exception e) {
                log.error("Failed to deserialize cluster progress event from Redis: {}", e.getMessage());
            }
        });
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
