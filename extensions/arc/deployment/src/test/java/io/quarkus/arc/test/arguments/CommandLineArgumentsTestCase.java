package io.quarkus.arc.test.arguments;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.annotations.CommandLineArguments;
import io.quarkus.test.QuarkusUnitTest;

public class CommandLineArgumentsTestCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setCommandLineParameters("Hello", "World")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    @CommandLineArguments
    String[] args;

    @Test
    public void testConfigWasInjected() {
        Assertions.assertArrayEquals(new String[] { "Hello", "World" }, args);
    }

}
