package io.quarkus.devshell.deployment.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ANSI escape code utilities for terminal rendering.
 */
public final class AnsiRenderer {

    private static final Pattern ANSI_PATTERN = Pattern
            .compile("\033\\[(?:[0-9;]*[a-zA-Z]|\\][^\007]*\007|[()][A-Z0-9])");

    private AnsiRenderer() {
    }

    public static String stripAnsi(String text) {
        if (text == null) {
            return "";
        }
        return ANSI_PATTERN.matcher(text).replaceAll("");
    }

    public static final String BOX_HORIZONTAL = "─";
    public static final String BOX_VERTICAL = "│";
    public static final String ARROW_RIGHT = "▶";

    public static List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }

        String[] paragraphs = text.split("\n");
        for (String paragraph : paragraphs) {
            if (paragraph.length() <= maxWidth) {
                lines.add(paragraph);
            } else {
                String[] words = paragraph.split(" ");
                StringBuilder currentLine = new StringBuilder();

                for (String word : words) {
                    if (currentLine.length() + word.length() + 1 <= maxWidth) {
                        if (currentLine.length() > 0) {
                            currentLine.append(" ");
                        }
                        currentLine.append(word);
                    } else {
                        if (currentLine.length() > 0) {
                            lines.add(currentLine.toString());
                            currentLine = new StringBuilder();
                        }
                        if (word.length() > maxWidth) {
                            int start = 0;
                            while (start < word.length()) {
                                int end = Math.min(start + maxWidth, word.length());
                                lines.add(word.substring(start, end));
                                start = end;
                            }
                        } else {
                            currentLine.append(word);
                        }
                    }
                }

                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
            }
        }

        return lines;
    }

    public static String fixedWidth(String text, int width) {
        if (text == null) {
            text = "";
        }
        if (text.length() > width) {
            return text.substring(0, width - 1) + "…";
        }
        return String.format("%-" + width + "s", text);
    }
}
