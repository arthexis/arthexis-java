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
    rabbitTemplate.convertAndSend("arthexis.jobs", "station.poll", "poll-all");
    logger.info("Queued station polling sweep job");
  }
}
