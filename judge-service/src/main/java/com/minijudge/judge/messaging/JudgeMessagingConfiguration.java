package com.minijudge.judge.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minijudge.contracts.MessagingNames;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JudgeMessagingConfiguration {

    private static final String REQUESTED_DEAD_LETTER_QUEUE = "submission.requested.dlq";
    private static final String STARTED_DEAD_LETTER_QUEUE = "submission.started.dlq";
    private static final String EVALUATED_DEAD_LETTER_QUEUE = "submission.evaluated.dlq";

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    TopicExchange eventsExchange() {
        return new TopicExchange(MessagingNames.EVENTS_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(MessagingNames.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue submissionRequestedQueue() {
        return deadLetteredQueue(MessagingNames.SUBMISSION_REQUESTED_QUEUE,
                REQUESTED_DEAD_LETTER_QUEUE);
    }

    @Bean
    Queue submissionStartedQueue() {
        return deadLetteredQueue(MessagingNames.SUBMISSION_STARTED_QUEUE,
                STARTED_DEAD_LETTER_QUEUE);
    }

    @Bean
    Queue submissionEvaluatedQueue() {
        return deadLetteredQueue(MessagingNames.SUBMISSION_EVALUATED_QUEUE,
                EVALUATED_DEAD_LETTER_QUEUE);
    }

    @Bean
    Queue submissionRequestedDeadLetterQueue() {
        return QueueBuilder.durable(REQUESTED_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Queue submissionStartedDeadLetterQueue() {
        return QueueBuilder.durable(STARTED_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Queue submissionEvaluatedDeadLetterQueue() {
        return QueueBuilder.durable(EVALUATED_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding submissionRequestedBinding(Queue submissionRequestedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(submissionRequestedQueue)
                .to(eventsExchange).with(MessagingNames.SUBMISSION_REQUESTED_ROUTING_KEY);
    }

    @Bean
    Binding submissionStartedBinding(Queue submissionStartedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(submissionStartedQueue)
                .to(eventsExchange).with(MessagingNames.SUBMISSION_STARTED_ROUTING_KEY);
    }

    @Bean
    Binding submissionEvaluatedBinding(
            Queue submissionEvaluatedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(submissionEvaluatedQueue)
                .to(eventsExchange).with(MessagingNames.SUBMISSION_EVALUATED_ROUTING_KEY);
    }

    @Bean
    Binding requestedDeadLetterBinding(
            Queue submissionRequestedDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(submissionRequestedDeadLetterQueue)
                .to(deadLetterExchange).with(REQUESTED_DEAD_LETTER_QUEUE);
    }

    @Bean
    Binding startedDeadLetterBinding(
            Queue submissionStartedDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(submissionStartedDeadLetterQueue)
                .to(deadLetterExchange).with(STARTED_DEAD_LETTER_QUEUE);
    }

    @Bean
    Binding evaluatedDeadLetterBinding(
            Queue submissionEvaluatedDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(submissionEvaluatedDeadLetterQueue)
                .to(deadLetterExchange).with(EVALUATED_DEAD_LETTER_QUEUE);
    }

    private Queue deadLetteredQueue(String name, String deadLetterQueue) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(MessagingNames.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(deadLetterQueue)
                .build();
    }
}
