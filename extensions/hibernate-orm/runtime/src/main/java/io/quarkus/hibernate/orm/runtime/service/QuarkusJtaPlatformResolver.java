package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformResolver;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class QuarkusJtaPlatformResolver implements StandardServiceInitiator<JtaPlatformResolver> {

    private final JtaPlatform jtaPlatform;

    public QuarkusJtaPlatformResolver(JtaPlatform jtaPlatform) {
        this.jtaPlatform = jtaPlatform;
    }

    @Override
    public JtaPlatformResolver initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new JtaPlatformResolver() {
            @Override
            public JtaPlatform resolveJtaPlatform(Map configurationValues, ServiceRegistryImplementor registry) {
                return jtaPlatform;
            }
        };
    }

    @Override
    public Class<JtaPlatformResolver> getServiceInitiated() {
        return JtaPlatformResolver.class;
    }
}
