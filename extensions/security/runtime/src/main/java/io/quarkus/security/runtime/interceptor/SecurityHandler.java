package io.quarkus.security.runtime.interceptor;

import static io.quarkus.security.spi.runtime.SecurityHandlerConstants.EXECUTED;
import static io.quarkus.security.spi.runtime.SecurityHandlerConstants.SECURITY_HANDLER;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.InvocationContext;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Singleton
public class SecurityHandler {

    @Inject
    SecurityConstrainer constrainer;

    public Object handle(InvocationContext ic) throws Exception {
        if (alreadyHandled(ic)) {
            return ic.proceed();
        }
        Class<?> returnType = ic.getMethod().getReturnType();
        if (Uni.class.isAssignableFrom(returnType)) {
            return constrainer.nonBlockingCheck(ic.getMethod(), ic.getParameters())
                    .onItem().transformToUni(new UniContinuation(ic));
        } else if (CompletionStage.class.isAssignableFrom(returnType)) {
            return constrainer.nonBlockingCheck(ic.getMethod(), ic.getParameters())
                    .onItem().transformToUni((s) -> {
                        try {
                            return Uni.createFrom().completionStage((CompletionStage<?>) ic.proceed());
                        } catch (Exception e) {
                            return Uni.createFrom().failure(e);
                        }
                    }).subscribeAsCompletionStage();
        } else if (Multi.class.isAssignableFrom(returnType)) {
            return constrainer.nonBlockingCheck(ic.getMethod(), ic.getParameters())
                    .onItem().transformToMulti(new MultiContinuation(ic));
        } else {
            constrainer.check(ic.getMethod(), ic.getParameters());
            return ic.proceed();
        }
    }

    private boolean alreadyHandled(InvocationContext ic) {
        return ic.getContextData().put(SECURITY_HANDLER, EXECUTED) != null;
    }

    private static class UniContinuation implements Function<Object, Uni<?>> {
        private final InvocationContext ic;

        UniContinuation(InvocationContext invocationContext) {
            ic = invocationContext;
        }

        @Override
        public Uni<?> apply(Object o) {
            try {
                return (Uni<?>) ic.proceed();
            } catch (Exception e) {
                return Uni.createFrom().failure(e);
            }
        }
    }

    private static class MultiContinuation implements Function<Object, Multi<?>> {
        private final InvocationContext ic;

        public MultiContinuation(InvocationContext invocationContext) {
            ic = invocationContext;
        }

        @Override
        public Multi<?> apply(Object o) {
            try {
                return (Multi<?>) ic.proceed();
            } catch (Exception e) {
                return Multi.createFrom().failure(e);
            }
        }
    }
}
