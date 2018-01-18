package com.walmart.otto.models;

import java.util.Arrays;
import java.util.Collections;

public class Fixtures {

  public static TestSuite testSuite() {
    return new TestSuite(
        "matrixName",
        2,
        1,
        0,
        0,
        10,
        Arrays.asList(
            TestCase.failure("1", "testFailure", "ClassName", "stacktrace"),
            TestCase.success("1", "testSuccess", "ClassName")));
  }

  public static TestSuite testSuiteWithFailure() {
    return new TestSuite(
        "matrixName",
        1,
        1,
        0,
        0,
        10,
        Collections.singletonList(TestCase.failure("1", "testName", "ClassName", "stacktrace")));
  }

  public static TestSuite testSuiteWithSuccess() {
    return new TestSuite(
        "matrixName",
        1,
        0,
        0,
        0,
        10,
        Collections.singletonList(TestCase.success("1-retry0", "testName", "ClassName")));
  }
}
