package io.quarkus.hibernate.orm.runtime.observers;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;

/**
 * Executes the data init script once the session factory is created,
 * for schema management strategies that do not create the schema
 * (and thus never execute the script as part of schema management).
 * <p>
 * Registered right after {@link SessionFactoryObserverForSchemaExport}, so that the schema is up-to-date.
 */
public final class SessionFactoryObserverForDataPopulation implements SessionFactoryObserver {

    @Override
    public void sessionFactoryCreated(SessionFactory factory) {
        factory.getSchemaManager().populate();
    }
}
