package io.quarkus.hibernate.orm.runtime.dev;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class HibernateOrmDevIntegrator implements Integrator {
    @Override
    public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactoryImplementor,
            SessionFactoryServiceRegistry sessionFactoryServiceRegistry) {
        HibernateOrmDevController.get().pushPersistenceUnit(
                sessionFactoryImplementor,
                (String) sessionFactoryImplementor.getProperties()
                        .get(org.hibernate.cfg.AvailableSettings.PERSISTENCE_UNIT_NAME),
                metadata, sessionFactoryServiceRegistry,
                (String) sessionFactoryImplementor.getProperties().get(AvailableSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE));
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactoryImplementor,
            SessionFactoryServiceRegistry sessionFactoryServiceRegistry) {
        HibernateOrmDevController.get().clearData();
    }
}
