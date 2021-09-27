package io.quarkus.maven.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.aesh.readline.Readline;
import org.aesh.readline.ReadlineBuilder;
import org.aesh.readline.tty.terminal.TerminalConnection;

/**
 * Prompt implementation.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Prompter {

    private static class Prompt {
        private final String prompt;
        private final String defaultValue;
        private final Consumer<String> inputConsumer;

        public Prompt(String prompt, String defaultValue, Consumer<String> inputConsumer) {
            this.prompt = prompt;
            this.defaultValue = defaultValue;
            this.inputConsumer = inputConsumer;
        }
    }

    private final List<Prompt> prompts = new ArrayList<>();

    public Prompter() throws IOException {
    }

    public Prompter addPrompt(String prompt, Consumer<String> inputConsumer) {
        prompts.add(new Prompt(prompt, null, inputConsumer));
        return this;
    }

    public Prompter addPrompt(String prompt, String defaultValue, Consumer<String> inputConsumer) {
        prompts.add(new Prompt(prompt, defaultValue, inputConsumer));
        return this;
    }

    public void collectInput() throws IOException {
        if (prompts.isEmpty()) {
            return;
        }
        final TerminalConnection connection = new TerminalConnection();
        try {
            read(connection, ReadlineBuilder.builder().enableHistory(false).build(), prompts.iterator());
            connection.openBlocking();
        } finally {
            connection.close();
        }
    }

    private static void read(TerminalConnection connection, Readline readline, Iterator<Prompt> prompts) {
        final Prompt prompt = prompts.next();
        readline.readline(connection, prompt.prompt, input -> {
            prompt.inputConsumer.accept(
                    (input == null || input.isBlank()) && prompt.defaultValue != null ? prompt.defaultValue : input);
            if (!prompts.hasNext()) {
                connection.close();
            } else {
                read(connection, readline, prompts);
            }
        });
    }
}
