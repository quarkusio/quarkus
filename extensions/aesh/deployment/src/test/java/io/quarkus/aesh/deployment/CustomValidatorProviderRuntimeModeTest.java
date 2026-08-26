package io.quarkus.aesh.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.OptionValidator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.console.AeshContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.CliSettings;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that a custom {@link ValidatorInvocationProvider} set via {@link CliSettings}
 * is applied in runtime mode ({@code DefaultAeshRuntimeRunnerFactory} path).
 * <p>
 * The custom provider wraps the validator invocation with a minimum length
 * threshold so the validator can enforce it, proving the provider is active.
 */
public class CustomValidatorProviderRuntimeModeTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    ValidatedCmd.class,
                    MinLengthValidatorInvocation.class,
                    MinLengthValidatorInvocationProvider.class,
                    MinLengthValidator.class,
                    ValidatorCliSettings.class))
            .setApplicationName("custom-validator-provider-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("--name=ValidName");

    @Test
    void validatorInvocationProviderAppliedWithValidInput() {
        // Valid input (length >= 3) should succeed
        Assertions.assertThat(config.getStartupConsoleOutput())
                .containsOnlyOnce("Hello ValidName!");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    /**
     * Custom {@link ValidatorInvocation} that carries a minimum length.
     */
    public static class MinLengthValidatorInvocation implements ValidatorInvocation<String, Object> {

        private final ValidatorInvocation<String, Object> delegate;
        private final int minLength;

        @SuppressWarnings("unchecked")
        MinLengthValidatorInvocation(ValidatorInvocation<?, ?> delegate, int minLength) {
            this.delegate = (ValidatorInvocation<String, Object>) delegate;
            this.minLength = minLength;
        }

        public int getMinLength() {
            return minLength;
        }

        @Override
        public String getValue() {
            return delegate.getValue();
        }

        @Override
        public Object getCommand() {
            return delegate.getCommand();
        }

        @Override
        public AeshContext getAeshContext() {
            return delegate.getAeshContext();
        }
    }

    public static class MinLengthValidatorInvocationProvider implements ValidatorInvocationProvider {

        @Override
        public ValidatorInvocation enhanceValidatorInvocation(ValidatorInvocation validatorInvocation) {
            return new MinLengthValidatorInvocation(validatorInvocation, 3);
        }
    }

    /**
     * Validator that uses the custom invocation to enforce a minimum length.
     */
    public static class MinLengthValidator implements OptionValidator<MinLengthValidatorInvocation> {

        @Override
        public void validate(MinLengthValidatorInvocation invocation) throws OptionValidatorException {
            String value = invocation.getValue();
            if (value != null && value.length() < invocation.getMinLength()) {
                throw new OptionValidatorException(
                        "Value '" + value + "' is too short (min " + invocation.getMinLength() + " chars)");
            }
        }
    }

    @ApplicationScoped
    public static class ValidatorCliSettings implements CliSettings {

        @Override
        @SuppressWarnings("unchecked")
        public void customize(SettingsBuilder<?> builder) {
            ((SettingsBuilder) builder).validatorInvocationProvider(new MinLengthValidatorInvocationProvider());
        }
    }

    /**
     * Single command with a validated option that expects the custom invocation.
     */
    @CommandDefinition(name = "validated", description = "Test validator provider")
    public static class ValidatedCmd implements Command<CommandInvocation> {

        @Option(name = "name", defaultValue = "World", validator = MinLengthValidator.class)
        String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello " + name + "!");
            return CommandResult.SUCCESS;
        }
    }
}
