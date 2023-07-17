package io.quarkus.it.picocli;

import static io.quarkus.it.picocli.TestUtils.createConfig;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import picocli.CommandLine;

public class TestParsedCommand {
    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("hello-app", ParsedCommand.class,
            ConfigFromParseResult.class, ConfigProducer.class)
            .setCommandLineParameters("-p", "FromConfig");

    @Test
    public void simpleTest() {
        Assertions.assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Set value: FromConfig");
        Assertions.assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Parsed value: FromConfig");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @ApplicationScoped
    static class ConfigProducer {

        @Produces
        @ApplicationScoped
        ConfigFromParseResult configFromParseResult(CommandLine.ParseResult parseResult) {
            return new ConfigFromParseResult(parseResult.matchedOption("p").getValue());
        }

    }
}
