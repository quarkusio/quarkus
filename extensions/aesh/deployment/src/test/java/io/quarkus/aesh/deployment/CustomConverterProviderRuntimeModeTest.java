package io.quarkus.aesh.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.converter.ConverterInvocationProvider;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.console.AeshContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.CliSettings;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that a custom {@link ConverterInvocationProvider} set via {@link CliSettings}
 * is applied in runtime mode ({@code DefaultAeshRuntimeRunnerFactory} path).
 * <p>
 * The custom provider wraps the converter invocation with a prefix so the
 * converter can prepend it to the converted value, proving the provider is active.
 */
public class CustomConverterProviderRuntimeModeTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    PrefixedCmd.class,
                    PrefixedConverterInvocation.class,
                    PrefixedConverterInvocationProvider.class,
                    PrefixingConverter.class,
                    PrefixedCliSettings.class))
            .setApplicationName("custom-converter-provider-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("--label=World");

    @Test
    void converterInvocationProviderApplied() {
        // The PrefixingConverter prepends the prefix from the custom invocation
        Assertions.assertThat(config.getStartupConsoleOutput())
                .containsOnlyOnce("Label: prefixed:World");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    /**
     * Custom {@link ConverterInvocation} that carries a prefix string.
     */
    public static class PrefixedConverterInvocation implements ConverterInvocation {

        private final ConverterInvocation delegate;
        private final String prefix;

        PrefixedConverterInvocation(ConverterInvocation delegate, String prefix) {
            this.delegate = delegate;
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getInput() {
            return delegate.getInput();
        }

        @Override
        public AeshContext getAeshContext() {
            return delegate.getAeshContext();
        }
    }

    public static class PrefixedConverterInvocationProvider implements ConverterInvocationProvider {

        @Override
        public ConverterInvocation enhanceConverterInvocation(ConverterInvocation converterInvocation) {
            return new PrefixedConverterInvocation(converterInvocation, "prefixed");
        }
    }

    /**
     * Converter that uses the custom invocation to prepend a prefix.
     */
    public static class PrefixingConverter implements Converter<String, PrefixedConverterInvocation> {

        @Override
        public String convert(PrefixedConverterInvocation invocation) throws OptionValidatorException {
            return invocation.getPrefix() + ":" + invocation.getInput();
        }
    }

    @ApplicationScoped
    public static class PrefixedCliSettings implements CliSettings {

        @Override
        @SuppressWarnings("unchecked")
        public void customize(SettingsBuilder<?> builder) {
            ((SettingsBuilder) builder).converterInvocationProvider(new PrefixedConverterInvocationProvider());
        }
    }

    /**
     * Single command with a converter that expects the custom invocation.
     */
    @CommandDefinition(name = "prefixed", description = "Test converter provider")
    public static class PrefixedCmd implements Command<CommandInvocation> {

        @Option(name = "label", converter = PrefixingConverter.class)
        String label;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Label: " + label);
            return CommandResult.SUCCESS;
        }
    }
}
