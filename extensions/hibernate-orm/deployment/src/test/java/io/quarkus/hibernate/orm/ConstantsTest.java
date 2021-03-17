package io.quarkus.hibernate.orm;

import javax.persistence.metamodel.StaticMetamodel;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor;
import io.quarkus.hibernate.orm.deployment.HibernateUserTypeProcessor;

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

    @Test
    public void testHibernateUserTypeProcessor() {
        assertMatch(HibernateUserTypeProcessor.TYPE, org.hibernate.annotations.Type.class);
        assertMatch(HibernateUserTypeProcessor.TYPE_DEFINITION, org.hibernate.annotations.TypeDef.class);
        assertMatch(HibernateUserTypeProcessor.TYPE_DEFINITIONS, org.hibernate.annotations.TypeDefs.class);
    }

    private void assertMatch(final DotName dotName, final Class<?> clazz) {
        Assertions.assertEquals(dotName.toString(), clazz.getName());
    }

}
