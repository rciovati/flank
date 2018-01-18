package com.walmart.otto.shards;

import com.walmart.otto.models.Device;

public class Retry {

  private final String className;
  private final String testName;
  private final Device targetDevice;

  public Retry(String className, String testName, Device targetDevice) {
    this.className = className;
    this.testName = testName;
    this.targetDevice = targetDevice;
  }

  public String getClassName() {
    return className;
  }

  public String getTestName() {
    return testName;
  }

  public Device getTargetDevice() {
    return targetDevice;
  }
}
