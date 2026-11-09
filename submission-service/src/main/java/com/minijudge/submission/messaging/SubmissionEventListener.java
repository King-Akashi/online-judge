package com.minijudge.submission.messaging;

import com.minijudge.contracts.MessagingNames;
import com.minijudge.contracts.SubmissionEvaluated;
import com.minijudge.contracts.SubmissionStarted;
import com.minijudge.submission.service.SubmissionService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SubmissionEventListener {

    private final SubmissionService submissionService;

    public SubmissionEventListener(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @RabbitListener(queues = MessagingNames.SUBMISSION_STARTED_QUEUE)
    public void onSubmissionStarted(SubmissionStarted event) {
        submissionService.markRunning(event);
    }

    @RabbitListener(queues = MessagingNames.SUBMISSION_EVALUATED_QUEUE)
    public void onSubmissionEvaluated(SubmissionEvaluated event) {
        submissionService.complete(event);
    }
}
