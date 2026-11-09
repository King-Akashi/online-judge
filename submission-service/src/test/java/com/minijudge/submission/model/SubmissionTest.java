package com.minijudge.submission.model;

import com.minijudge.contracts.Language;
import com.minijudge.contracts.SubmissionStatus;
import com.minijudge.contracts.Verdict;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionTest {

    @Test
    void evaluationTransitionsQueuedSubmissionToTerminalVerdict() {
        Instant now = Instant.now();
        Submission submission = new Submission(UUID.randomUUID(), Language.JAVA, "class Main {}",
                now);

        submission.markRunning(now.plusMillis(1));
        submission.complete(Verdict.AC, 12, 1024, 3, 3, now.plusMillis(20));

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
        assertThat(submission.getVerdict()).isEqualTo(Verdict.AC);
        assertThat(submission.getPassedTests()).isEqualTo(3);
    }

    @Test
    void duplicateEvaluationCannotReplaceTerminalVerdict() {
        Instant now = Instant.now();
        Submission submission = new Submission(UUID.randomUUID(), Language.JAVA, "class Main {}",
                now);
        submission.complete(Verdict.WA, 10, 1024, 1, 2, now.plusMillis(10));

        submission.complete(Verdict.AC, 5, 512, 2, 2, now.plusMillis(20));

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
        assertThat(submission.getVerdict()).isEqualTo(Verdict.WA);
        assertThat(submission.getPassedTests()).isEqualTo(1);
    }
}
