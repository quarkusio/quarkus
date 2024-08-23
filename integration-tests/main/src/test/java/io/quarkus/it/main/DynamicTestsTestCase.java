package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import io.quarkus.it.arc.UnusedBean;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DynamicTestsTestCase {

    @Inject
    UnusedBean bean;

    @Test
    public void testInjection() {
        assertNotNull(bean);
    }

    @TestFactory
    public List<DynamicTest> dynamicTests() {
        return Arrays.asList(
                DynamicTest.dynamicTest("test 1", () -> {
                    assertNotNull(bean);
                }),
                DynamicTest.dynamicTest("test 2", () -> {
                    assertEquals(1, 1);
                }));
    }
}
