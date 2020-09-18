package io.quarkus.mutiny.runtime;

import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.mutiny.infrastructure.Infrastructure;

@Recorder
public class MutinyInfrastructure {

    public void configureMutinyInfrastructure(ExecutorService exec) {
        Infrastructure.setDefaultExecutor(exec);
    }

    public void configureDroppedExceptionHandler() {
        Logger logger = Logger.getLogger(MutinyInfrastructure.class);
        Infrastructure.setDroppedExceptionHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                logger.error("Mutiny had to drop the following exception", throwable);
            }
        });
    }

    public void configureThreadBlockingChecker() {
        Infrastructure.setCanCallerThreadBeBlockedSupplier(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                String threadName = Thread.currentThread().getName();
                return !threadName.contains("vertx-eventloop-thread-");
            }
        });
    }
}
