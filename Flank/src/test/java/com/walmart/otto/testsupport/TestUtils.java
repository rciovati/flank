package com.walmart.otto.testsupport;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUtils {

  public static Path readFileFromResources(String name) throws URISyntaxException {
    URL resource = TestUtils.class.getClassLoader().getResource(name);
    return Paths.get(resource.toURI());
  }

  public static String readFileContent(Path path) throws IOException {
    return new String(Files.readAllBytes(path), Charset.defaultCharset()).trim();
  }
}
