package io.quarkus.hibernate.orm.persistenceunitextension;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.inject.spi.DeploymentException;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Interceptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidTypeForPersistenceUnitExtensionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .assertException(t -> {
                assertThat(t)
                        .isInstanceOf(DeploymentException.class)
                        .hasMessageContainingAll(
                                "A @PersistenceUnitExtension bean must implement one or more of the following types:",
                                TenantResolver.class.getName(),
                                TenantConnectionResolver.class.getName(),
                                Interceptor.class.getName(),
                                "Invalid bean:",
                                InvalidExtension.class.getName());
            })
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyEntity.class, InvalidExtension.class))
            .withConfigurationResource("application.properties");

    @Test
    public void testInvalidConfiguration() {
        // deployment exception should happen first
        Assertions.fail();
    }

    // Make sure this type implements a Hibernate ORM interface that is usually implemented by users,
    // but one that is not supported with @PersistenceUnitExtension
    // (see HibernateOrmCdiProcessor.PERSISTENCE_UNIT_EXTENSION_VALID_TYPES).
    @PersistenceUnitExtension
    public static class InvalidExtension implements PhysicalNamingStrategy {
        private IllegalStateException shouldNotBeCalled() {
            return new IllegalStateException("This method should not be called in this test");
        }

        @Override
        public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment jdbcEnvironment) {
            throw shouldNotBeCalled();
        }

        @Override
        public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment) {
            throw shouldNotBeCalled();
        }

        @Override
        public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
            throw shouldNotBeCalled();
        }

        @Override
        public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment jdbcEnvironment) {
            throw shouldNotBeCalled();
        }

        @Override
        public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment) {
            throw shouldNotBeCalled();
        }
    }

    @Entity
    public static class MyEntity {
        @Id
        public Long id;

        public MyEntity() {
        }
    }
}
