package io.quarkus.virtual.threads;

import java.util.concurrent.ExecutorService;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

/**
 * Creates the {@link VirtualThreads @VirtualThreads} {@link ExecutorService} bean.
 */
public class VirtualThreadsExecutorCreator implements BeanCreator<ExecutorService> {

    @Override
    public ExecutorService create(SyntheticCreationalContext<ExecutorService> context) {
        return VirtualThreadsRecorder.VIRTUAL_THREADS_EXECUTOR_SUPPLIER.get();
    }
}
