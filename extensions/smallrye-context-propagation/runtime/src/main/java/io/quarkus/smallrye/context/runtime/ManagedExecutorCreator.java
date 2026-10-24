package io.quarkus.smallrye.context.runtime;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.eclipse.microprofile.context.ThreadContext;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.ExecutorRecorder;
import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.context.SmallRyeThreadContext;

public class ManagedExecutorCreator implements BeanCreator<SmallRyeManagedExecutor> {

    @Override
    public SmallRyeManagedExecutor create(SyntheticCreationalContext<SmallRyeManagedExecutor> context) {
        ThreadContext threadContext = context.getInjectedReference(ThreadContext.class);
        ExecutorService executorService = (ExecutorService) ExecutorRecorder.getCurrent();
        return new SmallRyeManagedExecutor(-1, -1, (SmallRyeThreadContext) threadContext, executorService, "no-ip") {
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
}
