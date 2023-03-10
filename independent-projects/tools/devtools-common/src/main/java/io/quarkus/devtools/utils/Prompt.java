package io.quarkus.devtools.utils;

import java.io.Console;
import java.util.Optional;

public final class Prompt {

    private Prompt() {
        //Utility
    }

    /**
     * Utility to prompt users for a yes/no answer.
     * The utility will prompt user in a loop until it gets a yes/no or blank response.
     *
     * @param defaultValue The value to return if user provides a blank response.
     * @param prompt The text to display
     * @param args Formatting args for the prompt
     * @return true if user replied with `y` or `yes`, false if user provided `n` or `no`, defaultValue if user provided empty
     *         response.
     */
    public static boolean yesOrNo(boolean defaultValue, String prompt, String... args) {
        String choices = defaultValue ? " (Y/n)" : " (y/N)";
        String optionalQuestionMark = prompt.matches(".*\\?\\s*$") ? " " : " ? ";
        while (true) {
            try {
                Optional<Console> console = Optional.ofNullable(System.console());
                String response = console
                        .map(c -> c.readLine(prompt + choices + optionalQuestionMark, args).trim().toLowerCase())
                        .orElse(defaultValue ? "y" : "n");
                if (response.isBlank()) {
                    return defaultValue;
                }
                if (response.equals("y") || response.equals("yes")) {
                    return true;
                }
                if (response.equals("n") || response.equals("no")) {
                    return false;
                }
            } catch (Exception ignore) {
                return defaultValue;
            }
        }
    }
}
