package io.quarkus.hibernate.orm;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.enhancer.Address;
import io.quarkus.test.QuarkusUnitTest;

public class HibernateMetadataWithInjectionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Address.class)
                    .addClass(MyEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    HibernateMetadata hibernateMetadata;

    @Inject
    EntityManager em;

    @Test
    public void testExpectedEntities() {
        assertThat(hibernateMetadata).isNotNull().satisfies(h -> {
            assertThat(h.getDefaultPersistenceUnitMetadata()).hasValueSatisfying(pu -> {
                assertThat(pu.getEntityClassNames()).containsOnly(Address.class.getName(), MyEntity.class.getName());
            });
        });
    }
}
