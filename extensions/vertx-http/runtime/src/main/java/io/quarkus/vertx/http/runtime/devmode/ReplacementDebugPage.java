package io.quarkus.vertx.http.runtime.devmode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.dev.ErrorPageGenerators;
import io.quarkus.dev.config.ConfigurationProblem;
import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.runtime.TemplateHtmlBuilder;
import io.quarkus.runtime.util.ExceptionUtil;

/**
 * Generates an error page with a stack trace.
 */
public class ReplacementDebugPage {

    public static String generateHtml(final Throwable exception, String currentUri) {
        Throwable rootCause = ExceptionUtil.getRootCause(exception);
        if (rootCause == null) {
            rootCause = exception;
        }
        Function<Throwable, String> generator = ErrorPageGenerators.get(rootCause.getClass().getName());
        if (generator != null) {
            return generator.apply(rootCause);
        }
        // Default error page
        Set<String> configErrors = new HashSet<>();
        Set<Throwable> seen = new HashSet<>();
        Deque<Throwable> toProcess = new ArrayDeque<>();
        toProcess.add(exception);
        while (!toProcess.isEmpty()) {
            Throwable ex = toProcess.poll();
            if (seen.contains(ex)) {
                continue;
            }
            if (ex instanceof ConfigurationProblem) {
                configErrors.addAll(((ConfigurationProblem) ex).getConfigKeys());
            }
            seen.add(ex);
            if (ex.getCause() != null) {
                toProcess.add(ex.getCause());
            }
            toProcess.addAll(Arrays.asList(ex.getSuppressed()));
        }
        List<CurrentConfig> toEdit = new ArrayList<>();
        List<CurrentConfig> keys = CurrentConfig.CURRENT;
        for (CurrentConfig i : keys) {
            if (configErrors.contains(i.getPropertyName())) {
                toEdit.add(i);
                configErrors.remove(i.getPropertyName());
            }
        }
        for (String i : configErrors) {
            toEdit.add(new CurrentConfig(i, "", "", ConfigProvider.getConfig().getOptionalValue(i, String.class).orElse(null),
                    null));
        }

        TemplateHtmlBuilder builder = new TemplateHtmlBuilder("Error restarting Quarkus", exception.getClass().getName(),
                generateHeaderMessage(exception), currentUri, toEdit);
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
