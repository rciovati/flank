package com.walmart.otto.aggregator;

import com.walmart.otto.models.TestCase;
import com.walmart.otto.models.TestSuite;
import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JunitReportWriter {

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
  private static final String HOSTNAME_ATTRIBUTE = "hostname";

  public static void generate(Path outputFile, TestSuite testSuite)
      throws XmlReportGenerationException {

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setExpandEntityReferences(false);

    DocumentBuilder documentBuilder;
    try {
      documentBuilder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new XmlReportGenerationException(e);
    }

    Document document = documentBuilder.newDocument();
    document.setXmlStandalone(false);

    long tests = testSuite.getTestsCount();
    long failures = testSuite.getFailuresCount();
    long errors = testSuite.getErrorsCount();
    long skipped = testSuite.getSkippedCount();
    double duration = testSuite.getDurantion();

    Element testSuiteElement = document.createElement(TESTSUITE_TAG);
    testSuiteElement.setAttribute(TESTS_ATTRIBUTE, Long.toString(tests));
    testSuiteElement.setAttribute(FAILURES_ATTRIBUTE, Long.toString(failures));
    testSuiteElement.setAttribute(ERRORS_ATTRIBUTE, Long.toString(errors));
    testSuiteElement.setAttribute(SKIPPED_ATTRIBUTE, Long.toString(skipped));
    testSuiteElement.setAttribute(TIME_ATTRIBUTE, new DecimalFormat("#0.00").format(duration));
    testSuiteElement.setAttribute(HOSTNAME_ATTRIBUTE, "localhost");

    document.appendChild(testSuiteElement);

    for (TestCase testCase : testSuite.getTestCaseList()) {
      Element testCaseElement = document.createElement(TESTCASE_TAG);
      testCaseElement.setAttribute(NAME_ATTRIBUTE, testCase.getTestName());
      testCaseElement.setAttribute(CLASSNAME_ATTRIBUTE, testCase.getClassName());

      String exceptionMessage = testCase.getExceptionMessage();
      switch (testCase.getResult()) {
        case SKIPPED:
          Element skippedElement = document.createElement(SKIPPED_TAG);
          testCaseElement.appendChild(skippedElement);
          break;
        case ERROR:
          Element errorElement = document.createElement(ERROR_TAG);
          errorElement.setTextContent(exceptionMessage);
          testCaseElement.appendChild(errorElement);
          break;
        case FAILURE:
          Element failureElement = document.createElement(FAILURE_TAG);
          failureElement.setTextContent(exceptionMessage);
          testCaseElement.appendChild(failureElement);
          break;
        case SUCCESS:
          //nothing to do here, the success test case doesn't have child nodes.
          break;
        default:
          throw new IllegalStateException("Invalid test case result");
      }

      testSuiteElement.appendChild(testCaseElement);
    }

    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      BufferedWriter outputStream = Files.newBufferedWriter(outputFile, Charset.forName("UTF-8"));

      StreamResult result = new StreamResult(outputStream);

      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

      transformer.transform(new DOMSource(document), result);
    } catch (Exception e) {
      throw new XmlReportGenerationException(e);
    }

    System.out.println("XML report written to: " + outputFile.toString());
  }
}
