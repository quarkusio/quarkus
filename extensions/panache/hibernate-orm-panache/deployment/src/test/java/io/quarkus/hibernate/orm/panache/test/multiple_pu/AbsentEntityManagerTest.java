package io.quarkus.hibernate.orm.panache.test.multiple_pu;

import javax.persistence.PersistenceException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.test.QuarkusUnitTest;

public class AbsentEntityManagerTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("quarkus.datasource.url=jdbc:h2:mem:test\n" +
                            "quarkus.datasource.driver=org.h2.Driver\n"),
                            "application.properties"));

    @Test
    public void shouldDefaultToConfiguredPersistenceUnit() {
        Assertions.assertThrows(PersistenceException.class, () -> {
            JpaOperations.getEntityManager(EntityWithFirstPU.class);
        });
    }
}
