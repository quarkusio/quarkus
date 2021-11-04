package io.quarkus.funqy.test;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class GreetingSyncTest extends GreetTestBase {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
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
