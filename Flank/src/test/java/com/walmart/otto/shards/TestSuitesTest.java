package com.walmart.otto.shards;

import static com.walmart.otto.shards.TestSuites.isGloballySuccessful;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.walmart.otto.models.Fixtures;
import com.walmart.otto.models.TestSuite;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class TestSuitesTest {

  @Test
  public void isGloballySuccessfulIfThereIsAtLeastOneSuccessForEachTest() {
    TestSuite testSuiteWithFailure = Fixtures.testSuiteWithFailure();
    TestSuite testSuiteWithSuccess = Fixtures.testSuiteWithSuccess();

    List<TestSuite> testSuites = Arrays.asList(testSuiteWithFailure, testSuiteWithSuccess);

    assertThat(isGloballySuccessful(testSuites), is(true));
  }

  @Test
  public void isNotGloballySuccessfulIfThereIsNotAtLeastOneSuccessForEachTest() {
    TestSuite firstSuite = Fixtures.testSuiteWithFailure();
    TestSuite secondSuite = Fixtures.testSuiteWithFailure();

    List<TestSuite> testSuites = Arrays.asList(firstSuite, secondSuite);

    assertThat(isGloballySuccessful(testSuites), is(false));
  }

  @Test
  public void shouldCreateSummaryWithSuccessForTestsPassedOnRetry() {
    TestSuite testSuiteWithFailure = Fixtures.testSuiteWithFailure();
    TestSuite testSuiteWithSuccess = Fixtures.testSuiteWithSuccess();

    List<TestSuite> testSuites = Arrays.asList(testSuiteWithFailure, testSuiteWithSuccess);

    TestSuite summary = TestSuites.createSummary("matrixName", testSuites);
    assertThat(summary.hasFailures(), is(false));
    assertThat(summary.getTestsCount(), is(1));
    assertThat(summary.getFailuresCount(), is(0));
    assertThat(summary.getDurantion(), is(20f));
  }

  @Test
  public void shouldCreateSummaryWithFailureForTestsFailedOnRetry() {
    TestSuite firstSuite = Fixtures.testSuiteWithFailure();
    TestSuite secondSuite = Fixtures.testSuiteWithFailure();

    List<TestSuite> testSuites = Arrays.asList(firstSuite, secondSuite);

    TestSuite summary = TestSuites.createSummary("matrixName", testSuites);
    assertThat(summary.hasFailures(), is(true));
    assertThat(summary.getTestsCount(), is(1));
    assertThat(summary.getFailuresCount(), is(1));
    assertThat(summary.getDurantion(), is(20f));
  }
}
