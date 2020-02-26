package io.quarkus.hibernate.orm.runtime.customized;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jndi.internal.JndiServiceInitiator;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class QuarkusJndiServiceInitiator implements StandardServiceInitiator<JndiService> {

    public static final JndiServiceInitiator INSTANCE = new JndiServiceInitiator();

    @Override
    public Class<JndiService> getServiceInitiated() {
        return JndiService.class;
    }

    @Override
    public JndiService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        throw new HibernateException("JNDI is not available in Quarkus - please setup integrations in some different way");
    }
}
