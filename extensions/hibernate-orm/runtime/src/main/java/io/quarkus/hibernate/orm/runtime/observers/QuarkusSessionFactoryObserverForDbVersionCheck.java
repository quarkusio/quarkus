package io.quarkus.hibernate.orm.runtime.observers;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import io.quarkus.hibernate.orm.runtime.service.QuarkusRuntimeInitDialectFactory;

public class QuarkusSessionFactoryObserverForDbVersionCheck implements SessionFactoryObserver {
    @Override
    public void sessionFactoryCreated(SessionFactory factory) {
        var dialectFactory = (QuarkusRuntimeInitDialectFactory) ((SessionFactoryImplementor) factory)
                .getServiceRegistry().getService(DialectFactory.class);
        dialectFactory.checkActualDbVersion();
    }
}
