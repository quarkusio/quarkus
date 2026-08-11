package io.quarkus.it.panache.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReactiveTestTransactionQuarkusTestTest {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Inject
    Mutiny.Session session;

    @Test
    @TestTransaction
    @Order(1)
    public Uni<Void> testInsertIsRolledBack() {
        Person person = new Person();
        person.name = "QuarkusTestReactivePerson";
        return session.persist(person)
                .call(() -> session.flush())
                .invoke(() -> assertThat(person.id).isNotNull());
    }

    @Test
    @RunOnVertxContext
    @Order(2)
    public void testInsertWasRolledBack(UniAsserter asserter) {
        asserter.assertThat(
                () -> sessionFactory.withSession(s -> s
                        .createQuery("select count(p) from Person2 p where p.name = 'QuarkusTestReactivePerson'",
                                Long.class)
                        .getSingleResult()),
                count -> assertThat(count).isEqualTo(0L));
    }
}
