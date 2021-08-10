package io.quarkus.hibernate.orm.runtime.devconsole;

import static org.hibernate.cfg.AvailableSettings.HBM2DDL_IMPORT_FILES;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class HibernateOrmDevConsoleIntegrator implements Integrator {
    @Override
    public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactoryImplementor,
            SessionFactoryServiceRegistry sessionFactoryServiceRegistry) {
        HibernateOrmDevConsoleInfoSupplier.pushPersistenceUnit(
                (String) sessionFactoryImplementor.getProperties().get(AvailableSettings.PERSISTENCE_UNIT_NAME),
                metadata, sessionFactoryServiceRegistry,
                (String) sessionFactoryImplementor.getProperties().get(HBM2DDL_IMPORT_FILES));
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactoryImplementor,
            SessionFactoryServiceRegistry sessionFactoryServiceRegistry) {
        // Nothing to do
    }
}
