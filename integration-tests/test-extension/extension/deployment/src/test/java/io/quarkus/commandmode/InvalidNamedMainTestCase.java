package io.quarkus.commandmode;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class InvalidNamedMainTestCase {
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsManifestResource("application.properties", "microprofile-config.properties")
                    .addClasses(JavaMain.class, HelloWorldNonDefault.class, NamedMain.class))
            .setApplicationName("run-exit")
            .overrideConfigKey("quarkus.package.main-class", "wrong")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectedException(IllegalArgumentException.class);

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked because of invalid main class");
    }

}
