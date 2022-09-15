package io.quarkus.hibernate.orm.envers;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.internal.SessionImpl;
import org.hibernate.persister.entity.EntityPersister;

public abstract class AbstractEnversResource {
    @Inject
    public EntityManager em;

    @Inject
    public UserTransaction transaction;

    public String getDefaultAuditEntityName(Class<?> clazz) {
        return clazz.getName() + "_AUD";
    }

    public EntityPersister getEntityPersister(String entityName) {
        return ((SessionImpl) em.getDelegate()).getSessionFactory().getMetamodel().entityPersister(entityName);
    }

    public AuditEntitiesConfiguration getAuditEntitiesConfiguration() {
        return getEnversService().getAuditEntitiesConfiguration();
    }

    public GlobalConfiguration getGlobalConfiguration() {
        return getEnversService().getGlobalConfiguration();
    }

    public AuditStrategy getAuditStrategy() {
        return getEnversService().getAuditStrategy();
    }

    public EnversService getEnversService() {
        return ((((SessionImpl) em.getDelegate()).getFactory().getServiceRegistry())
                .getParentServiceRegistry())
                .getService(EnversService.class);
    }
}
