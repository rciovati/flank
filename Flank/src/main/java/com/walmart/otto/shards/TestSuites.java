package com.walmart.otto.shards;

import static java.util.stream.Collectors.groupingBy;

import com.walmart.otto.models.Result;
import com.walmart.otto.models.TestCase;
import com.walmart.otto.models.TestSuite;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TestSuites {

  public static boolean isGloballySuccessful(List<TestSuite> testSuites) {

    boolean hasEmptyTestSuites =
        testSuites.stream().anyMatch(testSuite -> testSuite.getTestsCount() == 0);

    if (hasEmptyTestSuites) {
      return false;
    }

    // Aggregate tests by id (generated using test class and testname)
    // The list of TestCases contains multiple elements only if the same test have been retried
    Map<String, List<TestCase>> testCasesMap =
        testSuites
            .stream()
            .flatMap(testSuite -> testSuite.getTestCaseList().stream())
            .collect(groupingBy(TestCase::getCanonicalName));

    // If all the buckets have at least one success it means all the tests have passed
    return testCasesMap.values().stream().allMatch(TestSuites::isSuccess);
  }

  public static List<TestCase> readFailures(List<TestSuite> suites) {
    return suites
        .stream()
        .flatMap(testSuite -> testSuite.getTestCaseList().stream())
        .filter(TestCase::isFailure)
        .collect(Collectors.toList());
  }

  private static boolean isSuccess(List<TestCase> testCases) {
    long testSuccessCount = testCases.stream().filter(TestCase::isSuccess).count();
    long testFailureCount = testCases.stream().filter(TestCase::isFailure).count();
    return testSuccessCount >= testFailureCount;
  }

  public static TestSuite createSummary(String matrixName, List<TestSuite> suites) {
    double totalDuration = suites.stream().mapToDouble(TestSuite::getDurantion).sum();

    // Aggregate tests by id (generated using test class and testname)
    // The list of TestCases contains multiple elements only if the same test have been retried
    Map<String, List<TestCase>> testCasesMap =
        suites
            .stream()
            .flatMap(testSuite -> testSuite.getTestCaseList().stream())
            .collect(groupingBy(TestCase::getCanonicalName));

    List<TestCase> newTestCaseList = new ArrayList<>();

    for (List<TestCase> testCases : testCasesMap.values()) {
      if (testCases.size() == 1) { // test hasn't been retried
        newTestCaseList.addAll(testCases);
      } else {
        Optional<TestCase> passedTestCase =
            testCases.stream().filter(TestCase::isSuccess).findFirst();

        Optional<TestCase> failedTestCase =
            testCases.stream().filter(TestCase::isFailure).findFirst();

        // if there is a passed test it means the test passed on retry,
        // in the case we can pick the successful one and discard the failing one.
        TestCase testCase =
            passedTestCase.orElseGet(() -> failedTestCase.orElseThrow(IllegalStateException::new));
        newTestCaseList.add(testCase);
      }
    }

    int uniqueTestsCount = testCasesMap.keySet().size();
    int testFailedCount = countTestCasesByResult(newTestCaseList, Result.FAILURE);
    int testErrorCount = countTestCasesByResult(newTestCaseList, Result.ERROR);
    int testSkipped = countTestCasesByResult(newTestCaseList, Result.SKIPPED);

    return new TestSuite(
        matrixName,
        uniqueTestsCount,
        testFailedCount,
        testErrorCount,
        testSkipped,
        (float) totalDuration,
        newTestCaseList);
  }

  private static int countTestCasesByResult(List<TestCase> list, Result result) {
    return (int)
        list.stream().filter(testCase -> testCase.getResult() == result).distinct().count();
  }
}
