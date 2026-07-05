package io.quarkus.hibernate.reactive.transaction;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
import static io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder.OPENED_SESSIONS_STATE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.runtime.OpenedSessionsState;
import io.quarkus.reactive.transaction.runtime.TransactionalInterceptorRequired;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

/**
 * Verifies that declarative transactional methods combined in parallel using {@code Uni.combine().all().unis()}
 * do not leak session state to other reactive pipelines on the same request context.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/52815">Issue #52815</a>
 */
public class InterleaveTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(Hero.class)
                    .addClasses(TransactionalInterceptorRequired.class)
                    .addAsResource("initialTransactionData.sql", "import.sql"))
            .withConfigurationResource("application-reactive-transaction.properties");

    @Inject
    Mutiny.Session session;

    @Test
    @RunOnVertxContext
    public void testTransactionDoesNotLeakToNonTransactionalParallelMethod(UniAsserter asserter) {
        asserter.execute(() -> Uni.combine().all().unis(findHero(50L), nonTransactionalMethod()));
    }

    @Test
    @RunOnVertxContext
    public void testParallelTransactionalMethodsUseIsolatedSessions(UniAsserter asserter) {
        asserter.assertThat(
                () -> Uni.combine().all().unis(findHero(50L), findHeroName(50L)).asTuple(),
                tuple -> {
                    assertThat(tuple.getItem1().name).isEqualTo("initialName");
                    assertThat(tuple.getItem2()).isEqualTo("initialName");
                });
    }

    @Transactional
    public Uni<Hero> findHero(Long heroId) {
        return session.find(Hero.class, heroId);
    }

    @Transactional
    public Uni<String> findHeroName(Long heroId) {
        return session.find(Hero.class, heroId).map(Hero::getName);
    }

    public Uni<Void> nonTransactionalMethod() {
        Optional<OpenedSessionsState.SessionWithKey<Mutiny.Session>> openedSessionsState = OPENED_SESSIONS_STATE
                .getOpenedSession(Vertx.currentContext(), DEFAULT_PERSISTENCE_UNIT_NAME);

        assertThat(openedSessionsState).isEmpty();

        return Uni.createFrom().voidItem();
    }
}
