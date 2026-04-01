package com.arthexis.platform.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JobExecutionTracker.class})
class StationPollMessageConsumerTests {

  @org.springframework.beans.factory.annotation.Autowired JobExecutionRecordRepository repository;
  @org.springframework.beans.factory.annotation.Autowired JobExecutionTracker tracker;

  private RabbitTemplate rabbitTemplate;
  private StationPollExecutor executor;
  private StationPollMessageConsumer consumer;

  @BeforeEach
  void setUp() {
    rabbitTemplate = Mockito.mock(RabbitTemplate.class);
    executor = Mockito.mock(StationPollExecutor.class);
    tracker = new JobExecutionTracker(repository, new SimpleMeterRegistry());
    consumer = new StationPollMessageConsumer(tracker, executor, rabbitTemplate, 3);
  }

  @Test
  void consumeCompletesAndDeduplicatesByIdempotencyKey() {
    Message message =
        MessageBuilder.withBody("all".getBytes())
            .setHeader("x-job-id", "job-1")
            .setHeader("x-idempotency-key", "station.poll:job-1")
            .setHeader("x-attempt", 1)
            .build();

    consumer.consume(message, "station.poll");
    consumer.consume(message, "station.poll");

    assertThat(repository.count()).isEqualTo(1);
    JobExecutionRecord record = repository.findAll().getFirst();
    assertThat(record.getStatus()).isEqualTo(JobExecutionStatus.COMPLETED);
    verify(executor, times(1)).execute(eq("all"));
  }

  @Test
  void consumeSchedulesRetryThenDeadLettersAtMaxAttempts() {
    doThrow(new IllegalStateException("boom")).when(executor).execute(any());
    Message firstAttempt =
        MessageBuilder.withBody("all".getBytes())
            .setHeader("x-job-id", "job-2")
            .setHeader("x-idempotency-key", "station.poll:job-2")
            .setHeader("x-attempt", 1)
            .build();

    consumer.consume(firstAttempt, "station.poll");

    JobExecutionRecord record = repository.findAll().getFirst();
    assertThat(record.getStatus()).isEqualTo(JobExecutionStatus.RETRY_SCHEDULED);
    verify(rabbitTemplate)
        .convertAndSend(
            eq("arthexis.jobs.retry"),
            eq("station.poll.retry"),
            any(),
            any(MessagePostProcessor.class));

    Message finalAttempt =
        MessageBuilder.withBody("all".getBytes())
            .setHeader("x-job-id", "job-2")
            .setHeader("x-idempotency-key", "station.poll:job-2")
            .setHeader("x-attempt", 3)
            .build();

    consumer.consume(finalAttempt, "station.poll");

    JobExecutionRecord updated = repository.findAll().getFirst();
    assertThat(updated.getStatus()).isEqualTo(JobExecutionStatus.FAILED);
    verify(rabbitTemplate)
        .convertAndSend(
            eq("arthexis.jobs.dlx"),
            eq("station.poll.dead"),
            any(),
            any(MessagePostProcessor.class));
  }
}
