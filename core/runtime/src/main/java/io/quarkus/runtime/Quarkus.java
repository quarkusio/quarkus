package io.quarkus.runtime;

import static io.quarkus.runtime.serial.Deserializer.deserializeResource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import io.quarkus.runtime.execution.Execution;
import io.quarkus.runtime.execution.ExecutionChain;
import io.quarkus.runtime.execution.ExecutionContext;
import io.quarkus.runtime.execution.RestartingExecutionHandler;
import io.quarkus.runtime.execution.ShutdownHookExecutionHandler;
import io.quarkus.runtime.execution.SignalHandlerExecutionHandler;

/**
 * The main entry point class of Quarkus.
 */
public class Quarkus {

    static {
        Timing.staticInitStarted();
        try {
            final Class<?> init = Class.forName("io.quarkus.runtime.generated.Init", true, Quarkus.class.getClassLoader());
            INITIAL_CONTEXT = (ExecutionContext) init.getDeclaredMethod("getInitialContext").invoke(null);
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            try {
                throw e.getCause();
            } catch (Error | RuntimeException e2) {
                throw e2;
            } catch (Throwable throwable) {
                throw new UndeclaredThrowableException(throwable);
            }
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
        STARTUP_CHAIN = deserializeResource(ExecutionChain.class, "META-INF/quarkus.init");
        Timing.staticInitStopped();
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
