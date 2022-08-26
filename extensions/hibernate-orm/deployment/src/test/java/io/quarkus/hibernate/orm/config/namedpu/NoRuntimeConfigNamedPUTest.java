package io.quarkus.hibernate.orm.config.namedpu;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.Bindable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.PersistenceUnit.PersistenceUnitLiteral;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test that Hibernate ORM is able to start a named PU,
 * even when there is no runtime configuration for that PU.
 * <p>
 * This case is interesting mainly because it would most likely lead to an NPE
 * unless we take extra care to avoid it.
 */
public class NoRuntimeConfigNamedPUTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addPackage(MyEntity.class.getPackage().getName()))
            .overrideConfigKey("quarkus.datasource.\"ds-1\".db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.\"ds-1\".jdbc.url", "jdbc:h2:mem:test")
            // For Hibernate ORM, provide only build-time config; rely on defaults for everything else.
            .overrideConfigKey("quarkus.hibernate-orm.\"pu-1\".datasource", "ds-1")
            // Disable dev services, since they could interfere with runtime config (in particular the schema generation settings).
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Test
    public void test() {
        // Check that runtime config for our PU is indeed missing.
        HibernateOrmRuntimeConfig config = Arc.container().instance(HibernateOrmRuntimeConfig.class).get();
        assertThat(config.persistenceUnits).isNullOrEmpty();
        // Test that Hibernate ORM was started correctly regardless.
        EntityManagerFactory entityManagerFactory = Arc.container()
                .instance(EntityManagerFactory.class, new PersistenceUnitLiteral("pu-1")).get();
        assertThat(entityManagerFactory).isNotNull();
        assertThat(entityManagerFactory.getMetamodel().getEntities())
                .extracting(Bindable::getBindableJavaType)
                .containsExactlyInAnyOrder((Class) MyEntity.class);
    }
}
