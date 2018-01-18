package com.walmart.otto.models;

public class TestCase {

  private final String shardName;
  private final String testName;
  private final String className;
  private final String exceptionMessage;
  private final Result result;
  private final String canonicalName;
  private final String id;

  private TestCase(
      Result result, String shardName, String testName, String className, String exceptionMessage) {
    this.result = result;
    this.shardName = shardName;
    this.testName = testName;
    this.className = className;
    this.exceptionMessage = exceptionMessage;
    this.canonicalName = generateCanonicalName(testName, className);
    this.id = generateId(shardName, canonicalName);
  }

  static String generateId(String shardName, String canonicalName) {
    return shardName + "_ " + canonicalName;
  }

  static String generateCanonicalName(String testName, String className) {
    return className.replaceAll("\\.", "_") + "_" + testName;
  }

  public static TestCase success(String shardName, String testName, String className) {
    return new TestCase(Result.SUCCESS, shardName, testName, className, null);
  }

  public static TestCase failure(
      String shardName, String testName, String className, String exceptionMessage) {
    return new TestCase(Result.FAILURE, shardName, testName, className, exceptionMessage);
  }

  public static TestCase error(
      String shardName, String testName, String className, String exceptionMessage) {
    return new TestCase(Result.ERROR, shardName, testName, className, exceptionMessage);
  }

  public static TestCase skipped(String shardName, String testName, String className) {
    return new TestCase(Result.SKIPPED, shardName, testName, className, null);
  }

  public boolean isFailure() {
    return result == Result.FAILURE;
  }

  public boolean isSuccess() {
    return result == Result.SUCCESS;
  }

  public String getTestName() {
    return testName;
  }

  public String getExceptionMessage() {
    return exceptionMessage;
  }

  public String getClassName() {
    return className;
  }

  public String getShardName() {
    return shardName;
  }

  public Result getResult() {
    return result;
  }

  public String getCanonicalName() {
    return canonicalName;
  }

  public String getId() {
    return id;
  }
}
