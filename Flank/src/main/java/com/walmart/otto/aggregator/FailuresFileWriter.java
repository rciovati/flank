package com.walmart.otto.aggregator;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.walmart.otto.models.TestCase;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

class FailuresFileWriter {

  public static void write(Path filePath, List<TestCase> testCases) {

    if (testCases.size() == 0) {
      return;
    }

    List<Failure> failures =
        testCases.stream().map(FailuresFileWriter::newFailure).collect(Collectors.toList());

    Moshi moshi = new Moshi.Builder().build();
    Type type = Types.newParameterizedType(List.class, Failure.class);
    String json = moshi.adapter(type).toJson(failures);

    try {
      Files.write(filePath, json.getBytes(Charset.defaultCharset()));
    } catch (IOException e) {
      throw new RuntimeException("Unable to write failures file", e);
    }
  }

  private static Failure newFailure(TestCase testCase) {
    return new Failure(testCase.getClassName(), testCase.getTestName());
  }

  private static class Failure {

    public final String class_name;
    public final String test_name;

    private Failure(String class_name, String test) {
      this.class_name = class_name;
      this.test_name = test;
    }
  }
}
