package com.arthexis.platform.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

  @Bean
  public TopicExchange jobExchange() {
    return new TopicExchange("arthexis.jobs");
  }

  @Bean
  public Queue stationPollQueue() {
    return new Queue("arthexis.station.poll");
  }

  @Bean
  public Binding stationPollBinding(TopicExchange jobExchange, Queue stationPollQueue) {
    return BindingBuilder.bind(stationPollQueue).to(jobExchange).with("station.poll");
  }
}
