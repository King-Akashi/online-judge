package com.minijudge.judge.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minijudge.contracts.MessagingNames;
import com.minijudge.contracts.SubmissionEvaluated;
import com.minijudge.judge.model.ResultOutboxEvent;
import com.minijudge.judge.repository.ResultOutboxRepository;
import com.minijudge.judge.service.JudgeJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Component
public class JudgeResultOutboxPublisher {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JudgeResultOutboxPublisher.class);

    private final ResultOutboxRepository resultOutboxRepository;
    private final JudgeJobStore judgeJobStore;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final long confirmTimeoutMs;

    public JudgeResultOutboxPublisher(
            ResultOutboxRepository resultOutboxRepository,
            JudgeJobStore judgeJobStore,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${minijudge.result-outbox.confirm-timeout-ms:5000}") long confirmTimeoutMs) {
        this.resultOutboxRepository = resultOutboxRepository;
        this.judgeJobStore = judgeJobStore;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.confirmTimeoutMs = confirmTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${minijudge.result-outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<ResultOutboxEvent> pending = resultOutboxRepository
                .findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        Instant.now());
        for (ResultOutboxEvent outboxEvent : pending) {
            try {
                publish(outboxEvent);
                outboxEvent.markPublished(Instant.now());
                judgeJobStore.markResultPublished(outboxEvent.getSubmissionId());
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

    private void publish(ResultOutboxEvent outboxEvent)
            throws InterruptedException, ExecutionException, TimeoutException {
        SubmissionEvaluated event;
        try {
            event = objectMapper.readValue(outboxEvent.getPayload(), SubmissionEvaluated.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored evaluation event is invalid", exception);
        }

        CorrelationData correlationData = new CorrelationData(event.eventId().toString());
        rabbitTemplate.convertAndSend(
                MessagingNames.EVENTS_EXCHANGE,
                MessagingNames.SUBMISSION_EVALUATED_ROUTING_KEY,
                event,
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                },
                correlationData);
        CorrelationData.Confirm confirm = correlationData.getFuture()
                .get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
        if (!confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ rejected the result: " + confirm.getReason());
        }
        if (correlationData.getReturned() != null) {
            throw new IllegalStateException("RabbitMQ returned an unroutable result");
        }
    }

    private void recordFailure(ResultOutboxEvent event, Exception exception) {
        String message = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        event.markFailed(Instant.now(), message);
        LOGGER.warn("Could not publish judge result {}; retry scheduled: {}",
                event.getEventId(), message, exception);
    }
}
