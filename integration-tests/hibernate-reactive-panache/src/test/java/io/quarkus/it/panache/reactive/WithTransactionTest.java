package io.quarkus.it.panache.reactive;

import static io.quarkus.vertx.VertxContextSupport.subscribeAndAwait;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;

/**
 * Hibernate Reactive and Panache store the created sessions in a local Vert.x context,
 * and we need to make sure that they won't get lost.
 * See issue <a href="https://github.com/quarkusio/quarkus/issues/47314">47314</a>
 *
 * @see io.quarkus.hibernate.reactive.panache.common.runtime.SessionOperations
 * @see io.quarkus.hibernate.reactive.panache.common.WithTransaction
 */
@QuarkusTest
public class WithTransactionTest {
    // How many times we want to increase the counter
    private static final int INCREASES_NUM = 10;

    @Inject
    WithTransactionCounterBean counterBean;

    @BeforeEach
    public void createOrResetCounter() throws Throwable {
        subscribeAndAwait(counterBean::createOrResetCounter);
    }

    @Test
    void increaseCounterWithHibernateReactive() throws Throwable {
        subscribeAndAwait(counterBean::increaseCounterWithHR);
        Counter counter = subscribeAndAwait(counterBean::findCounter);

        assertThat(counter.getCount()).isEqualTo(1);
    }

    @Test
    void increaseCounterWithPanache() throws Throwable {
        subscribeAndAwait(counterBean::increaseCounterWithPanache);
        Counter counter = subscribeAndAwait(counterBean::findCounter);

        assertThat(counter.getCount()).isEqualTo(1);
    }

    @Test
    void shouldReuseExistingSessions() throws Throwable {
        subscribeAndAwait(counterBean::assertThatSessionsAreEqual);
    }

    @Test
    void increaseCounter() throws Throwable {
        List<Supplier<Uni<Counter>>> suppliers = new ArrayList<>();
        for (int i = 0; i < INCREASES_NUM; i++) {
            suppliers.add(() -> counterBean.increaseCounterWithHR());
        }

        final List<Counter> results = new ArrayList<>();
        suppliers.stream()
                .parallel()
                .forEach(uni -> increaseCounter(uni, results));

        final Counter dbCounter = subscribeAndAwait(() -> counterBean.findCounter());
        assertThat(dbCounter.getCount()).isEqualTo(INCREASES_NUM);
    }

    private static void increaseCounter(Supplier<Uni<Counter>> func, List<Counter> counters) {
        try {
            final var counter = subscribeAndAwait(func);
            counters.add(counter);
        } catch (final Throwable e) {
            fail(e);
        }
    }
}
