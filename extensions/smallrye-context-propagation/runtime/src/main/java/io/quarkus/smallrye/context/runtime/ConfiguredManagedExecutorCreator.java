package io.quarkus.smallrye.context.runtime;

import org.eclipse.microprofile.context.ManagedExecutor;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

public class ConfiguredManagedExecutorCreator implements BeanCreator<ManagedExecutor> {

    public static final String CLEARED = "cleared";
    public static final String PROPAGATED = "propagated";
    public static final String MAX_ASYNC = "maxAsync";
    public static final String MAX_QUEUED = "maxQueued";

    @Override
    public ManagedExecutor create(SyntheticCreationalContext<ManagedExecutor> context) {
        return ManagedExecutor.builder()
                .propagated((String[]) context.getParams().get(PROPAGATED))
                .cleared((String[]) context.getParams().get(CLEARED))
                .maxAsync((Integer) context.getParams().get(MAX_ASYNC))
                .maxQueued((Integer) context.getParams().get(MAX_QUEUED))
                .build();
    }
}
