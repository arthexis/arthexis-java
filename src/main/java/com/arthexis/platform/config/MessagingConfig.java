package com.arthexis.platform.config;

import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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
  public TopicExchange jobRetryExchange() {
    return new TopicExchange("arthexis.jobs.retry");
  }

  @Bean
  public TopicExchange jobDeadLetterExchange() {
    return new TopicExchange("arthexis.jobs.dlx");
  }

  @Bean
  public Queue stationPollQueue() {
    return QueueBuilder.durable("arthexis.station.poll")
        .withArguments(
            Map.of(
                "x-dead-letter-exchange", "arthexis.jobs.retry",
                "x-dead-letter-routing-key", "station.poll.retry"))
        .build();
  }

  @Bean
  public Queue stationPollRetryQueue() {
    return QueueBuilder.durable("arthexis.station.poll.retry")
        .withArgument("x-message-ttl", 5000)
        .withArgument("x-dead-letter-exchange", "arthexis.jobs")
        .withArgument("x-dead-letter-routing-key", "station.poll")
        .build();
  }

  @Bean
  public Queue stationPollDeadLetterQueue() {
    return QueueBuilder.durable("arthexis.station.poll.dlq").build();
  }

  @Bean
  public Binding stationPollBinding(TopicExchange jobExchange, Queue stationPollQueue) {
    return BindingBuilder.bind(stationPollQueue).to(jobExchange).with("station.poll");
  }

  @Bean
  public Binding stationPollRetryBinding(TopicExchange jobRetryExchange, Queue stationPollRetryQueue) {
    return BindingBuilder.bind(stationPollRetryQueue).to(jobRetryExchange).with("station.poll.retry");
  }

  @Bean
  public Binding stationPollDeadLetterBinding(
      TopicExchange jobDeadLetterExchange, Queue stationPollDeadLetterQueue) {
    return BindingBuilder.bind(stationPollDeadLetterQueue)
        .to(jobDeadLetterExchange)
        .with("station.poll.dead");
  }
}
