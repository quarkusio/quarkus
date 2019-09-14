package io.quarkus.hibernate.orm.panache.test.multiple_pu;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.persistence.EntityManager;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultingToConfiguredEmTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(EntityWithFirstPU.class, EntityWithSecondPU.class)
                    .addAsResource(new StringAsset("" +
                            "quarkus.datasource.url=jdbc:h2:mem:test\n" +
                            "quarkus.datasource.driver=org.h2.Driver\n" +
                            "quarkus.hibernate-orm.dialect=org.hibernate.dialect.H2Dialect\n"),
                            "application.properties"));

    @Test
    public void shouldDefaultToConfiguredPersistenceUnit() {
        EntityManager entityManager = JpaOperations.getEntityManager(EntityWithFirstPU.class);
        assertNotNull(entityManager);

        entityManager = JpaOperations.getEntityManager(EntityWithSecondPU.class);
        assertNotNull(entityManager);
    }
}
