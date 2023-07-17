package io.quarkus.vertx.http.runtime.logstream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

class LogControllerTest {

    @Test
    void updateLogLevelShouldUseParentWhenLevelIsNull() {
        Logger logger = Logger.getLogger("foo.bar");
        logger.setLevel(Level.CONFIG);
        Logger parent = Logger.getLogger("foo");
        LogController.updateLogLevel("foo.bar", null);
        assertThat(logger.getLevel()).isEqualTo(parent.getLevel());
    }

    @Test
    void updateLogLevelShouldThrowIAEinRootLoggerWhenLevelIsNull() {
        assertThatIllegalArgumentException().isThrownBy(() -> LogController.updateLogLevel("", null))
                .withMessage("The level of the root logger cannot be set to null");
    }

}
