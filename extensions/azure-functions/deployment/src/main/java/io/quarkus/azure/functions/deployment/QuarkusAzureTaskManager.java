package io.quarkus.azure.functions.deployment;

import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class QuarkusAzureTaskManager extends AzureTaskManager {
    @Override
    protected void doRead(Runnable runnable, AzureTask<?> task) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    protected void doWrite(Runnable runnable, AzureTask<?> task) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    protected void doRunLater(Runnable runnable, AzureTask<?> task) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    protected void doRunOnPooledThread(Runnable runnable, AzureTask<?> task) {
        Mono.fromRunnable(runnable).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    protected void doRunAndWait(Runnable runnable, AzureTask<?> task) {
        runnable.run();
    }

    @Override
    protected void doRunInBackground(Runnable runnable, AzureTask<?> task) {
        doRunOnPooledThread(runnable, task);
    }

    @Override
    protected void doRunInModal(Runnable runnable, AzureTask<?> task) {
        throw new UnsupportedOperationException("not support");
    }
}
