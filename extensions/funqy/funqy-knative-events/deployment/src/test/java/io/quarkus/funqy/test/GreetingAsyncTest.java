package io.quarkus.funqy.test;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class GreetingAsyncTest extends GreetTestBase {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("greeting-async.properties", "application.properties")
                    .addClasses(PrimitiveFunctions.class, GreetingFunctions.class, Greeting.class, GreetingService.class,
                            Identity.class));

    @Override
    protected String getCeSource() {
        return "greetAsync";
    }

    @Override
    protected String getCeType() {
        return "greetAsync.output";
    }
}
