package com.minijudge.judge.messaging;

import com.minijudge.contracts.MessagingNames;
import com.minijudge.contracts.SubmissionRequested;
import com.minijudge.judge.service.JudgeOrchestrator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SubmissionRequestedListener {

    private final JudgeOrchestrator judgeOrchestrator;

    public SubmissionRequestedListener(JudgeOrchestrator judgeOrchestrator) {
        this.judgeOrchestrator = judgeOrchestrator;
    }

    @RabbitListener(queues = MessagingNames.SUBMISSION_REQUESTED_QUEUE)
    public void onSubmissionRequested(SubmissionRequested event) {
        judgeOrchestrator.process(event);
    }
}
