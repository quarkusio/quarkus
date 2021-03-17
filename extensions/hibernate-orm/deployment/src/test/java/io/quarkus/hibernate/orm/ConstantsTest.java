package io.quarkus.hibernate.orm;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.StaticMetamodel;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.deployment.HibernateOrmCdiProcessor;
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

    @Test
    public void testHibernateOrmCdiProcessor() {
        assertMatch(HibernateOrmCdiProcessor.SESSION_FACTORY_EXPOSED_TYPES, EntityManagerFactory.class, SessionFactory.class);
        assertMatch(HibernateOrmCdiProcessor.SESSION_EXPOSED_TYPES, EntityManager.class, Session.class);
        assertMatch(HibernateOrmCdiProcessor.PERSISTENCE_UNIT_QUALIFIER, PersistenceUnit.class);
        assertMatch(HibernateOrmCdiProcessor.JPA_PERSISTENCE_UNIT, javax.persistence.PersistenceUnit.class);
        assertMatch(HibernateOrmCdiProcessor.JPA_PERSISTENCE_CONTEXT, javax.persistence.PersistenceContext.class);
    }

    private void assertMatch(final DotName dotName, final Class<?> clazz) {
        Assertions.assertEquals(dotName.toString(), clazz.getName());
    }

    private void assertMatch(final Collection<DotName> dotNames, final Class<?>... clazzez) {
        Assertions.assertEquals(dotNames.size(), clazzez.length);
        for (Class c : clazzez) {
            Assertions.assertTrue(oneMatches(dotNames, c));
        }
    }

    private boolean oneMatches(Collection<DotName> dotNames, Class c) {
        int matches = 0;
        for (DotName d : dotNames) {
            if (c.getName().equals(d.toString()))
                matches++;
        }
        return 1 == matches;
    }

}
