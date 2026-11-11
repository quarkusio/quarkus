package io.quarkus.hibernate.search.orm.elasticsearch.test.accessor;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusExtensionTest;

public class AccessorDiscoveryTest {
    @RegisterExtension
    static final QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(Person.class, Address.class, PersonProjection.class, AddressProjection.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-accessor.strategy", "generated");

    @Inject
    SearchSession session;

    @Test
    @ActivateRequestContext
    void embeddedNonPersistentTypesAndProjectionConstructors() {
        QuarkusTransaction.requiringNew().run(() -> session.toEntityManager().persist(new Person(1L, new Address("Paris"))));
        QuarkusTransaction.requiringNew().run(() -> {
            assertThat(session.search(Person.class).select(PersonProjection.class)
                    .where(f -> f.match().field("address.city").matching("Paris")).fetchHits(10))
                    .containsExactly(new PersonProjection(new AddressProjection("Paris")));
        });
    }

    @Entity
    @Indexed
    public static class Person {
        @Id
        private long id;
        @Transient
        @IndexedEmbedded
        @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
        private Address address;

        public Person() {
        }

        public Person(long id, Address address) {
            this.id = id;
            this.address = address;
        }
    }

    public static class Address {
        @KeywordField(projectable = Projectable.YES)
        private final String city;

        public Address(String city) {
            this.city = city;
        }
    }

    @ProjectionConstructor
    public record PersonProjection(AddressProjection address) {
    }

    public record AddressProjection(String city) {
        @ProjectionConstructor
        public AddressProjection {
        }
    }
}
