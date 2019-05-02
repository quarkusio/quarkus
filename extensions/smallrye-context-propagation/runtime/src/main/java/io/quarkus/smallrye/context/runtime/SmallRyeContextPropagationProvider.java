package io.quarkus.smallrye.context.runtime;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

import io.smallrye.context.impl.ManagedExecutorImpl;
import io.smallrye.context.impl.ThreadContextImpl;

@ApplicationScoped
public class SmallRyeContextPropagationProvider {

    private volatile ManagedExecutorImpl managedExecutor;

    void initialize(ExecutorService executorService) {
        managedExecutor = new ManagedExecutorImpl(-1, -1, (ThreadContextImpl) getAllThreadContext(), executorService, "no-ip") {
            @Override
            public void shutdown() {
                throw new IllegalStateException("This executor is managed by the container and cannot be shut down.");
            }

            @Override
            public List<Runnable> shutdownNow() {
                throw new IllegalStateException("This executor is managed by the container and cannot be shut down.");
            }
        };
    }

    @Produces
    @Singleton
    public ThreadContext getAllThreadContext() {
        return ThreadContext.builder().propagated(ThreadContext.ALL_REMAINING).cleared().unchanged().build();
    }

    @Produces
    @Singleton
    public ManagedExecutor getAllManagedExecutor() {
        return managedExecutor;
    }

}
