package com.walmart.otto.aggregator;

import com.squareup.moshi.Moshi;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class ReportUrlsFileWriter {

  public static void write(Path filePath, List<String> htmlReports, List<String> xmlReports) {

    String json =
        new Moshi.Builder()
            .build()
            .adapter(Report.class)
            .toJson(new Report(htmlReports, xmlReports));

    try {
      Files.write(filePath, json.getBytes(Charset.defaultCharset()));
    } catch (IOException e) {
      throw new RuntimeException("Unable to write report urls file", e);
    }
  }

  private static class Report {

    public final List<String> html_reports;
    public final List<String> xml_reports;

    private Report(List<String> html_reports, List<String> xml_reports) {
      this.html_reports = html_reports;
      this.xml_reports = xml_reports;
    }
  }
}
