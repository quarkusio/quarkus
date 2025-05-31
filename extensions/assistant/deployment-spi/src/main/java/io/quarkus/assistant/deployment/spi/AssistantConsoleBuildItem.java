package io.quarkus.assistant.deployment.spi;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.assistant.runtime.dev.Assistant;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.console.ConsoleCommand;
import io.quarkus.deployment.dev.testing.MessageFormat;

/**
 * Add a menu item in the Assistant console menu
 *
 * Extensions can produce this to add a item under the Assistant heading in the console
 * This will only appear in the console if the assistant is available
 */
public final class AssistantConsoleBuildItem extends MultiBuildItem {
    private final ConsoleCommand consoleCommand;

    private final String description;
    private final char key;
    private final Optional<String> systemMessage;
    private final String userMessage;
    private final Supplier<String> colorSupplier;
    private final Supplier<String> stateSupplier;
    private final Map<String, String> variables;
    private final Optional<Function<Assistant, CompletionStage<?>>> function;

    public AssistantConsoleBuildItem(ConsoleCommand consoleCommand) {
        this.consoleCommand = consoleCommand;
        this.function = Optional.empty();
        this.description = consoleCommand.getDescription();
        this.key = consoleCommand.getKey();
        this.systemMessage = Optional.empty();
        this.userMessage = null;
        this.colorSupplier = consoleCommand.getHelpState().getColorSupplier();
        this.stateSupplier = consoleCommand.getHelpState().getStateSupplier();
        this.variables = Map.of();
    }

    private AssistantConsoleBuildItem(Builder builder) {
        this.description = builder.description;
        this.key = builder.key;
        this.systemMessage = builder.systemMessage;
        this.userMessage = builder.userMessage;
        this.colorSupplier = builder.colorSupplier;
        this.stateSupplier = builder.stateSupplier;
        this.variables = builder.variables;
        this.consoleCommand = null;
        this.function = builder.function;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String description;
        private char key = Character.MIN_VALUE;
        private Optional<String> systemMessage = Optional.empty();
        private String userMessage;
        private Supplier<String> colorSupplier = new Supplier<String>() {
            @Override
            public String get() {
                return MessageFormat.RESET;
            }
        };
        private Supplier<String> stateSupplier = null;
        private Map<String, String> variables = Map.of();
        private Optional<Function<Assistant, CompletionStage<?>>> function = Optional.empty();

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder key(char key) {
            this.key = key;
            return this;
        }

        public Builder systemMessage(String systemMessage) {
            this.systemMessage = Optional.of(systemMessage);
            return this;
        }

        public Builder userMessage(String userMessage) {
            this.userMessage = userMessage;
            return this;
        }

        public Builder colorSupplier(Supplier<String> colorSupplier) {
            this.colorSupplier = colorSupplier;
            return this;
        }

        public Builder stateSupplier(Supplier<String> stateSupplier) {
            this.stateSupplier = stateSupplier;
            return this;
        }

        public Builder variables(Map<String, String> variables) {
            this.variables = variables;
            return this;
        }

        public Builder function(Function<Assistant, CompletionStage<?>> function) {
            this.function = Optional.of(function);
            return this;
        }

        public AssistantConsoleBuildItem build() {
            if (key == Character.MIN_VALUE) {
                throw new IllegalStateException(
                        "You have to specify a key. This is the key the user will press to get to your function");
            }
            if (description == null || description.isBlank()) {
                throw new IllegalStateException(
                        "You have to specify a description. This is what the user will see in the console menu");
            }
            if (userMessage == null && !function.isPresent()) {
                throw new IllegalStateException(
                        "You have to specify userMessage that will be send to AI, or implement your own using the function");
            }

            return new AssistantConsoleBuildItem(this);
        }
    }

    public ConsoleCommand getConsoleCommand() {
        return consoleCommand;
    }

    public String getDescription() {
        return consoleCommand != null ? consoleCommand.getDescription() : description;
    }

    public char getKey() {
        return key;
    }

    public Optional<String> getSystemMessage() {
        return systemMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public Supplier<String> getColorSupplier() {
        return consoleCommand != null ? consoleCommand.getHelpState().getColorSupplier() : colorSupplier;
    }

    public Supplier<String> getStateSupplier() {
        return consoleCommand != null ? consoleCommand.getHelpState().getStateSupplier() : stateSupplier;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public Optional<Function<Assistant, CompletionStage<?>>> getFunction() {
        return function;
    }
}
