package io.quarkus.commandmode;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class AbstractQuarkusMainTestCase {
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsManifestResource("application.properties", "microprofile-config.properties")
                    .addClasses(AbstractQuarkusMain.class))
            .setApplicationName("run-exit")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectedException(RuntimeException.class);

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked because @QuarkusMain on abstract class should fail at build time");
    }

}
