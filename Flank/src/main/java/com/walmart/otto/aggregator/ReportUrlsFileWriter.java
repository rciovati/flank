package com.walmart.otto.aggregator;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ReportUrlsFileWriter {

  public static void write(Path filePath, List<String> htmlReports, List<String> xmlReports) {
    String json =
        String.format(
            "{\"htmlReports\": %s, \"xmlReports\": %s}",
            toJsonArray(htmlReports), toJsonArray(xmlReports));
    try {
      Files.write(filePath, json.getBytes(Charset.defaultCharset()));
    } catch (IOException e) {
      throw new RuntimeException("Unable to write report urls file", e);
    }
  }

  private static String toJsonArray(List<String> strings) {
    return strings
        .stream()
        .map(s -> String.format("\"%s\"", s))
        .collect(Collectors.joining(",", "[", "]"));
  }
}
