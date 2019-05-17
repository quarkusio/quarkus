package io.quarkus.runtime.execution;

/**
 * An application execution handler.
 */
@FunctionalInterface
public interface ExecutionHandler {

    /**
     * Run the application, returning an exit code when complete. The given {@code context} can be used
     * to pass control to the next handler, or the handler may perform a function and then immediately return.
     *
     * @param chain the execution chain (not {@code null})
     * @param context the application execution context (not {@code null})
     * @return the exit code to return
     * @throws Exception on startup failure
     */
    int run(ExecutionChain chain, ExecutionContext context) throws Exception;
}
