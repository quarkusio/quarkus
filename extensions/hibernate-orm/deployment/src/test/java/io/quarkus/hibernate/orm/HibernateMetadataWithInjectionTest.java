package io.quarkus.hibernate.orm;

import javax.inject.Inject;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;

import io.quarkus.hibernate.orm.enhancer.Address;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class HibernateMetadataWithInjectionTest {

    @Embeddable
    public static class SomeEmbeddable {
        private String name = "";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Entity
    public static class SomeEntity {
        @Id
        private Long id = 0L;

        @Embedded
        private SomeEmbeddable someEmbeddable = new SomeEmbeddable();

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public SomeEmbeddable getSomeEmbeddable() {
            return someEmbeddable;
        }

        public void setSomeEmbeddable(SomeEmbeddable someEmbeddable) {
            this.someEmbeddable = someEmbeddable;
        }
    }

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Address.class)
                    .addClass(MyEntity.class)
                    .addClass(SomeEntity.class)
                    .addClass(SomeEmbeddable.class)
                    .addAsResource("application.properties"));

    @Inject
    HibernateMetadata hibernateMetadata;

    @Inject
    EntityManager em;

    @Test
    public void testExpectedEntityNames() {
        assertThat(hibernateMetadata).isNotNull().satisfies(h -> {
            assertThat(h.getDefaultPersistenceUnitMetadata()).hasValueSatisfying(pu -> {
                assertThat(pu.getEntityClassNames())
                        .containsOnly(Address.class.getName(), MyEntity.class.getName(), SomeEntity.class.getName());
            });
        });
    }

    @Test
    public void testExpectedEnties() {
        assertThat(hibernateMetadata).isNotNull().satisfies(h -> {
            assertThat(h.getDefaultPersistenceUnitMetadata()).hasValueSatisfying(pu -> {
                assertThat(pu.resolveEntityClasses())
                        .containsOnly(Address.class, MyEntity.class, SomeEntity.class);
            });
        });
    }
}
