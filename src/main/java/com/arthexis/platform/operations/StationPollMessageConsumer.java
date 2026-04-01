package com.arthexis.platform.operations;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class StationPollMessageConsumer {

  private static final Logger logger = LoggerFactory.getLogger(StationPollMessageConsumer.class);

  private final JobExecutionTracker executionTracker;
  private final StationPollExecutor stationPollExecutor;
  private final RabbitTemplate rabbitTemplate;
  private final int maxAttempts;

  public StationPollMessageConsumer(
      JobExecutionTracker executionTracker,
      StationPollExecutor stationPollExecutor,
      RabbitTemplate rabbitTemplate,
      @Value("${arthexis.jobs.max-attempts:3}") int maxAttempts) {
    this.executionTracker = executionTracker;
    this.stationPollExecutor = stationPollExecutor;
    this.rabbitTemplate = rabbitTemplate;
    this.maxAttempts = maxAttempts;
  }

  @RabbitListener(queues = "${arthexis.jobs.station-poll-queue:arthexis.station.poll}")
  public void consume(Message message, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
    String jobType = routingKey == null || routingKey.isBlank() ? "station.poll" : routingKey;
    String body = new String(message.getBody(), StandardCharsets.UTF_8);
    String jobId = header(message, "x-job-id", UUID.randomUUID().toString());
    String stationScope = header(message, "x-station-scope", body);
    String idempotencyKey = header(message, "x-idempotency-key", jobId);
    int attempt = parseIntHeader(message, "x-attempt", 1);

    JobExecutionTracker.BeginResult beginResult =
        executionTracker.begin(jobId, jobType, stationScope, idempotencyKey, attempt);

    if (beginResult.duplicate()) {
      logger.info("Skipping duplicate job message idempotencyKey={}", idempotencyKey);
      return;
    }

    try {
      stationPollExecutor.execute(stationScope);
      executionTracker.markCompleted(beginResult.record(), jobType);
    } catch (RuntimeException ex) {
      if (attempt < maxAttempts) {
        executionTracker.markRetryScheduled(beginResult.record(), jobType, ex.getMessage());
        publishRetry(message, attempt + 1);
      } else {
        executionTracker.markFailed(beginResult.record(), jobType, ex.getMessage());
        publishDeadLetter(message);
      }
    }
  }

  private void publishRetry(Message message, int nextAttempt) {
    rabbitTemplate.convertAndSend(
        "arthexis.jobs.retry",
        "station.poll.retry",
        message.getBody(),
        outbound -> {
          outbound.getMessageProperties().getHeaders().putAll(message.getMessageProperties().getHeaders());
          outbound.getMessageProperties().setHeader("x-attempt", nextAttempt);
          return outbound;
        });
  }

  private void publishDeadLetter(Message message) {
    rabbitTemplate.convertAndSend(
        "arthexis.jobs.dlx",
        "station.poll.dead",
        message.getBody(),
        outbound -> {
          outbound.getMessageProperties().getHeaders().putAll(message.getMessageProperties().getHeaders());
          return outbound;
        });
  }

  private String header(Message message, String name, String fallback) {
    Object value = message.getMessageProperties().getHeaders().get(name);
    return value == null ? fallback : String.valueOf(value);
  }

  private int parseIntHeader(Message message, String name, int fallback) {
    Object value = message.getMessageProperties().getHeaders().get(name);
    if (value == null) {
      return fallback;
    }
    try {
      return Integer.parseInt(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }
}
