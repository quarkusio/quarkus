package io.quarkus.bootstrap.resolver.maven;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.aether.collection.DependencyCollectionException;
import org.jboss.logging.Logger;

/**
 * This error handler will collect errors thrown by the Maven resolver trying to acquire read or write locks
 * in the local Maven repository and will re-try the corresponding tasks with a blocking task runner.
 *
 * In case other errors were caught, the re-try won't happen and a single error report will be logged and thrown
 * as a {@link RuntimeException} to the caller.
 */
public class RetryLockAcquisitionErrorHandler extends FailAtCompletionErrorHandler {

    private static final Logger log = Logger.getLogger(RetryLockAcquisitionErrorHandler.class);

    private final ConcurrentLinkedDeque<ModelResolutionTask> failedTasks = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean skipRetry = new AtomicBoolean();

    @Override
    public void handleError(ModelResolutionTask task, Exception error) {
        // collect the exception
        super.handleError(task, error);
        // check if it's a failure to acquire a lock
        if (isRetriableError(error)) {
            failedTasks.add(task);
        } else {
            // if it's a different error, we do not re-try
            skipRetry.set(true);
        }
    }

    @Override
    public void allTasksFinished() {
        if (isEmpty()) {
            return;
        }
        if (skipRetry.get()) {
            // this will throw an exception
            super.allTasksFinished();
        }
        final ModelResolutionTaskRunner blockingRunner = ModelResolutionTaskRunner.getBlockingTaskRunner();
        log.warn("Re-trying dependency resolution tasks previously failed to acquire locks in the local Maven repository");
        for (ModelResolutionTask task : failedTasks) {
            blockingRunner.run(task);
        }
    }

    private boolean isRetriableError(Exception error) {
        return isCouldNotAcquireLockError(error) || isMissingFileError(error);
    }

    private static boolean isCouldNotAcquireLockError(Exception error) {
        // it's either "Could not acquire read lock" or "Could not acquire write lock"
        return error.getLocalizedMessage().contains("Could not acquire ");
    }

    /**
     * It could happen, especially in Maven 3.8, that multiple threads could end up writing/reading
     * the same temporary files while resolving the same artifact. Once one of the threads completes
     * resolving the artifact, the temporary file will be renamed to the target artifact file
     * and the other thread will fail with one of the file-not-found exceptions.
     * In this case, we simply re-try the collect request, which should now pick up the already resolved artifact.
     * <p>
     * Checks whether the cause of this exception a kind of no-such-file exception.
     * This error should not be seen with later versions of Maven 3.9.
     *
     * @param error top level exception
     * @return whether cause is a missing file
     */
    private static boolean isMissingFileError(Exception error) {
        if (!(error instanceof DeploymentInjectionException)) {
            return false;
        }
        Throwable t = error.getCause();
        if (!(t instanceof DependencyCollectionException)) {
            return false;
        }
        while (t != null) {
            var cause = t.getCause();
            // It looks like in Maven 3.9 it's NoSuchFileException, while in Maven 3.8 it's FileNotFoundException
            if (cause instanceof NoSuchFileException e) {
                return true;
            } else if (cause instanceof FileNotFoundException) {
                return true;
            }
            t = cause;
        }
        return false;
    }
}
