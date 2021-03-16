package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.reactive.vertx.VertxInstance;
import org.hibernate.reactive.vertx.impl.ProvidedVertxInstance;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import io.vertx.core.Vertx;

public class VertxInstanceInitiator implements StandardServiceInitiator<VertxInstance> {

    private final Vertx vertx;

    public VertxInstanceInitiator(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public VertxInstance initiateService(Map map, ServiceRegistryImplementor serviceRegistryImplementor) {
        return new ProvidedVertxInstance(vertx);
    }

    @Override
    public Class<VertxInstance> getServiceInitiated() {
        return VertxInstance.class;
    }
}
