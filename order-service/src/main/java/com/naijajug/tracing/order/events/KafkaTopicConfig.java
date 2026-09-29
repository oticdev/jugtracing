package com.naijajug.tracing.order.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
class KafkaTopicConfig {

    @Bean
    NewTopic orderEventsTopic(@Value("${app.kafka.topics.order-events}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
