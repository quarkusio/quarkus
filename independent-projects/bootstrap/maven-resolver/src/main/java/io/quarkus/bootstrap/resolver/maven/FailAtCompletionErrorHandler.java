package io.quarkus.bootstrap.resolver.maven;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.jboss.logging.Logger;

/**
 * Error handler that collects all the task execution errors and throws a single one reporting all the captured
 * failures once all the tasks have finished.
 */
public class FailAtCompletionErrorHandler implements ModelResolutionTaskErrorHandler {

    private static final Logger log = Logger.getLogger(FailAtCompletionErrorHandler.class);

    /**
     * Errors caught while running tasks
     */
    private final Collection<Exception> errors = new ConcurrentLinkedDeque<>();

    @Override
    public void handleError(ModelResolutionTask task, Exception error) {
        errors.add(error);
    }

    @Override
    public void allTasksFinished() {
        if (!errors.isEmpty()) {
            throwFailureReport(errors);
        }
    }

    protected boolean isEmpty() {
        return errors.isEmpty();
    }

    private static void throwFailureReport(Collection<Exception> errors) {
        var sb = new StringBuilder(
                "The following errors were encountered while processing Quarkus application dependencies:");
        log.error(sb);
        var i = 1;
        for (var error : errors) {
            var prefix = i++ + ")";
            log.error(prefix, error);
            sb.append(System.lineSeparator()).append(prefix).append(" ").append(error.getLocalizedMessage());
            for (var e : error.getStackTrace()) {
                sb.append(System.lineSeparator());
                for (int j = 0; j < prefix.length(); ++j) {
                    sb.append(" ");
                }
                sb.append("at ").append(e);
                if (e.getClassName().contains("io.quarkus")) {
                    sb.append(System.lineSeparator());
                    for (int j = 0; j < prefix.length(); ++j) {
                        sb.append(" ");
                    }
                    sb.append("...");
                    break;
                }
            }
        }
        throw new RuntimeException(sb.toString());
    }
}
