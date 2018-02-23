package com.walmart.otto.shards;

import static com.walmart.otto.shards.ShardCreator.generateTestCaseName;

import com.walmart.otto.configurator.Configurator;
import com.walmart.otto.models.Device;
import com.walmart.otto.tools.GcloudTool;
import com.walmart.otto.tools.GcloudTool.ExecutionResult;
import com.walmart.otto.tools.GsutilTool;
import com.walmart.otto.tools.ToolManager;
import com.walmart.otto.utils.FilterUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ShardExecutor {

  private final FailureAnalyzer failureAnalyzer;
  private final ExecutorService executorService;
  private final ShardCreator shardCreator;
  private final String bucket;
  private final Configurator configurator;
  private final GsutilTool gsutilTool;
  private final GcloudTool gcloudTool;

  public ShardExecutor(Configurator configurator, ToolManager toolManager, String bucket) {
    this.configurator = configurator;
    this.shardCreator = new ShardCreator(configurator);
    this.bucket = bucket;
    this.executorService = Executors.newCachedThreadPool();
    this.gsutilTool = toolManager.get(GsutilTool.class);
    this.gcloudTool = toolManager.get(GcloudTool.class);
    this.failureAnalyzer = new FailureAnalyzer(gsutilTool);
  }

  public void execute(List<String> testCases) {
    List<CompletableFuture> futures = new ArrayList<>();
    List<String> shards = shardCreator.getConfigurableShards(testCases);

    if (shards.isEmpty()) {
      shards = shardCreator.getShards(testCases);
    }

    int shardIndex = configurator.getShardIndex();
    if (shardIndex != -1) {
      String shardName = String.valueOf(shardIndex);
      printTests(shards.get(shardIndex), shardName);
      futures.add(createShardFuture(shards.get(shardIndex), shardName));
    } else {
      printExecutionSummary(shards);

      for (int i = 0; i < shards.size(); i++) {
        String shardName = String.valueOf(i);
        String testsInShard = shards.get(i);

        printTests(testsInShard, shardName);
        futures.add(createShardFuture(testsInShard, shardName));
      }
    }

    CompletableFuture[] futuresArray = futures.toArray(new CompletableFuture[futures.size()]);
    CompletableFuture.allOf(futuresArray).join();
  }

  private void printExecutionSummary(List<String> shards) {
    String deviceIds =
        configurator
            .getDevices()
            .stream()
            .map(Device::getId)
            .reduce((s, s2) -> s + ", " + s2)
            .orElseThrow(() -> new IllegalStateException("Device ids is empty"));
    System.out.printf("%d shards will be executed on: %s%n", shards.size(), deviceIds);
  }

  private CompletableFuture createShardFuture(String testCase, String shardName) {
    return CompletableFuture.supplyAsync(() -> executeShard(testCase, shardName), executorService)
        .thenComposeAsync(this::retryTestsIfNeeded);
  }

  private CompletableFuture retryTestsIfNeeded(ExecutionResult executionResult) {

    try {
      System.out.println("Waiting 10 seconds before fetching test report for " + executionResult.getShardName());
      Thread.sleep(TimeUnit.SECONDS.toMillis(10));
    } catch (InterruptedException e) {
      System.out.println("Failed to wait 10 seconds before fetching test report for " + executionResult.getShardName());
    }

    List<Retry> retries = failureAnalyzer.calculateTestToRetry(executionResult);

    if (retries.isEmpty()) {
      System.out.printf("Shard %s completed without failures%n", executionResult.getShardName());
      return CompletableFuture.completedFuture(executionResult);
    }

    System.out.printf(
        "Shard %s completed with errors, will retry %d tests%n",
        executionResult.getShardName(), retries.size());

    CompletableFuture[] futures = new CompletableFuture[retries.size()];

    for (int i = 0; i < retries.size(); i++) {
      Retry retry = retries.get(i);

      String testCase = generateTestCaseName(retry.getClassName(), retry.getTestName());
      String newShardName = getNewShardName(executionResult, i);
      List<Device> devices = Collections.singletonList(retry.getTargetDevice());

      futures[i] =
          CompletableFuture.supplyAsync(
              () -> executeShard(testCase, newShardName, devices), executorService);
    }

    return CompletableFuture.allOf(futures);
  }

  private String getNewShardName(ExecutionResult executionResult, int i) {
    return executionResult.getShardName() + "-retry" + i;
  }

  private ExecutionResult executeShard(String testCase, String shardName) {
    try {
      return gcloudTool.runGcloud(testCase, bucket, shardName);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private ExecutionResult executeShard(String testCase, String shardName, List<Device> devices) {
    try {
      return gcloudTool.runGcloud(testCase, bucket, shardName, devices);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void printTests(String testsString, String shardName) {
    String tests = FilterUtils.filterString(testsString, "class");
    if (tests.length() > 0 && tests.charAt(tests.length() - 1) == ',') {
      tests = tests.substring(0, tests.length() - 1);
    }
    System.out.println("Executing shard " + shardName + ": " + tests + "\n");
  }
}
