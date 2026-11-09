package com.minijudge.problem.service;

import com.minijudge.contracts.Language;
import com.minijudge.problem.api.dto.JudgeTestCase;
import com.minijudge.problem.api.dto.ProblemDetails;
import com.minijudge.problem.model.Problem;
import com.minijudge.problem.model.ProblemDifficulty;
import com.minijudge.problem.model.TestCase;
import com.minijudge.problem.repository.ProblemRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProblemServiceTest {

    @Test
    void publicDetailsExposeOnlySampleTestCases() {
        UUID id = UUID.randomUUID();
        Problem problem = problemWithTests(id);
        ProblemRepository repository = mock(ProblemRepository.class);
        when(repository.findWithTestCasesById(id)).thenReturn(Optional.of(problem));

        ProblemDetails details = new ProblemService(repository).get(id);

        assertThat(details.sampleTestCases()).hasSize(1);
        assertThat(details.sampleTestCases().getFirst().input()).isEqualTo("sample input");
        assertThat(details.toString()).doesNotContain("hidden input", "hidden output");
    }

    @Test
    void judgeTestSuiteIncludesSampleAndHiddenCases() {
        UUID id = UUID.randomUUID();
        Problem problem = problemWithTests(id);
        ProblemRepository repository = mock(ProblemRepository.class);
        when(repository.findWithTestCasesById(id)).thenReturn(Optional.of(problem));

        List<JudgeTestCase> testCases = new ProblemService(repository).getTestCases(id);

        assertThat(testCases).hasSize(2);
        assertThat(testCases.get(1).input()).isEqualTo("hidden input");
        assertThat(testCases.get(1).sample()).isFalse();
    }

    private Problem problemWithTests(UUID id) {
        Problem problem = new Problem("Test", ProblemDifficulty.EASY, Language.JAVA,
                "statement", "constraints", "starter", 1000, 128, Instant.now());
        problem.addTestCase(new TestCase("sample input", "sample output", true, 0));
        problem.addTestCase(new TestCase("hidden input", "hidden output", false, 1));
        return problem;
    }
}
