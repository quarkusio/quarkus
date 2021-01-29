package io.quarkus.grpc.runtime;

import io.grpc.Context;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import java.util.Map;

public class GrpcContextProvider implements ThreadContextProvider {

    private static final String GRPC_CONTEXT = "GRPC";

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        final Context currentContext = Context.current();
        if (currentContext == null) {
            return null;
        }
        return () -> {
            // store the context before attaching the saved context
            final Context previousContext = Context.current();

            // attach the saved context
            currentContext.attach();
            return () -> {
                // restore the previous context
                currentContext.detach(previousContext);
            };
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        // nothing to do here
        return () -> () -> {
        };
    }

    @Override
    public String getThreadContextType() {
        return GRPC_CONTEXT;
    }
}
