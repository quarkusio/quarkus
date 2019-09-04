package io.quarkus.smallrye.context.runtime;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.inject.Singleton;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.context.SmallRyeThreadContext;

@ApplicationScoped
public class SmallRyeContextPropagationProvider {

    private SmallRyeManagedExecutor managedExecutor;

    void initialize(ExecutorService executorService) {
        managedExecutor = new SmallRyeManagedExecutor(-1, -1, (SmallRyeThreadContext) getAllThreadContext(), executorService,
                "no-ip") {
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

    @Typed(ManagedExecutor.class)
    @Produces
    @Singleton
    public ManagedExecutor getAllManagedExecutor() {
        return managedExecutor;
    }

}
