package io.quarkus.tck.opentelemetry;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import io.quarkus.arc.Arc;

public class ExecutorProvider implements Executor {

    private final ExecutorService executorService;

    public ExecutorProvider() {
        this.executorService = Arc.container().getExecutorService();
    }

    @Override
    public void execute(Runnable command) {
        executorService.execute(command);
    }
}
