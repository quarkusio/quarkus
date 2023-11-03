package io.quarkus.hibernate.orm.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class QuarkusJndiServiceInitiator implements StandardServiceInitiator<JndiService> {

    public static final QuarkusJndiServiceInitiator INSTANCE = new QuarkusJndiServiceInitiator();
    private static final QuarkusJndiService SERVICE_INSTANCE = new QuarkusJndiService();

    @Override
    public Class<JndiService> getServiceInitiated() {
        return JndiService.class;
    }

    @Override
    public JndiService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return SERVICE_INSTANCE;
    }
}
