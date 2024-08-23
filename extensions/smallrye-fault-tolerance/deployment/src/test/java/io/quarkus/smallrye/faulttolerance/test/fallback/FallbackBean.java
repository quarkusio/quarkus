package io.quarkus.smallrye.faulttolerance.test.fallback;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

@ApplicationScoped
public class FallbackBean {
    @Fallback(RecoverFallback.class)
    public String hello() {
        throw new RuntimeException();
    }

    public static class RecoverFallback implements FallbackHandler<String> {
        @Override
        public String handle(ExecutionContext context) {
            return RecoverFallback.class.getName();
        }
    }
}
