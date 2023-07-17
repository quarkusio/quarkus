package io.quarkus.hibernate.reactive.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * This was the only way I found to get our hands on the settings used during metadata building.
 * Feel free to use some other solution if you find one.
 */
public class SettingsSpyingIdentifierGenerator implements IdentifierGenerator {
    public static final List<Map<String, Object>> collectedSettings = new ArrayList<>();

    @Override
    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
        collectedSettings.add(new HashMap<>(serviceRegistry.getService(ConfigurationService.class).getSettings()));
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        throw new IllegalStateException("This should not be called");
    }
}
