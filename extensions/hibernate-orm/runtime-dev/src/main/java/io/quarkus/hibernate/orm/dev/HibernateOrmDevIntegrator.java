package io.quarkus.hibernate.orm.dev;

import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitsHolder;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;

public class HibernateOrmDevIntegrator implements Integrator {
    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext, SessionFactoryImplementor sf) {
        String name = (String) sf.getProperties()
                .get(AvailableSettings.PERSISTENCE_UNIT_NAME);
        HibernateOrmDevController.get().pushPersistenceUnit(
                sf,
                getPersistenceUnitDescriptor(name, sf),
                name,
                metadata,
                sf.getServiceRegistry(),
                (String) sf.getProperties().get(JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE));
    }

    private static QuarkusPersistenceUnitDescriptor getPersistenceUnitDescriptor(String name, SessionFactoryImplementor sf) {
        // This is not great but avoids needing to depend on reactive
        boolean isReactive = sf.getClass().getPackage().getName().contains("reactive");
        return PersistenceUnitsHolder.getPersistenceUnitDescriptor(name, isReactive);
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactoryImplementor,
            SessionFactoryServiceRegistry sessionFactoryServiceRegistry) {
        HibernateOrmDevController.get().clearData();
    }
}
