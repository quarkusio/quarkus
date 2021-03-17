package io.quarkus.hibernate.orm;

import java.util.Collection;
import java.util.Optional;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.MappedSuperclass;
import javax.persistence.metamodel.StaticMetamodel;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.DB297Dialect;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.hibernate.orm.deployment.Dialects;
import io.quarkus.hibernate.orm.deployment.HibernateOrmCdiProcessor;
import io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor;
import io.quarkus.hibernate.orm.deployment.HibernateUserTypeProcessor;
import io.quarkus.hibernate.orm.deployment.JpaJandexScavenger;
import io.quarkus.hibernate.orm.runtime.dialect.QuarkusH2Dialect;
import io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect;

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
        assertMatch(HibernateOrmProcessor.PROXY, org.hibernate.annotations.Proxy.class);
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

    @Test
    public void testJpaJandexScavenger() {
        assertMatch(JpaJandexScavenger.JPA_ENTITY, Entity.class);
        assertMatch(JpaJandexScavenger.EMBEDDABLE, Embeddable.class);
        assertMatch(JpaJandexScavenger.EMBEDDED_ANNOTATIONS, Embedded.class, ElementCollection.class);
        assertMatch(JpaJandexScavenger.MAPPED_SUPERCLASS, MappedSuperclass.class);
        assertMatch(JpaJandexScavenger.ENUM, Enum.class);
    }

    @Test
    public void testDialectNames() {
        assertDialectMatch(DatabaseKind.DB2, DB297Dialect.class);
        assertDialectMatch(DatabaseKind.POSTGRESQL, QuarkusPostgreSQL10Dialect.class);
        assertDialectMatch(DatabaseKind.H2, QuarkusH2Dialect.class);
        assertDialectMatch(DatabaseKind.MARIADB, MariaDB103Dialect.class);
        assertDialectMatch(DatabaseKind.MYSQL, MySQL8Dialect.class);
        assertDialectMatch(DatabaseKind.DERBY, DerbyTenSevenDialect.class);
        assertDialectMatch(DatabaseKind.MSSQL, SQLServer2012Dialect.class);
    }

    private void assertDialectMatch(String dbName, Class<?> dialectClass) {
        final Optional<String> guessDialect = Dialects.guessDialect(dbName);
        Assertions.assertTrue(guessDialect.isPresent());
        Assertions.assertEquals(dialectClass.getName(), guessDialect.get());
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
