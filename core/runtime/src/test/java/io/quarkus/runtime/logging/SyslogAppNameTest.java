package io.quarkus.runtime.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.jboss.logmanager.handlers.SyslogHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SyslogAppNameTest {

    @ParameterizedTest
    @CsvSource({
            "java, java",
            "/usr/lib/jvm/java-21/bin/java, java",
            "C:\\Program Files\\Microsoft\\jdk-21.0.10.7-hotspot\\bin\\java.exe, java.exe",
            "my app, my-app",
            "café, caf-",
            "C:\\, -",
            "'', -"
    })
    void producesValidAppName(String processName, String expectedAppName) {
        String appName = LoggingSetup.sanitizeSyslogAppName(processName);
        assertThat(appName).isEqualTo(expectedAppName);
        // must be accepted by the SyslogHandler, which requires printable ASCII without spaces
        assertThatCode(() -> new SyslogHandler("localhost", 514).setAppName(appName))
                .doesNotThrowAnyException();
    }

    @Test
    void currentProcessNameProducesValidAppName() {
        String appName = LoggingSetup.sanitizeSyslogAppName(io.smallrye.common.os.Process.getProcessName());
        assertThatCode(() -> new SyslogHandler("localhost", 514).setAppName(appName))
                .doesNotThrowAnyException();
    }
}
