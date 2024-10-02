package io.quarkus.smallrye.faulttolerance.test.retry.beforeretry;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.BeforeRetryHandler;

@Dependent
public class BeforeRetryHandlerService {
    static final Set<Integer> ids = ConcurrentHashMap.newKeySet();

    @Retry
    @BeforeRetry(MyBeforeRetryHandler.class)
    public void hello() {
        throw new IllegalArgumentException();
    }

    static class MyBeforeRetryHandler implements BeforeRetryHandler {
        @Inject
        MyDependency dep;

        @Override
        public void handle(ExecutionContext context) {
            ids.add(dep.id);
        }
    }
}
