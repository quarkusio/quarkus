package io.quarkus.hibernate.orm.runtime.dev;

import static org.hibernate.cfg.AvailableSettings.HBM2DDL_IMPORT_FILES;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;

public class HibernateOrmDevIntegrator implements Integrator {
    private static final Map<String, QuarkusPersistenceUnitDescriptor> puDescriptorMap = new ConcurrentHashMap<>();

    public static void clearPuMap() {
        puDescriptorMap.clear();
    }

    public static void mapPersistenceUnit(String pu, QuarkusPersistenceUnitDescriptor descriptor) {
        puDescriptorMap.put(pu, descriptor);
    }

    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
            SessionFactoryImplementor sessionFactoryImplementor) {
        String name = (String) sessionFactoryImplementor.getProperties()
                .get(AvailableSettings.PERSISTENCE_UNIT_NAME);
        HibernateOrmDevController.get().pushPersistenceUnit(
                sessionFactoryImplementor,
                puDescriptorMap.get(name),
                name,
                metadata,
                sessionFactoryImplementor.getServiceRegistry(),
                (String) sessionFactoryImplementor.getProperties().get(HBM2DDL_IMPORT_FILES));
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactoryImplementor,
            SessionFactoryServiceRegistry sessionFactoryServiceRegistry) {
        HibernateOrmDevController.get().clearData();
    }
}
