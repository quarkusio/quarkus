package io.quarkus.runtime.execution;

import java.io.Serializable;

import io.quarkus.runtime.StartupContext;

/**
 * An execution handler using a classic generated setup method.
 */
public abstract class ClassicExecutionHandler implements ExecutionHandler, Serializable {

    private static final long serialVersionUID = -7982701424547869731L;

    public int run(final ExecutionChain chain, final ExecutionContext context) throws Exception {
        try (StartupContext startupContext = new StartupContext(context)) {
            deploy(startupContext);
            return chain.proceed(startupContext.addValuesTo(context));
        }
    }

    /**
     * Execute the classic-style generation step.
     *
     * @param startupContext the startup context (not {@code null})
     * @throws Exception if the setup step fails
     */
    public abstract void deploy(StartupContext startupContext) throws Exception;
}
