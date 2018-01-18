package com.walmart.otto.utils;

import java.time.Duration;

public class TimeUtils {

  public static String formatDuration(Duration duration) {
    long seconds = duration.getSeconds();
    if (seconds < 0) {
      throw new IllegalArgumentException("invalid negative duration");
    }
    return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
  }
}
