package io.quarkus.hibernate.reactive.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.transaction.runtime.TransactionalInterceptorRequired;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReactiveTestTransactionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(Hero.class)
                    .addClasses(TransactionalInterceptorRequired.class)
                    .addAsResource("initialTransactionData.sql", "import.sql"))
            .withConfigurationResource("application-reactive-transaction.properties");

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Inject
    Mutiny.Session session;

    @Test
    @TestTransaction
    @Order(1)
    public Uni<Void> testInsertIsRolledBack() {
        Hero hero = new Hero("ReactiveTestHero");
        return session.persist(hero)
                .call(() -> session.flush())
                .invoke(() -> assertThat(hero.id).isNotNull());
    }

    @Test
    @RunOnVertxContext
    @Order(2)
    public void testInsertWasRolledBack(UniAsserter asserter) {
        asserter.assertThat(
                () -> sessionFactory.withSession(s -> s.createQuery("from Hero where name = 'ReactiveTestHero'", Hero.class)
                        .getResultList()),
                list -> assertThat(list).isEmpty());
    }

    @Test
    @TestTransaction
    @Order(3)
    public Uni<Void> testUpdateIsRolledBack() {
        Long heroId = 50L;
        return session.find(Hero.class, heroId)
                .invoke(hero -> hero.setName("modifiedByReactiveTestTransaction"))
                .call(() -> session.flush())
                .invoke(hero -> assertThat(hero.getName()).isEqualTo("modifiedByReactiveTestTransaction"))
                .replaceWithVoid();
    }

    @Test
    @RunOnVertxContext
    @Order(4)
    public void testUpdateWasRolledBack(UniAsserter asserter) {
        Long heroId = 50L;
        asserter.assertThat(
                () -> sessionFactory.withSession(s -> s.find(Hero.class, heroId)),
                hero -> assertThat(hero.getName()).isEqualTo("initialName"));
    }
}
