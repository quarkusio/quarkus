package io.quarkus.it.jpa.integrator;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class TestIntegrator implements Integrator {
    public static final AtomicInteger COUNTER = new AtomicInteger();

    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
            SessionFactoryImplementor sessionFactory) {
        COUNTER.incrementAndGet();
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
        COUNTER.decrementAndGet();
    }
}
