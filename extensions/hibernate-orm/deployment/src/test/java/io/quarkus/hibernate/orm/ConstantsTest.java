package io.quarkus.hibernate.orm;

import javax.persistence.metamodel.StaticMetamodel;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor;

public class ConstantsTest {

    @Test
    public void testConstantsInHibernateOrmProcessor() {
        assertMatch(HibernateOrmProcessor.TENANT_CONNECTION_RESOLVER,
                io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver.class);
        assertMatch(HibernateOrmProcessor.TENANT_RESOLVER, io.quarkus.hibernate.orm.runtime.tenant.TenantResolver.class);
        assertMatch(HibernateOrmProcessor.STATIC_METAMODEL, StaticMetamodel.class);
        assertMatch(HibernateOrmProcessor.PERSISTENCE_UNIT, PersistenceUnit.class);
        assertMatch(HibernateOrmProcessor.PERSISTENCE_UNIT_REPEATABLE_CONTAINER, PersistenceUnit.List.class);
        assertMatch(HibernateOrmProcessor.JPA_ENTITY, javax.persistence.Entity.class);
        assertMatch(HibernateOrmProcessor.MAPPED_SUPERCLASS, javax.persistence.MappedSuperclass.class);
    }

    private void assertMatch(final DotName dotName, final Class<?> clazz) {
        Assertions.assertEquals(dotName.toString(), clazz.getName());
    }

}
