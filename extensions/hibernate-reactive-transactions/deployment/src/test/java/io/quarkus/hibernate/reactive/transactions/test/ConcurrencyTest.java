package io.quarkus.hibernate.reactive.transactions.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniAndGroup2;

public class ConcurrencyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hero.class)
                    .addAsResource("initialTransactionData.sql", "import.sql"))
            .withConfigurationResource("application.properties");

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    @RunOnVertxContext
    public void testCombineOperation(UniAsserter asserter) {
        // initialTransactionData.sql
        Long previousHeroId = 70L;

        Random rand = new Random();

        int wait1 = rand.nextInt(1, 1000);
        int wait2 = rand.nextInt(1, 1000);

        Uni<Hero> doSomething1 = sessionFactory.withTransaction(session -> {
            System.out.println("Start update 1 waiting " + wait1 + " threadId " + Thread.currentThread().getId());
            blockThread(wait1);
            return updateHero(session, previousHeroId, "updatedName1")
                    .onItem().invoke(() -> System.out.println("End update 1 threadId " + Thread.currentThread().getId()));

        });

        Uni<Hero> doSomething2 = sessionFactory.withTransaction(session -> {
            System.out.println("Start update 2 waiting " + wait2 + " threadId " + Thread.currentThread().getId());
            blockThread(wait2);
            return updateHero(session, previousHeroId, "updatedName2").onItem()
                    .invoke(() -> System.out.println("End update 2 threadId " + Thread.currentThread().getId()));
        });

        UniAndGroup2<Hero, Hero> result = Uni.combine().all().unis(
                doSomething1,
                doSomething2);

        Uni<Hero> refreshedHero = result.withUni((h1, h2) -> {
            return null;
        }).onFailure().recoverWithNull()
                .chain(id -> sessionFactory.withTransaction(session -> findHero(session, previousHeroId)));

        asserter.assertThat(() -> refreshedHero, h -> {
            assertThat(h.name).isEqualTo("updatedName2");
        });

    }

    private static void blockThread(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Uni<Hero> updateHero(Mutiny.Session session, Long id, String newName) {
        return session.find(Hero.class, id)
                .map(h -> {
                    h.setName(newName);
                    return h;
                }).call(() -> session.flush());
    }

    public Uni<Hero> findHero(Mutiny.Session session, Long id) {
        return session.find(Hero.class, id);
    }

}
