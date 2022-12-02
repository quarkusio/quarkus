package org.acme.myproject.with.grand.parent.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class WithGrandParentDevModeTest {
    @RegisterExtension
    static final QuarkusDevModeTest devModeTest = new QuarkusDevModeTest() // Start hot reload (DevMode) test with your extension loaded
        .withEmptyApplication();

    @Test
    public void test() {
        // Write your tests here - see the testing extension guide https://quarkus.io/guides/writing-extensions#testing-hot-reload for more information
        Assertions.fail("Add dev mode assertions to " + getClass().getName());
    }

}
