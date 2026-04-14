package io.quarkus.hibernate.reactive.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class HibernateReactiveTransactionalRetryTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(Hero.class)
                    .addAsResource("initialTransactionRetryData.sql", "import.sql"))
            .withConfigurationResource("application-reactive-transaction.properties");

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Inject
    Mutiny.Session session;

    private final AtomicInteger callCounter = new AtomicInteger();

    @Test
    @RunOnVertxContext
    public void testTransactionalRetry(UniAsserter asserter) {
        Long hero1Id = 50L;
        Long hero2Id = 51L;
        callCounter.set(0);

        // Test that:
        // 1. First call fails -> DB changes to hero1 are rolled back
        // 2. Method is retried (second call)
        // 3. Second call succeeds -> DB changes to hero2 are committed
        asserter.assertThat(
                () -> updateHeroWithRetry(hero1Id, hero2Id)
                        .onFailure().retry().atMost(1),
                h -> {
                    // Verify the successful update returned hero2
                    assertThat(h.id).isEqualTo(hero2Id);
                    assertThat(h.name).isEqualTo("updatedHero2");
                    // Verify the method was called twice (first failed, second succeeded)
                    assertThat(callCounter.get()).isEqualTo(2);
                });

        // Verify that hero1 still has its original name (first call was rolled back)
        asserter.assertThat(
                () -> findHero(hero1Id),
                h -> assertThat(h.name).isEqualTo("hero1"));

        // Verify that hero2 has the updated name (second call was committed)
        asserter.assertThat(
                () -> findHero(hero2Id),
                h -> assertThat(h.name).isEqualTo("updatedHero2"));
    }

    @Transactional
    public Uni<Hero> updateHeroWithRetry(Long hero1Id, Long hero2Id) {
        int invocation = callCounter.incrementAndGet();

        if (invocation == 1) {
            // First call: update hero1, then fail
            return session.find(Hero.class, hero1Id)
                    .map(hero -> {
                        hero.setName("shouldBeRolledBack");
                        return hero;
                    })
                    .onItem().invoke(() -> {
                        throw new RuntimeException("Simulated failure on first call");
                    });
        } else {
            // Second call: update hero2, then succeed
            return session.find(Hero.class, hero2Id)
                    .map(hero -> {
                        hero.setName("updatedHero2");
                        return hero;
                    });
        }
    }

    @Transactional
    public Uni<Hero> findHero(Long heroId) {
        return session.find(Hero.class, heroId);
    }
}
