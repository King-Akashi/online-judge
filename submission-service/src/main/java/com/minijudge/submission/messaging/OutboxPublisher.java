package com.minijudge.submission.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minijudge.contracts.MessagingNames;
import com.minijudge.contracts.SubmissionRequested;
import com.minijudge.submission.model.OutboxEvent;
import com.minijudge.submission.repository.OutboxEventRepository;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OutboxPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final long confirmTimeoutMs;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${minijudge.outbox.confirm-timeout-ms:5000}") long confirmTimeoutMs) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.confirmTimeoutMs = confirmTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${minijudge.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository
                .findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        Instant.now());
        for (OutboxEvent outboxEvent : pending) {
            try {
                publish(outboxEvent);
                outboxEvent.markPublished(Instant.now());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                recordFailure(outboxEvent, exception);
                return;
            } catch (ExecutionException | TimeoutException | AmqpException
                     | IllegalStateException exception) {
                recordFailure(outboxEvent, exception);
            }
        }
    }

    private void publish(OutboxEvent outboxEvent)
            throws InterruptedException, ExecutionException, TimeoutException {
        SubmissionRequested event;
        try {
            event = objectMapper.readValue(outboxEvent.getPayload(), SubmissionRequested.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored submission event is invalid", exception);
        }
        if (!SubmissionRequested.class.getSimpleName().equals(outboxEvent.getEventType())) {
            throw new IllegalStateException("Unsupported outbox event type: "
                    + outboxEvent.getEventType());
        }

        CorrelationData correlationData = new CorrelationData(event.eventId().toString());
        rabbitTemplate.convertAndSend(
                MessagingNames.EVENTS_EXCHANGE,
                MessagingNames.SUBMISSION_REQUESTED_ROUTING_KEY,
                event,
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                },
                correlationData);
        CorrelationData.Confirm confirm = correlationData.getFuture()
                .get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
        if (!confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ rejected the event: " + confirm.getReason());
        }
        if (correlationData.getReturned() != null) {
            throw new IllegalStateException("RabbitMQ returned an unroutable event");
        }
    }

    private void recordFailure(OutboxEvent event, Exception exception) {
        String message = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        event.markFailed(Instant.now(), message);
        LOGGER.warn("Could not publish outbox event {}; retry scheduled: {}",
                event.getEventId(), message, exception);
    }
}
