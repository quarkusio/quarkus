package io.quarkus.xmlsecurity.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class XmlsecurityTest {

    // Start unit test with your extension loaded
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
        .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void writeYourOwnUnitTest() {
        // Write your unit tests here - see the testing extension guide https://quarkus.io/guides/writing-extensions#testing-extensions for more information
        Assertions.assertTrue(true, "Add some assertions to " + getClass().getName());
    }
}
