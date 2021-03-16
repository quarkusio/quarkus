package io.quarkus.funqy.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class GreetingSyncTest extends GreetTestBase {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("greeting.properties", "application.properties")
                    .addClasses(PrimitiveFunctions.class, GreetingFunctions.class, Greeting.class, GreetingService.class,
                            Identity.class));

    @Override
    protected String getCeSource() {
        return "greet";
    }

    @Override
    protected String getCeType() {
        return "greet.output";
    }

}
