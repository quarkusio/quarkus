package io.quarkus.mutiny.runtime;

import java.util.concurrent.ExecutorService;

import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.mutiny.infrastructure.Infrastructure;

@Recorder
public class MutinyInfrastructure {

    public void configureMutinyInfrastructure(ExecutorService exec) {
        Infrastructure.setDefaultExecutor(exec);
    }
}
