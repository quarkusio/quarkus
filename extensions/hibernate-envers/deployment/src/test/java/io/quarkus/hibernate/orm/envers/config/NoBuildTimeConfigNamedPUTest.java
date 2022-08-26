package io.quarkus.hibernate.orm.envers.config;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.Bindable;

import org.hibernate.envers.DefaultRevisionEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.envers.HibernateEnversBuildTimeConfig;
import io.quarkus.hibernate.orm.PersistenceUnit.PersistenceUnitLiteral;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test that Hibernate Envers is able to start in a named PU,
 * even when there is no build-time configuration for that PU.
 * <p>
 * This case is interesting mainly because it would most likely lead to an NPE
 * unless we take extra care to avoid it.
 */
public class NoBuildTimeConfigNamedPUTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyAuditedEntity.class))
            .overrideConfigKey("quarkus.datasource.\"ds-1\".db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.\"ds-1\".jdbc.url", "jdbc:h2:mem:test")
            .overrideConfigKey("quarkus.hibernate-orm.\"pu-1\".datasource", "ds-1")
            .overrideConfigKey("quarkus.hibernate-orm.\"pu-1\".packages",
                    MyAuditedEntity.class.getPackage().getName())
            // For Hibernate Envers, do not provide any build-time config; rely on defaults for everything.

            // Disable dev services, since they could interfere with runtime config (in particular the schema generation settings).
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            // Make build-time config accessible through CDI
            .overrideConfigKey("quarkus.arc.unremovable-types", HibernateEnversBuildTimeConfig.class.getName());

    @Test
    public void test() {
        // Check that build-time config for our PU is indeed missing.
        HibernateEnversBuildTimeConfig config = Arc.container()
                .instance(HibernateEnversBuildTimeConfig.class).get();
        assertThat(config.persistenceUnits).isNullOrEmpty();
        // Test that Hibernate Envers was started correctly regardless.
        EntityManagerFactory entityManagerFactory = Arc.container()
                .instance(EntityManagerFactory.class, new PersistenceUnitLiteral("pu-1")).get();
        assertThat(entityManagerFactory.getMetamodel().getEntities())
                .extracting(Bindable::getBindableJavaType)
                .contains((Class) DefaultRevisionEntity.class);
    }
}
