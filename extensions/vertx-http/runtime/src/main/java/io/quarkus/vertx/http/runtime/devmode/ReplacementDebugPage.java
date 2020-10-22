package io.quarkus.vertx.http.runtime.devmode;

import java.util.function.Function;

import io.quarkus.dev.ErrorPageGenerators;
import io.quarkus.runtime.TemplateHtmlBuilder;
import io.quarkus.runtime.util.ExceptionUtil;

/**
 * Generates an error page with a stack trace.
 */
public class ReplacementDebugPage {

    public static String generateHtml(final Throwable exception) {
        Throwable rootCause = ExceptionUtil.getRootCause(exception);
        if (rootCause == null) {
            rootCause = exception;
        }
        Function<Throwable, String> generator = ErrorPageGenerators.get(rootCause.getClass().getName());
        if (generator != null) {
            return generator.apply(rootCause);
        }
        // Default error page
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
