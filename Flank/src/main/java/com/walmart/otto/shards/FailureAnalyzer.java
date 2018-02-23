package com.walmart.otto.shards;

import com.walmart.otto.models.Device;
import com.walmart.otto.models.TestCase;
import com.walmart.otto.models.TestSuite;
import com.walmart.otto.tools.GcloudTool.ExecutionResult;
import com.walmart.otto.tools.GsutilTool;
import com.walmart.otto.utils.JUnitReportParser;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FailureAnalyzer {

  private final GsutilTool gsutilTool;

  public FailureAnalyzer(GsutilTool gsutilTool) {
    this.gsutilTool = gsutilTool;
  }

  public List<Retry> calculateTestToRetry(ExecutionResult executionResult) {

    if (!executionResult.isFailure()) {
      return Collections.emptyList();
    }

    File shardReportsDir;
    List<TestSuite> suites;
    try {
      shardReportsDir = gsutilTool.fetchShardResult(executionResult.getShardName());
      ensureTestReportFileExists(shardReportsDir);
      suites = JUnitReportParser.parseReportsInFolder(shardReportsDir.toPath());
    } catch (Exception e) {
      throw new RuntimeException(
          "Error analyzing test report to compute retry. Shard name: " + executionResult
              .getShardName(), e);
    }

    List<TestSuite> testSuiteWithFailures =
        suites.stream().filter(TestSuite::hasFailures).collect(Collectors.toList());

    List<Retry> retries = new ArrayList<>();
    for (TestSuite suite : testSuiteWithFailures) {
      String matrixName = suite.getMatrixName();
      Device device = buildDeviceFromMatrix(matrixName);
      suite
          .getTestCaseList()
          .stream()
          .filter(TestCase::isFailure)
          .map(testCase -> new Retry(testCase.getClassName(), testCase.getTestName(), device))
          .forEach(retries::add);
    }

    return Collections.unmodifiableList(retries);
  }

  private void ensureTestReportFileExists(File shardReportsDir) {
    if (!shardReportsDir.exists()) {
      throw new IllegalStateException("Unable to read report at location: " + shardReportsDir);
    }
  }

  static Device buildDeviceFromMatrix(String matrixName) {
    //use a regex instead of a split is safe in case deviceId has a dash in its name
    Pattern pattern = Pattern.compile("(\\S+)-(\\S+)-(\\S+)-(\\S+)");
    Matcher matcher = pattern.matcher(matrixName);
    if (matcher.find()) {
      return new Device.Builder()
          .setId(matcher.group(1))
          .setOsVersion(matcher.group(2))
          .setLocale(matcher.group(3))
          .setOrientation(matcher.group(4))
          .build();
    }
    throw new IllegalStateException("Unable to parse device from matrixName: " + matrixName);
  }
}
