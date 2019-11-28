package io.quarkus.vertx.http.runtime.devmode;

import io.quarkus.runtime.TemplateHtmlBuilder;

/**
 * Generates an error page with a stack trace.
 */
public class ReplacementDebugPage {

    public static String generateHtml(final Throwable exception) {
        TemplateHtmlBuilder builder = new TemplateHtmlBuilder("Error restarting Quarkus", exception.getClass().getName(),
                generateHeaderMessage(exception));

        builder.stack(exception);

        return builder.toString();
    }

    private static String generateHeaderMessage(final Throwable exception) {
        return String.format("%s: %s", exception.getClass().getName(), extractFirstLine(exception.getMessage()));
    }

    private static String extractFirstLine(final String message) {
        if (null == message) {
            return "";
        }

        String[] lines = message.split("\\r?\\n");
        return lines[0].trim();
    }
}
