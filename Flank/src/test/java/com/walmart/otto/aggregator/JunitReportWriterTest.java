package com.walmart.otto.aggregator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.walmart.otto.models.Fixtures;
import com.walmart.otto.models.TestSuite;
import com.walmart.otto.testsupport.TestUtils;
import com.walmart.otto.testsupport.XmlUtils;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JunitReportWriterTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void shouldGenerateAggregateReportFromTestSuites() throws Exception {
    TestSuite testSuite = Fixtures.testSuite();

    Path outputFile = folder.newFile().toPath();

    JunitReportWriter.generate(outputFile, testSuite);

    Path expectedFile = TestUtils.readFileFromResources("combined_test_report.xml");

    assertThat(XmlUtils.haveSameContent(outputFile, expectedFile), is(true));
  }
}
