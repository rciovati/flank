package com.walmart.otto.utils;

import static java.util.stream.Collectors.toList;

import com.walmart.otto.models.TestCase;
import com.walmart.otto.models.TestSuite;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JUnitReportParser {

  private static final String TESTCASE_TAG = "testcase";
  private static final String TESTSUITE_TAG = "testsuite";
  private static final String ERROR_TAG = "error";
  private static final String FAILURE_TAG = "failure";
  private static final String SKIPPED_TAG = "skipped";

  private static final String NAME_ATTRIBUTE = "name";
  private static final String CLASSNAME_ATTRIBUTE = "classname";
  private static final String TESTS_ATTRIBUTE = "tests";
  private static final String FAILURES_ATTRIBUTE = "failures";
  private static final String ERRORS_ATTRIBUTE = "errors";
  private static final String SKIPPED_ATTRIBUTE = "skipped";
  private static final String TIME_ATTRIBUTE = "time";

  public static List<TestSuite> parseReportsInFolder(Path reportsBaseDir) throws IOException {
    return findReportFiles(reportsBaseDir).map(JUnitReportParser::readTestSuite).collect(toList());
  }

  static Stream<Path> findReportFiles(Path reportsBaseDir) throws IOException {
    return Files.find(reportsBaseDir, 3, (path, basicFileAttributes) -> matchesTestReport(path));
  }

  static boolean matchesTestReport(Path path) {
    return FileUtils.getFileName(path).startsWith("test_result_");
  }

  public static TestSuite readTestSuite(Path filePath) {

    String shardName = extractShardName(filePath);
    String matrixName = extractMatrixName(filePath);

    Document document = XMLUtils.getXMLFile(filePath.toFile().getAbsolutePath());

    List<TestCase> results = readTestCases(document, shardName);

    NodeList elements = document.getElementsByTagName(TESTSUITE_TAG);
    Element testSuite = (Element) elements.item(0);

    String testCount = testSuite.getAttribute(TESTS_ATTRIBUTE);
    String failuresCount = testSuite.getAttribute(FAILURES_ATTRIBUTE);
    String errorsCount = testSuite.getAttribute(ERRORS_ATTRIBUTE);
    String skippedCount = testSuite.getAttribute(SKIPPED_ATTRIBUTE);
    String duration = testSuite.getAttribute(TIME_ATTRIBUTE);

    return new TestSuite(
        matrixName,
        Integer.parseInt(testCount),
        Integer.parseInt(failuresCount),
        Integer.parseInt(errorsCount),
        Integer.parseInt(skippedCount),
        Float.parseFloat(duration),
        results);
  }

  private static List<TestCase> readTestCases(Document document, String shardName) {
    NodeList nodes = document.getElementsByTagName(TESTCASE_TAG);
    List<TestCase> testCases = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (node instanceof Element) {
        Element testCaseNode = (Element) node;
        TestCase testCase = readSingleTestCase(testCaseNode, shardName);
        testCases.add(testCase);
      }
    }
    return testCases;
  }

  private static TestCase readSingleTestCase(Element testCaseElement, String shardName) {
    String testName = testCaseElement.getAttribute(NAME_ATTRIBUTE);
    String className = testCaseElement.getAttribute(CLASSNAME_ATTRIBUTE);

    boolean hasChildNodes = testCaseElement.hasChildNodes();

    if (hasChildNodes) {
      Node firstElement = extractFirstChildElement(testCaseElement);
      String message = firstElement.getTextContent().trim();
      switch (firstElement.getNodeName()) {
        case FAILURE_TAG:
          return TestCase.failure(shardName, testName, className, message);
        case ERROR_TAG:
          return TestCase.error(shardName, testName, className, message);
        case SKIPPED_TAG:
          return TestCase.skipped(shardName, testName, className);
        default:
          throw new IllegalStateException("Unable to process element: " + firstElement);
      }
    }

    return TestCase.success(shardName, testName, className);
  }

  private static Node extractFirstChildElement(Element testCaseNode) {
    Node firstElement = null;

    NodeList childNodes = testCaseNode.getChildNodes();
    for (int j = 0; j < childNodes.getLength(); j++) {
      Node item = childNodes.item(j);
      if (item.getNodeType() == Node.ELEMENT_NODE) {
        firstElement = item;
        break;
      }
    }

    if (firstElement == null) {
      throw new IllegalStateException();
    }
    return firstElement;
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private static String extractShardName(Path filePath) {
    return filePath.getParent().getParent().getFileName().toString();
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private static String extractMatrixName(Path filePath) {
    return filePath.getParent().getFileName().toString();
  }
}
