package org.acme;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GreetingResourceTest {
  @Test
  void shouldReturnExpectedValue() {
    String actual = new GreetingResource().hello();
    Assertions.assertEquals(actual, GreetingResource.HELLO);
  }
}