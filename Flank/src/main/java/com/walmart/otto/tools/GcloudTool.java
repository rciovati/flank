package com.walmart.otto.tools;

import static com.walmart.otto.utils.FileUtils.getSimpleName;

import com.walmart.otto.models.Device;
import com.walmart.otto.reporter.TimeReporter;
import com.walmart.otto.utils.FileUtils;
import com.walmart.otto.utils.FilterUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GcloudTool extends Tool {

  private boolean printTests = true;
  private boolean testFailed = false;
  private int latestExecutionTime;

  public GcloudTool(ToolManager.Config config) {
    super(ToolManager.GCLOUD_TOOL, config);
  }

  public ExecutionResult runGcloud(String testCase, String bucket, String shardName)
      throws IOException, InterruptedException {
    return runGcloud(testCase, bucket, shardName, getConfigurator().getDevices());
  }

  public ExecutionResult runGcloud(
      String testCase, String bucket, String shardName, List<Device> devices)
      throws IOException, InterruptedException {
    // don't quote arguments or ProcessBuilder will error in strange ways.
    String[] runGcloud =
        new String[] {
          getConfigurator().getGcloud(),
          betaConfiguration(),
          "firebase",
          "test",
          "android",
          "run",
          "--type",
          "instrumentation",
          orchestratorFlag(),
          "--app",
          bucket + getSimpleName(getAppAPK()),
          "--test",
          bucket + getSimpleName(getTestAPK()),
          "--results-bucket",
          "gs://" + bucket.split("/")[2],
          "--timeout",
          getConfigurator().getShardTimeout() + "m",
          "--results-dir",
          bucket.split("/")[3] + "/" + shardName,
          "--test-targets",
          testCase
        };

    List<String> gcloudList = new ArrayList<>(Arrays.asList(runGcloud));
    for (Device device : devices) {
      gcloudList.add("--device");
      gcloudList.add(device.toString());
    }

    addParameterIfValueSet(
        gcloudList, "--environment-variables", getConfigurator().getEnvironmentVariables());

    addParameterIfValueSet(
        gcloudList, "--directories-to-pull", getConfigurator().getDirectoriesToPull());

    gcloudList =
        gcloudList
            .stream()
            .filter(param -> param != null && !param.isEmpty())
            .collect(Collectors.toList());

    String[] cmdArray = gcloudList.toArray(new String[0]);

    return executeGcloud(cmdArray, testCase, shardName);
  }

  private String orchestratorFlag() {
    if (getConfigurator().isUseOrchestrator()) {
      return "--use-orchestrator";
    }
    return "--no-use-orchestrator";
  }

  private String betaConfiguration() {
    if (getConfigurator().isUseGCloudBeta()) {
      return "beta";
    }
    return "";
  }

  private void addParameterIfValueSet(List<String> list, String parameter, String value) {
    if (!value.isEmpty()) {
      list.add(parameter);
      list.add(value);
    }
  }

  public boolean hasTestFailed() {
    return testFailed;
  }

  private ExecutionResult executeGcloud(String[] commands, String test, String shardName)
      throws RuntimeException, IOException, InterruptedException {
    List<String> inputStreamList = new ArrayList<>();
    List<String> errorStreamList = new ArrayList<>();

    String resultsLink = null;

    executeCommand(commands, inputStreamList, errorStreamList);

    for (String line : errorStreamList) {
      if (line.contains("More details are available")) {
        resultsLink = line;
      } else if (line.contains("Test time=")) {
        String[] timeLine = line.split(Pattern.quote("time="));
        latestExecutionTime = Integer.parseInt(timeLine[1].replaceAll("\\D+", ""));
        TimeReporter.addExecutionTime(Integer.parseInt(timeLine[1].replaceAll("\\D+", "")));
      } else if (line.contains("ERROR")) {
        //TODO retry when error is returned from FTL
        //throw new RuntimeException(line);
      }
    }

    for (String line : inputStreamList) {
      if (line.contains("Failed")) {
        testFailed = true;
      }
      if (printTests) {
        printTests(test);
        printTests = false;
      }

      System.out.println(line);
    }

    printTests = true;

    if (resultsLink != null) {
      System.out.println("\n" + resultsLink + "\n");
    }

    if (testFailed) {
      return ExecutionResult.failure(shardName);
    } else {
      return ExecutionResult.success(shardName);
    }
  }

  public String getProjectName() throws IOException, InterruptedException {
    String[] projectDetails =
        new String[] {getConfigurator().getGcloud(), "config", "get-value", "project"};
    List<String> inputStreamList = new ArrayList<>();
    String projectName = "";

    executeCommand(projectDetails, inputStreamList, new ArrayList<>());

    for (String projectProperties : inputStreamList) {
      projectName = projectProperties;
    }
    return projectName;
  }

  private void printTests(String test) {
    test = FilterUtils.filterString(test, "class");

    if (test.charAt(test.length() - 1) == ',') {
      test = test.substring(0, test.length() - 1);
    }

    if (!test.contains(",")) {
      //Save test case name and execution times
      FileUtils.writeToShardFile(test, String.valueOf(latestExecutionTime));
    }

    System.out.println("Test(s):" + test);
  }

  public static class ExecutionResult {

    private final boolean isFailure;
    private final String shardName;

    private ExecutionResult(boolean isFailure, String shardName) {
      this.isFailure = isFailure;
      this.shardName = shardName;
    }

    public static ExecutionResult success(String shardIndex) {
      return new ExecutionResult(false, shardIndex);
    }

    public static ExecutionResult failure(String shardIndex) {
      return new ExecutionResult(true, shardIndex);
    }

    public boolean isFailure() {
      return isFailure;
    }

    public String getShardName() {
      return shardName;
    }
  }
}
