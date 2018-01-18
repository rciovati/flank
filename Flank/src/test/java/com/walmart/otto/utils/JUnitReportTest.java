package com.walmart.otto.utils;

import static com.walmart.otto.utils.JUnitReportParser.matchesTestReport;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import com.walmart.otto.models.TestCase;
import com.walmart.otto.models.TestSuite;
import com.walmart.otto.testsupport.TestUtils;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JUnitReportTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void shouldProperlyReadTestSuiteFromFile() throws Exception {
    Path reportPath = TestUtils.readFileFromResources("test_report_with_failures.xml");

    TestSuite testSuite = JUnitReportParser.readTestSuite(reportPath);
    assertThat(testSuite.getFailuresCount(), equalTo(1));
    assertThat(testSuite.getTestsCount(), equalTo(4));
    assertThat(testSuite.getDurantion(), equalTo(123.0F));

    long failingTestCasesCount =
        testSuite.getTestCaseList().stream().filter(TestCase::isFailure).count();
    assertThat(failingTestCasesCount, equalTo(1L));

    TestCase failure =
        testSuite.getTestCaseList().stream().filter(TestCase::isFailure).findFirst().get();

    assertThat(failure.getExceptionMessage(), equalTo("stacktrace"));
    assertThat(failure.getTestName(), equalTo("test1"));
    assertThat(failure.getClassName(), equalTo("com.foo.Class"));
  }

  @Test
  public void findReportFiles() throws Exception {
    Path reports = TestUtils.readFileFromResources("reports");

    List<Path> pathList = JUnitReportParser.findReportFiles(reports).collect(Collectors.toList());
    assertThat(pathList.size(), equalTo(2));

    Path first = TestUtils.readFileFromResources("reports/0/test_result_0.xml");
    Path second = TestUtils.readFileFromResources("reports/1/test_result_0.xml");
    Path third = TestUtils.readFileFromResources("reports/2/file.txt");

    assertThat(pathList, hasItems(first, second));
    assertThat(pathList, not(hasItem(third)));
  }

  @Test
  public void shouldMatchXmlReportName() throws Exception {
    Path stubFile = folder.newFile("test_result_0.xml").toPath();
    assertThat(matchesTestReport(stubFile), is(true));
  }

  @Test
  public void shouldNotMatchOtherFiles() throws Exception {
    Path stubFile = folder.newFile("logcat").toPath();
    assertThat(matchesTestReport(stubFile), is(false));
  }
}
