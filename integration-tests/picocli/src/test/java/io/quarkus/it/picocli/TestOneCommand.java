package io.quarkus.it.picocli;

import static io.quarkus.it.picocli.TestUtils.createConfig;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import picocli.CommandLine;

public class TestOneCommand {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("hello-app", HelloCommand.class, EventListener.class)
            .setCommandLineParameters("--name=Tester");

    @Test
    public void simpleTest() {
        Assertions.assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("ParseResult Event: Tester");
        Assertions.assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Hello Tester!");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @Dependent
    static class EventListener {
        void onParseEvent(@Observes CommandLine.ParseResult result) {
            System.out.println("ParseResult Event: " + result.matchedOption("name").getValue());
        }
    }

}
