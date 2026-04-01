package com.arthexis.platform.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "arthexis.jobs.station-poll-enabled", havingValue = "true", matchIfMissing = true)
public class StationPollJob {

  private static final Logger logger = LoggerFactory.getLogger(StationPollJob.class);
  private final RabbitTemplate rabbitTemplate;

  public StationPollJob(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Scheduled(fixedDelayString = "${arthexis.jobs.station-poll-delay-ms:60000}")
  public void enqueueStationPollingSweep() {
    String jobId = java.util.UUID.randomUUID().toString();
    rabbitTemplate.convertAndSend(
        "arthexis.jobs",
        "station.poll",
        "all",
        message -> {
          message.getMessageProperties().setHeader("x-job-id", jobId);
          message.getMessageProperties().setHeader("x-job-type", "station.poll");
          message.getMessageProperties().setHeader("x-station-scope", "all");
          message.getMessageProperties().setHeader("x-idempotency-key", "station.poll:" + jobId);
          message.getMessageProperties().setHeader("x-attempt", 1);
          return message;
        });
    logger.info("Queued station polling sweep job jobId={}", jobId);
  }
}
