package com.minijudge.problem.service;

import com.minijudge.problem.api.dto.CreateProblemRequest;
import com.minijudge.problem.api.dto.CreateTestCaseRequest;
import com.minijudge.problem.api.dto.JudgeTestCase;
import com.minijudge.problem.api.dto.ProblemDetails;
import com.minijudge.problem.api.dto.ProblemSummary;
import com.minijudge.problem.api.dto.SampleTestCase;
import com.minijudge.problem.model.Problem;
import com.minijudge.problem.model.TestCase;
import com.minijudge.problem.repository.ProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProblemService {

    private final ProblemRepository problemRepository;

    public ProblemService(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    @Transactional
    public ProblemDetails create(CreateProblemRequest request) {
        if (request.testCases().stream().noneMatch(CreateTestCaseRequest::sample)) {
            throw new InvalidProblemException("At least one sample test case is required");
        }
        if (problemRepository.existsByTitleIgnoreCase(request.title().trim())) {
            throw new ProblemConflictException("A problem with this title already exists");
        }

        Problem problem = new Problem(
                request.title().trim(),
                request.difficulty(),
                request.language(),
                request.statement(),
                request.constraints(),
                request.starterCode(),
                request.timeLimitMs(),
                request.memoryLimitMb(),
                Instant.now());

        for (int index = 0; index < request.testCases().size(); index++) {
            CreateTestCaseRequest testCase = request.testCases().get(index);
            problem.addTestCase(new TestCase(
                    testCase.input(), testCase.expectedOutput(), testCase.sample(), index));
        }
        return toDetails(problemRepository.save(problem));
    }

    @Transactional(readOnly = true)
    public List<ProblemSummary> list() {
        return problemRepository.findAllByOrderByTitleAsc().stream()
                .map(problem -> new ProblemSummary(
                        problem.getId(),
                        problem.getTitle(),
                        problem.getDifficulty(),
                        problem.getLanguage(),
                        problem.getTimeLimitMs(),
                        problem.getMemoryLimitMb()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProblemDetails get(UUID id) {
        return toDetails(findWithTestCases(id));
    }

    @Transactional(readOnly = true)
    public List<JudgeTestCase> getTestCases(UUID id) {
        return findWithTestCases(id).getTestCases().stream()
                .map(testCase -> new JudgeTestCase(
                        testCase.getId(),
                        testCase.getInput(),
                        testCase.getExpectedOutput(),
                        testCase.isSample(),
                        testCase.getOrderIndex()))
                .toList();
    }

    private Problem findWithTestCases(UUID id) {
        return problemRepository.findWithTestCasesById(id)
                .orElseThrow(() -> new ProblemNotFoundException(id));
    }

    private ProblemDetails toDetails(Problem problem) {
        List<SampleTestCase> samples = problem.getTestCases().stream()
                .filter(TestCase::isSample)
                .map(testCase -> new SampleTestCase(
                        testCase.getInput(), testCase.getExpectedOutput()))
                .toList();
        return new ProblemDetails(
                problem.getId(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getLanguage(),
                problem.getStatement(),
                problem.getConstraintsText(),
                problem.getStarterCode(),
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb(),
                samples);
    }
}
