package io.quarkus.deployment.console;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConsoleCommand {

    private final char key;
    private final String description;
    private final String promptString;
    private final int promptPriority;
    private final Runnable runnable;
    private final Consumer<String> readLineHandler;
    private final HelpState helpState;

    public ConsoleCommand(char key, String description, String promptString, int promptPriority, HelpState helpState,
            Consumer<String> readLineHandler) {
        this.key = key;
        this.description = description;
        this.promptString = promptString;
        this.promptPriority = promptPriority;
        this.runnable = null;
        this.helpState = helpState;
        this.readLineHandler = readLineHandler;
    }

    public ConsoleCommand(char key, String description, String promptString, int promptPriority, HelpState helpState,
            Runnable runnable) {
        this.key = key;
        this.description = description;
        this.promptString = promptString;
        this.promptPriority = promptPriority;
        this.runnable = runnable;
        this.helpState = helpState;
        this.readLineHandler = null;
    }

    public ConsoleCommand(char key, String description, HelpState helpState, Runnable runnable) {
        this(key, description, null, -1, helpState, runnable);
    }

    public char getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public String getPromptString() {
        return promptString;
    }

    public int getPromptPriority() {
        return promptPriority;
    }

    public HelpState getHelpState() {
        return helpState;
    }

    public Consumer<String> getReadLineHandler() {
        return readLineHandler;
    }

    public static class HelpState {

        final Supplier<Boolean> toggleState;
        final Supplier<String> colorSupplier;
        final Supplier<String> stateSupplier;

        public HelpState(Supplier<Boolean> toggleState) {
            this.toggleState = toggleState;
            this.colorSupplier = null;
            this.stateSupplier = null;
        }

        public HelpState(Supplier<String> colorSupplier, Supplier<String> stateSupplier) {
            this.toggleState = null;
            this.colorSupplier = colorSupplier;
            this.stateSupplier = stateSupplier;
        }

        public Supplier<Boolean> getToggleState() {
            return toggleState;
        }

        public Supplier<String> getColorSupplier() {
            return colorSupplier;
        }

        public Supplier<String> getStateSupplier() {
            return stateSupplier;
        }
    }

}
