package com.walmart.otto.models;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class TestCaseTest {

  @Test
  public void shouldGenerateIdWithClassNameAndTestname() throws Exception {
    String testId = TestCase.generateCanonicalName("testName", "com.foo.Bar");
    assertThat(testId, equalTo("com_foo_Bar_testName"));
  }
}
