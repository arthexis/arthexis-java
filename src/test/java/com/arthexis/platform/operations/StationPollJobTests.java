package com.arthexis.platform.operations;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class StationPollJobTests {

  @Test
  void enqueueAddsIdempotencyHeaders() {
    RabbitTemplate rabbitTemplate = org.mockito.Mockito.mock(RabbitTemplate.class);
    StationPollJob job = new StationPollJob(rabbitTemplate);

    job.enqueueStationPollingSweep();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<MessagePostProcessor> postProcessorCaptor =
        ArgumentCaptor.forClass(MessagePostProcessor.class);
    verify(rabbitTemplate)
        .convertAndSend(eq("arthexis.jobs"), eq("station.poll"), eq("all"), postProcessorCaptor.capture());

    var message = new org.springframework.amqp.core.Message(new byte[] {}, new org.springframework.amqp.core.MessageProperties());
    var processed = postProcessorCaptor.getValue().postProcessMessage(message);

    assertThat(processed.getMessageProperties().getHeaders()).containsKeys("x-idempotency-key", "x-job-id");
  }
}
