package io.quarkus.deployment.devmode;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.quarkus.runtime.TemplateHtmlBuilder;

/**
 * Generates an error page with a stack trace.
 */
public class ReplacementDebugPage {

    public static String generateHtml(final Throwable exception) {
        TemplateHtmlBuilder builder = new TemplateHtmlBuilder("Error restarting Quarkus", exception.getClass().getName(),
                generateHeaderMessage(exception));

        builder.stack(generateStackTrace(exception));

        return builder.toString();
    }

    private static String generateStackTrace(final Throwable exception) {
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(stringWriter);
        // if we have a nested cause, we go all the way to the root cause
        // and print it first, before printing the whole stacktrace
        if (exception.getCause() != null) {
            Throwable rootCause = exception;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            pw.println("--- (root cause shown first) ---");
            rootCause.printStackTrace(pw);
            pw.println();
            pw.println("--- (complete stacktrace follows) ---");
        }
        exception.printStackTrace(pw);

        return stringWriter.toString().trim();
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
