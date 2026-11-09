package com.minijudge.problem.seed;

import com.minijudge.contracts.Language;
import com.minijudge.problem.model.Problem;
import com.minijudge.problem.model.ProblemDifficulty;
import com.minijudge.problem.model.TestCase;
import com.minijudge.problem.repository.ProblemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class ProblemSeedData implements CommandLineRunner {

    private final ProblemRepository problemRepository;

    public ProblemSeedData(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (problemRepository.count() > 0) {
            return;
        }
        problemRepository.saveAll(List.of(
                problem("Two Sum", ProblemDifficulty.EASY,
                        "Given an array of integers and a target, print the indices of two distinct values whose sum is the target. Print the smaller index first.",
                        "2 <= n <= 100000; values and target are signed 32-bit integers; exactly one answer exists.",
                        "import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        // Read n, n integers, then target from standard input.\n    }\n}",
                        2000, 256,
                        test("4\n2 7 11 15\n9\n", "0 1\n", true),
                        test("3\n3 2 4\n6\n", "1 2\n", false)),
                problem("Valid Parentheses", ProblemDifficulty.EASY,
                        "Given a string containing only (), {}, and [], print true if the brackets are balanced and correctly nested; otherwise print false.",
                        "0 <= string length <= 100000.",
                        "import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        // Read one line from standard input.\n    }\n}",
                        2000, 256,
                        test("()[]{}\n", "true\n", true),
                        test("(]\n", "false\n", false)),
                problem("Reverse String", ProblemDifficulty.EASY,
                        "Read a line and print its characters in reverse order.",
                        "0 <= string length <= 100000; input contains one line.",
                        "import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        // Read one line from standard input.\n    }\n}",
                        2000, 256,
                        test("hello\n", "olleh\n", true),
                        test("A man\n", "nam A\n", false)),
                problem("Maximum Subarray", ProblemDifficulty.MEDIUM,
                        "Given an integer array, print the largest sum of a non-empty contiguous subarray.",
                        "1 <= n <= 200000; values are signed 32-bit integers.",
                        "import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        // Read n followed by n integers.\n    }\n}",
                        2000, 256,
                        test("9\n-2 1 -3 4 -1 2 1 -5 4\n", "6\n", true),
                        test("5\n-8 -3 -6 -2 -5\n", "-2\n", false)),
                problem("Binary Search", ProblemDifficulty.EASY,
                        "Given a sorted array and a target, print the target's index or -1 if it is absent. If the target occurs more than once, print its first index.",
                        "0 <= n <= 200000; array values are sorted in non-decreasing order.",
                        "import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        // Read n, n sorted integers, then the target.\n    }\n}",
                        2000, 256,
                        test("6\n-1 0 3 5 9 12\n9\n", "4\n", true),
                        test("4\n1 2 2 2\n2\n", "1\n", false))
        ));
    }

    private Problem problem(String title, ProblemDifficulty difficulty, String statement,
                            String constraints, String starterCode, int timeLimitMs,
                            int memoryLimitMb, TestCase... testCases) {
        Problem problem = new Problem(title, difficulty, Language.JAVA, statement, constraints,
                starterCode, timeLimitMs, memoryLimitMb, Instant.now());
        for (int index = 0; index < testCases.length; index++) {
            TestCase source = testCases[index];
            problem.addTestCase(new TestCase(
                    source.getInput(), source.getExpectedOutput(), source.isSample(), index));
        }
        return problem;
    }

    private TestCase test(String input, String expectedOutput, boolean sample) {
        return new TestCase(input, expectedOutput, sample, 0);
    }
}
