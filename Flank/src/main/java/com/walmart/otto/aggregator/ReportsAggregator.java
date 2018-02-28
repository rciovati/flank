package com.walmart.otto.aggregator;

import static java.util.stream.Collectors.groupingBy;

import com.walmart.otto.configurator.Configurator;
import com.walmart.otto.models.TestCase;
import com.walmart.otto.models.TestSuite;
import com.walmart.otto.shards.TestSuites;
import com.walmart.otto.tools.GsutilTool;
import com.walmart.otto.utils.FileUtils;
import com.walmart.otto.utils.JUnitReportParser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ReportsAggregator {

  private final HtmlReport htmlReport;
  private final ArtifactsProcessor artifactsProcessor;
  private final GsutilTool gsutilTool;
  private final Configurator configurator;

  public ReportsAggregator(Configurator configurator, GsutilTool gsutilTool) {
    this.artifactsProcessor = new ArtifactsProcessor(configurator);
    this.gsutilTool = gsutilTool;
    this.configurator = configurator;
    this.htmlReport = new HtmlReport(configurator);
  }

  public void aggregate(Path reportsBaseDir, File reportUrlsFile)
      throws XmlReportGenerationException, HtmlReportGenerationException, IOException,
          InterruptedException {
    String baseUrl = generateCloudStorageReportsBaseUrl(reportsBaseDir);

    System.out.println("Generating combined reports starting from: " + reportsBaseDir.toString());

    List<TestSuite> testSuites = JUnitReportParser.parseReportsInFolder(reportsBaseDir);

    Map<String, List<TestSuite>> map =
        testSuites.stream().collect(groupingBy(TestSuite::getMatrixName));

    List<String> htmlReportUrls = new ArrayList<>();
    List<String> xmlReportUrls = new ArrayList<>();

    for (Entry<String, List<TestSuite>> entry : map.entrySet()) {

      String matrixName = entry.getKey();
      List<TestSuite> suites = entry.getValue();

      TestSuite summaryTestSuite = TestSuites.createSummary(matrixName, suites);

      if (configurator.isGenerateAggregatedXmlReport()) {
        Path xmlOutputFile = reportsBaseDir.resolve(matrixName + "_results.xml");

        JunitReportWriter.generate(xmlOutputFile, summaryTestSuite);
        gsutilTool.uploadAggregatedXmlFiles(reportsBaseDir.toFile());

        String xmlReportUrl = baseUrl + "/" + FileUtils.getFileName(xmlOutputFile);
        xmlReportUrls.add(xmlReportUrl);

        System.out.println("XML report uploaded to: " + xmlReportUrl);
      }

      if (configurator.isGenerateAggregatedHtmlReport()) {
        Path htmlOutputFile = reportsBaseDir.resolve(matrixName + "_results.html");

        summaryTestSuite
            .getTestCaseList()
            .stream()
            .filter(TestCase::isFailure)
            .forEach(testCase -> processArtifacts(reportsBaseDir, matrixName, testCase));

        htmlReport.generate(baseUrl, htmlOutputFile, summaryTestSuite);
        gsutilTool.uploadAggregatedHtmlReports(reportsBaseDir.toFile());

        String htmlReportUrl = baseUrl + "/" + FileUtils.getFileName(htmlOutputFile);
        htmlReportUrls.add(htmlReportUrl);

        System.out.println("HTML report uploaded to: " + htmlReportUrl);
      }
    }

    if (reportUrlsFile != null) {
      ReportUrlsFileWriter.write(reportUrlsFile.toPath(), htmlReportUrls, xmlReportUrls);
      System.out.println("Written report urls to file: " + reportUrlsFile.getAbsolutePath());
    } else {
      System.out.println("Skipping writing the result urls to file.");
    }
  }

  private String generateCloudStorageReportsBaseUrl(Path reportBaseDir) {
    return "https://storage.cloud.google.com/"
        + configurator.getProjectName()
        + "/"
        + FileUtils.getFileName(reportBaseDir);
  }

  private void processArtifacts(Path reportBaseDir, String matrixName, TestCase testCase) {
    try {
      artifactsProcessor.generateArtifactsForTestCase(reportBaseDir, matrixName, testCase);
    } catch (IOException e) {
      System.out.println("Can't process test artifacts for: " + testCase.getTestName());
      throw new RuntimeException(e);
    }
  }
}
