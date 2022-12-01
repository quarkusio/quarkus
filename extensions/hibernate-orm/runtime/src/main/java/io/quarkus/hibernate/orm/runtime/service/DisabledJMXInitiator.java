package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jmx.internal.DisabledJmxServiceImpl;
import org.hibernate.jmx.spi.JmxService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.jboss.logging.Logger;

/**
 * Replacement implementation of org.hibernate.jmx.internal.JmxServiceInitiator
 * MBeans are currently too troublesome in GraalVM, so enforce disabling its
 * usage.
 */
public final class DisabledJMXInitiator implements StandardServiceInitiator<JmxService> {

    public static final DisabledJMXInitiator INSTANCE = new DisabledJMXInitiator();

    @Override
    public Class<JmxService> getServiceInitiated() {
        return JmxService.class;
    }

    @Override
    public JmxService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        if (ConfigurationHelper.getBoolean(AvailableSettings.JMX_ENABLED, configurationValues, false)) {
            Logger.getLogger(DisabledJMXInitiator.class)
                    .warn("Enabling JMX is not allowed in Quarkus: forcefully disabled. Ignoring property:"
                            + AvailableSettings.JMX_ENABLED);
        }
        return DisabledJmxServiceImpl.INSTANCE;
    }

}
