package io.quarkus.runtime;

import io.quarkus.runtime.execution.Execution;
import io.quarkus.runtime.execution.ExecutionChain;
import io.quarkus.runtime.execution.ExecutionContext;
import io.quarkus.runtime.execution.RestartingExecutionHandler;
import io.quarkus.runtime.execution.ShutdownHookExecutionHandler;
import io.quarkus.runtime.execution.SignalHandlerExecutionHandler;
import io.quarkus.runtime.generated.Init;

/**
 * The main entry point class of Quarkus.
 */
public class Quarkus {

    static {
        Timing.markStaticInitStart();
        INITIAL_CONTEXT = Init.getInitialContext();
        STARTUP_CHAIN = Init.getInitialChain();
    }

    private static final ExecutionContext INITIAL_CONTEXT;
    private static final ExecutionChain STARTUP_CHAIN;

    /**
     * Run the application and exit when it completes.
     *
     * @param args the program arguments
     */
    public static void main(String... args) {
        ExecutionChain chain = STARTUP_CHAIN;
        chain = new ExecutionChain(chain, new RestartingExecutionHandler(Execution.EXIT_RELOAD_FULL));
        chain = new ExecutionChain(chain, SignalHandlerExecutionHandler.builder().build());
        chain = new ExecutionChain(chain, ShutdownHookExecutionHandler.INSTANCE);
        chain.startAndExit(INITIAL_CONTEXT.withArguments(args));
    }
}
