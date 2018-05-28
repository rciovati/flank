package com.walmart.otto.aggregator;

import static com.walmart.otto.testsupport.TestUtils.readFileContent;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.walmart.otto.testsupport.TestUtils;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ReportUrlsFileWriterTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void shouldWriteJsonArray() throws IOException, URISyntaxException {

    File outputFile = folder.newFile();

    ReportUrlsFileWriter.write(
        outputFile.toPath(), Arrays.asList("link1", "link2"), Collections.emptyList());

    Path actual = outputFile.toPath();
    String actualContent = readFileContent(actual);

    Path expected = TestUtils.readFileFromResources("reports-file.json");
    String expectedContent = readFileContent(expected);

    assertThat(actualContent, is(expectedContent));
  }
}
