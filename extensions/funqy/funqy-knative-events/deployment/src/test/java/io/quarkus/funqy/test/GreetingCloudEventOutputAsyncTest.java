package io.quarkus.funqy.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class GreetingCloudEventOutputAsyncTest extends GreetTestBase {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("greeting-ce-async.properties", "application.properties")
                    .addClasses(PrimitiveFunctions.class, GreetingFunctions.class, Greeting.class, GreetingService.class,
                            Identity.class));

    @Override
    protected String getCeSource() {
        return "async-custom-source";
    }

    @Override
    protected String getCeType() {
        return "async-custom-type";
    }

}
