package io.quarkus.hibernate.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

public class NoJtaTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    Mutiny.SessionFactory factory;

    @Test
    @RunOnVertxContext
    public void test(UniAsserter asserter) {

        // Quick test to make sure HRX works
        MyEntity entity = new MyEntity("default");

        asserter.assertThat(() -> factory.withTransaction((session, tx) -> session.persist(entity))
                .chain(() -> factory.withTransaction((session, tx) -> session.clear().find(MyEntity.class, entity.getId()))),
                retrievedEntity -> assertThat(retrievedEntity).isNotSameAs(entity).returns(entity.getName(),
                        MyEntity::getName));
    }

    @Entity
    public static class MyEntity {

        private long id;

        private String name;

        public MyEntity() {
        }

        public MyEntity(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + ":" + name;
        }

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "defaultSeq")
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
