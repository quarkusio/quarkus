package io.quarkus.hibernate.reactive.transaction;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
import static io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder.OPENED_SESSIONS_STATE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.runtime.OpenedSessionsState;
import io.quarkus.reactive.transaction.TransactionalInterceptorRequired;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

/**
 * Test to demonstrate transaction context leakage when combining declarative transactional methods
 * with non-transactional methods in parallel reactive chains.
 *
 * This test is disabled because it's not fixed yet. The transaction context leaks between method calls
 * when they are combined in parallel using Uni.combine().all().unis(). This is a known limitation of
 * declarative transaction management using Vert.x context-based interceptors:
 * - @Transactional (jakarta.transaction.Transactional / io.quarkus.reactive.transaction.Transactional)
 * - Panache @WithTransaction / @WithSession / @WithSessionOnDemand
 *
 * Workaround: Use programmatic transaction management (sessionFactory.withTransaction() or
 * Panache.withTransaction()) which doesn't have this problem because you explicitly control where
 * the transaction is created.
 *
 * See documentation in hibernate-reactive.adoc for details on this limitation and workarounds.
 */
@Disabled("Known issue: Transaction context leaks with declarative transaction management in parallel chains - not fixed yet")
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
    public void testTransactionLeakageBetweenMethods(UniAsserter asserter) {
        // This test demonstrates that when combining @Transactional and non-transactional methods
        // in parallel, the transaction context leaks to the non-transactional method.
        // The assertion in nonTransactionalMethod() will fail because it expects no session,
        // but the session from findHero() will leak.
        asserter.execute(() -> Uni.combine().all().unis(findHero(50L), nonTransactionalMethod()));
    }

    @Transactional
    public Uni<Hero> findHero(Long heroId) {

        Uni<Hero> heroUni = session.find(Hero.class, heroId).invoke(s -> {
            Optional<OpenedSessionsState.SessionWithKey<Mutiny.Session>> openedSessionsState = OPENED_SESSIONS_STATE
                    .getOpenedSession(Vertx.currentContext(), DEFAULT_PERSISTENCE_UNIT_NAME);

            System.out.println(openedSessionsState.get());

        });
        return heroUni.chain(s -> Uni.createFrom().nothing());
    }

    public Uni<Void> nonTransactionalMethod() {

        Optional<OpenedSessionsState.SessionWithKey<Mutiny.Session>> openedSessionsState = OPENED_SESSIONS_STATE
                .getOpenedSession(Vertx.currentContext(), DEFAULT_PERSISTENCE_UNIT_NAME);

        // This will fail as the transaction will leak here.
        // This method shouldn't be able to access the transaction
        assertThat(openedSessionsState).isEmpty();

        return Uni.createFrom().nullItem();
    }
}