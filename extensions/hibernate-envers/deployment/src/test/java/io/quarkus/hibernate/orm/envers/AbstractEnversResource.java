package io.quarkus.hibernate.orm.envers;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.strategy.AuditStrategy;
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
        return ((SessionImplementor) em.getDelegate()).getSessionFactory().getMappingMetamodel()
                .findEntityDescriptor(entityName);
    }

    public EntitiesConfigurations getEntitiesConfiguration() {
        return getEnversService().getEntitiesConfigurations();
    }

    public Configuration getConfiguration() {
        return getEnversService().getConfig();
    }

    public AuditStrategy getAuditStrategy() {
        return getEnversService().getAuditStrategy();
    }

    public EnversService getEnversService() {
        return ((((SessionImplementor) em.getDelegate()).getFactory().getServiceRegistry()).getParentServiceRegistry())
                .getService(EnversService.class);
    }
}
