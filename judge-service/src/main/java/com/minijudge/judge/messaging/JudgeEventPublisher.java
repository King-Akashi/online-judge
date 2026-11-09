package com.minijudge.judge.messaging;

import com.minijudge.contracts.MessagingNames;
import com.minijudge.contracts.SubmissionRequested;
import com.minijudge.contracts.SubmissionStarted;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class JudgeEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final long confirmTimeoutMs;

    public JudgeEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${minijudge.result-outbox.confirm-timeout-ms:5000}") long confirmTimeoutMs) {
        this.rabbitTemplate = rabbitTemplate;
        this.confirmTimeoutMs = confirmTimeoutMs;
    }

    public void publishStarted(SubmissionStarted event) {
        publish(
                MessagingNames.SUBMISSION_STARTED_ROUTING_KEY,
                event.eventId().toString(),
                event,
                "started");
    }

    public void republishRequested(SubmissionRequested event) {
        publish(
                MessagingNames.SUBMISSION_REQUESTED_ROUTING_KEY,
                event.eventId().toString(),
                event,
                "recovery request");
    }

    private void publish(String routingKey, String correlationId, Object event, String description) {
        CorrelationData correlationData = new CorrelationData(correlationId);
        try {
            rabbitTemplate.convertAndSend(
                    MessagingNames.EVENTS_EXCHANGE,
                    routingKey,
                    event,
                    message -> {
                        message.getMessageProperties()
                                .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return message;
                    },
                    correlationData);
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException(
                        "RabbitMQ rejected the " + description + ": " + confirm.getReason());
            }
            if (correlationData.getReturned() != null) {
                throw new IllegalStateException(
                        "RabbitMQ returned an unroutable " + description);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Publishing the " + description + " was interrupted",
                    exception);
        } catch (ExecutionException | TimeoutException | AmqpException exception) {
            throw new IllegalStateException("Could not publish the " + description, exception);
        }
    }
}
