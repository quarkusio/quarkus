package io.quarkus.smallrye.faulttolerance.runtime;

import java.util.concurrent.ExecutorService;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.context.ManagedExecutor;

import io.smallrye.faulttolerance.AsyncExecutorProvider;

@Singleton
@Alternative
@Priority(1)
public class QuarkusAsyncExecutorProvider implements AsyncExecutorProvider {
    @Inject
    ManagedExecutor executor;

    @Override
    public ExecutorService get() {
        return executor;
    }
}
