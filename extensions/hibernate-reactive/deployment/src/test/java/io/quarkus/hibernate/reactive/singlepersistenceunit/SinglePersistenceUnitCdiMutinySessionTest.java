package io.quarkus.hibernate.reactive.singlepersistenceunit;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

public class SinglePersistenceUnitCdiMutinySessionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    @RunOnVertxContext
    public void test(UniAsserter asserter) {
        DefaultEntity entity = new DefaultEntity("default");
        asserter.assertThat(() -> sessionFactory.withTransaction((session, tx) -> session.persist(entity))
                .chain(() -> sessionFactory.withSession(session -> session.find(DefaultEntity.class, entity.getId()))),
                retrievedEntity -> assertThat(retrievedEntity)
                        .isNotSameAs(entity)
                        .returns(entity.getName(), DefaultEntity::getName));
    }
}
