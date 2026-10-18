package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.mutation.internal.StandardMutationExecutorService;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class QuarkusMutationExecutorServiceInitiator implements StandardServiceInitiator<MutationExecutorService> {
    /**
     * Singleton access
     */
    public static final QuarkusMutationExecutorServiceInitiator INSTANCE = new QuarkusMutationExecutorServiceInitiator();

    @Override
    public Class<MutationExecutorService> getServiceInitiated() {
        return MutationExecutorService.class;
    }

    @Override
    public MutationExecutorService initiateService(Map<String, Object> configurationValues,
            ServiceRegistryImplementor registry) {
        return new StandardMutationExecutorService(configurationValues);
    }

}
