package io.quarkus.commandmode;

import static org.assertj.core.api.Assertions.assertThat;
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
            .assertBuildException(t -> assertThat(t)
                    .hasStackTraceContaining("The @QuarkusMain class '" + AbstractQuarkusMain.class.getName()
                            + "' is an abstract QuarkusApplication and cannot be instantiated"));

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked because @QuarkusMain on abstract class should fail at build time");
    }

}
