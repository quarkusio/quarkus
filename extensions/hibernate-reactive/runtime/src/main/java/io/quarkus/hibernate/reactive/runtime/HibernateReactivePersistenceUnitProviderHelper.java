package io.quarkus.hibernate.reactive.runtime;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.qualifier;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.QuarkusPersistenceUnitProviderHelper;

public class HibernateReactivePersistenceUnitProviderHelper implements QuarkusPersistenceUnitProviderHelper {
    @Override
    public boolean isActive(String persistenceUnitName) {
        var instance = Arc.container().select(Mutiny.SessionFactory.class, qualifier(persistenceUnitName));
        return instance.isResolvable() && instance.getHandle().getBean().isActive();
    }
}
