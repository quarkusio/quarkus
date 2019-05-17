package io.quarkus.runtime.execution;

import java.io.Serializable;

import org.jboss.logging.Logger;

/**
 * A handler that restarts the application whenever it receives a matching exit code.
 */
public final class RestartingExecutionHandler implements ExecutionHandler, Serializable {
    private static final long serialVersionUID = -1631634540697411528L;

    private static final Logger log = Logger.getLogger("io.quarkus.execution.restart");

    private final int exitCode;

    /**
     * Construct a new instance for a single exit code.
     *
     * @param exitCode the exit code to check for
     */
    public RestartingExecutionHandler(final int exitCode) {
        this.exitCode = exitCode;
    }

    public int run(final ExecutionChain chain, final ExecutionContext context) throws Exception {
        int result;
        result = chain.proceed(context);
        while (result == exitCode) {
            log.debug("Reloading Quarkus");
            result = chain.proceed(context);
        }
        return result;
    }
}
