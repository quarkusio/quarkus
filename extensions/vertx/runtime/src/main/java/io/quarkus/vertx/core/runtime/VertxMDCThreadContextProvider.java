package io.quarkus.vertx.core.runtime;

import java.util.Collections;
import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.vertx.core.Vertx;

/**
 * Propagates the logging MDC through MicroProfile Context Propagation onto threads that have no Vert.x context, such
 * as the threads of an executor created by the application.
 * <p>
 * On a thread with a Vert.x context the MDC lives in the duplicated context, which is already carried by Quarkus (the
 * Quarkus executor and Mutiny run each task on a duplicated context that holds a copy of the MDC). The snapshot
 * leaves such threads alone: the duplicated context may be shared with other work in flight, so replacing its MDC
 * from a snapshot captured elsewhere would corrupt it.
 */
public class VertxMDCThreadContextProvider implements ThreadContextProvider {

    public static final String MDC = "MDC";

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        Map<String, Object> captured = VertxMDC.INSTANCE.copyObject();
        return new ThreadContextSnapshot() {
            @Override
            public ThreadContextController begin() {
                return replaceMdc(captured);
            }
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return new ThreadContextSnapshot() {
            @Override
            public ThreadContextController begin() {
                return replaceMdc(Collections.emptyMap());
            }
        };
    }

    @Override
    public String getThreadContextType() {
        return MDC;
    }

    private static ThreadContextController replaceMdc(Map<String, Object> mdc) {
        if (Vertx.currentContext() != null) {
            return NoopController.INSTANCE;
        }
        Map<String, Object> previous = VertxMDC.INSTANCE.copyObject();
        if (previous.isEmpty() && mdc.isEmpty()) {
            return NoopController.INSTANCE;
        }
        applyMdc(mdc);
        return new ThreadContextController() {
            @Override
            public void endContext() throws IllegalStateException {
                applyMdc(previous);
            }
        };
    }

    private static void applyMdc(Map<String, Object> mdc) {
        VertxMDC.INSTANCE.clear();
        for (Map.Entry<String, Object> entry : mdc.entrySet()) {
            VertxMDC.INSTANCE.putObject(entry.getKey(), entry.getValue());
        }
    }

    private static final class NoopController implements ThreadContextController {

        static final NoopController INSTANCE = new NoopController();

        @Override
        public void endContext() throws IllegalStateException {
        }
    }
}
