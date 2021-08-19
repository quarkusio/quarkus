package io.quarkus.devtools.utils;

import java.io.IOException;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Prompt implementation.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Prompter {

    private final LineReader lineReader;

    public Prompter() throws IOException {
        // we set dumb to "true" only to prevent any warning when a proper terminal cannot be detected.
        // If a proper terminal is detected by JLine then that terminal will be used and setting dumb=true
        // won't force a dumb terminal.
        // (https://github.com/jline/jline3/issues/291)
        final Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        this.lineReader = LineReaderBuilder.builder().terminal(terminal).build();
    }

    public String prompt(final String message, final Character mask) throws IOException {
        Objects.requireNonNull(message);

        final String prompt = String.format("%s: ", message);
        String value;
        do {
            value = lineReader.readLine(prompt, mask);
        } while (StringUtils.isBlank(value));
        return value;
    }

    public String prompt(final String message) throws IOException {
        Objects.requireNonNull(message);
        return prompt(message, null);
    }

    public String promptWithDefaultValue(final String message, final String defaultValue) throws IOException {
        Objects.requireNonNull(message);
        Objects.requireNonNull(defaultValue);

        final String prompt = String.format("%s [%s]: ", message, defaultValue);
        String value = lineReader.readLine(prompt);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return value;
    }

}
