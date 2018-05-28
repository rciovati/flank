package com.walmart.otto.aggregator;

import static com.walmart.otto.testsupport.TestUtils.readFileContent;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.walmart.otto.models.TestCase;
import com.walmart.otto.testsupport.TestUtils;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FailuresFileWriterTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void shouldWriteJsonArray() throws IOException, URISyntaxException {

    File outputFile = folder.newFile();

    List<TestCase> testCases =
        Collections.singletonList(TestCase.failure("shard", "testOne", "classOne", ""));

    FailuresFileWriter.write(outputFile.toPath(), testCases);

    Path actual = outputFile.toPath();
    String actualContent = readFileContent(actual);

    Path expected = TestUtils.readFileFromResources("failures-array.json");
    String expectedContent = readFileContent(expected);

    assertThat(actualContent, is(expectedContent));
  }

  @Test
  public void doesntWriteFailuresFileWhenNoFailures() {

    File outputFile = new File("i-should-not-exist");

    assertThat(outputFile.exists(), is(false));

    FailuresFileWriter.write(outputFile.toPath(), Collections.emptyList());

    assertThat(outputFile.exists(), is(false));
  }
}
