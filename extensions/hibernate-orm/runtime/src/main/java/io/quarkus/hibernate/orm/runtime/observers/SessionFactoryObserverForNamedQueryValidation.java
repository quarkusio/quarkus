package io.quarkus.hibernate.orm.runtime.observers;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.QueryEngine;

//TODO replace with vanilla SessionFactoryObserverForNamedQueryValidation ?
//currently not possible as it was made package private;
//even better: decouple the query validation from the SessionFactory,
//so to allow validations at build time.
public class SessionFactoryObserverForNamedQueryValidation implements SessionFactoryObserver {
    private final Metadata metadata;

    public SessionFactoryObserverForNamedQueryValidation(MetadataImplementor metadata) {
        this.metadata = metadata;
    }

    @Override
    public void sessionFactoryCreated(SessionFactory factory) {
        SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) factory;
        final QueryEngine queryEngine = sessionFactory.getQueryEngine();
        queryEngine.getNamedObjectRepository().prepare(sessionFactory, metadata);
        if (sessionFactory.getSessionFactoryOptions().isNamedQueryStartupCheckingEnabled()) {
            queryEngine.validateNamedQueries();
        }
    }
}
