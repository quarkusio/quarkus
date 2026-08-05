package org.acme.my.project.itest.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class ItestTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest() // Start unit test with your extension loaded
            .withEmptyApplication();

    @Test
    public void test() {
        // Write your tests here - see the testing extension guide https://quarkus.io/guides/writing-extensions#testing-extensions for more information
        Assertions.fail("Add some assertions to " + getClass().getName());
    }

}
