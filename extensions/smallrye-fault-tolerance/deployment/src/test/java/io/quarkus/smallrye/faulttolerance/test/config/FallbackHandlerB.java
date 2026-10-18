package io.quarkus.smallrye.faulttolerance.test.config;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

public class FallbackHandlerB implements FallbackHandler<String> {
    @Override
    public String handle(ExecutionContext context) {
        return "FallbackHandlerB";
    }
}
