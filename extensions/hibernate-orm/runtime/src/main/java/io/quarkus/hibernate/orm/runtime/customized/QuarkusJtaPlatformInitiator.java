package io.quarkus.hibernate.orm.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class QuarkusJtaPlatformInitiator implements StandardServiceInitiator<JtaPlatform> {

    public static final StandardServiceInitiator<JtaPlatform> INSTANCE = new QuarkusJtaPlatformInitiator();

    private QuarkusJtaPlatformInitiator() {
    }

    @Override
    public JtaPlatform initiateService(Map map, ServiceRegistryImplementor serviceRegistryImplementor) {
        return QuarkusJtaPlatform.INSTANCE;
    }

    @Override
    public Class<JtaPlatform> getServiceInitiated() {
        return JtaPlatform.class;
    }

}
