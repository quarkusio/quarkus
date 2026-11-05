package io.quarkus.runtime.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.jboss.logmanager.handlers.SyslogHandler;
import org.junit.jupiter.api.Test;

class SyslogAppNameTest {

    @Test
    void validNameIsUnchanged() {
        assertThat(LoggingSetup.toSyslogAppName("java")).isEqualTo("java");
        assertThat(LoggingSetup.toSyslogAppName("/usr/lib/jvm/java-21/bin/java"))
                .isEqualTo("/usr/lib/jvm/java-21/bin/java");
    }

    @Test
    void windowsExecutablePathIsReducedToBaseName() {
        assertThat(LoggingSetup.toSyslogAppName("C:\\Program Files\\Microsoft\\jdk-21.0.5.11-hotspot\\bin\\java.exe"))
                .isEqualTo("java.exe");
    }

    @Test
    void remainingInvalidCharactersAreReplaced() {
        assertThat(LoggingSetup.toSyslogAppName("/opt/my app/bin/my app")).isEqualTo("my_app");
        assertThat(LoggingSetup.toSyslogAppName("C:\\apps\\café.exe")).isEqualTo("caf_.exe");
    }

    @Test
    void emptyOrMissingNameFallsBackToJava() {
        assertThat(LoggingSetup.toSyslogAppName(null)).isEqualTo("java");
        assertThat(LoggingSetup.toSyslogAppName("")).isEqualTo("java");
        assertThat(LoggingSetup.toSyslogAppName("C:\\Program Files\\")).isEqualTo("java");
    }

    @Test
    void resultIsAcceptedBySyslogHandler() throws Exception {
        SyslogHandler handler = new SyslogHandler("localhost", 514);
        try {
            assertThatCode(() -> handler.setAppName(
                    LoggingSetup.toSyslogAppName("C:\\Program Files\\Java\\jdk 21\\bin\\java.exe")))
                    .doesNotThrowAnyException();
        } finally {
            handler.close();
        }
    }
}
