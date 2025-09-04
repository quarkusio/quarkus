package io.quarkus.hibernate.orm.runtime;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.qualifier;

import org.hibernate.SessionFactory;

import io.quarkus.arc.Arc;

public class HibernateOrmPersistenceUnitProviderHelper implements QuarkusPersistenceUnitProviderHelper {
    @Override
    public boolean isActive(String persistenceUnitName) {
        var instance = Arc.container().select(SessionFactory.class, qualifier(persistenceUnitName));
        return instance.isResolvable() && instance.getHandle().getBean().isActive();
    }
}
