package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.reactive.context.Context;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Custom Quarkus initiator for the {@link Context} service; this one creates instances of {@link CheckingVertxContext}.
 */
public class CheckingVertxContextInitiator implements StandardServiceInitiator<Context> {

    public static final CheckingVertxContextInitiator INSTANCE = new CheckingVertxContextInitiator();

    @Override
    public Context initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new CheckingVertxContext();
    }

    @Override
    public Class<Context> getServiceInitiated() {
        return Context.class;
    }

}
