package io.quarkus.it.panache.reactive;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

/**
 * The goal if this class is to test the usage of {@link WithTransaction},
 * you should not use {@link io.quarkus.hibernate.reactive.panache.Panache#withTransaction(Supplier)}
 * nor {@link org.hibernate.reactive.mutiny.Mutiny.SessionFactory#withTransaction(Function)}
 */
@ApplicationScoped
@WithTransaction
public class WithTransactionCounterBean {

    private static final Long ID_COUNTER = 42L;

    @Inject
    Mutiny.SessionFactory sessionFactory;

    public Uni<Counter> createOrResetCounter() {
        final Counter counter = new Counter(ID_COUNTER);
        return sessionFactory.withStatelessSession(session -> session
                .upsert(counter)
                .replaceWith(counter));
    }

    public Uni<Counter> increaseCounterWithHR() {
        return sessionFactory.withSession(session -> session
                .find(Counter.class, ID_COUNTER, PESSIMISTIC_WRITE)
                .invoke(Counter::increase));
    }

    public Uni<Counter> increaseCounterWithPanache() {
        return Counter
                .<Counter> findById(ID_COUNTER, PESSIMISTIC_WRITE)
                .invoke(Counter::increase);
    }

    public Uni<Void> assertThatSessionsAreEqual() {
        return sessionFactory.withSession(hrSession -> Panache
                .getSession().chain(panacheSession -> {
                    if (panacheSession != hrSession) {
                        return Uni.createFrom().failure(new AssertionError("Sessions are different!"));
                    }
                    return Uni.createFrom().voidItem();
                }));
    }

    public Uni<Counter> findCounter() {
        return sessionFactory.withSession(session -> session
                .find(Counter.class, ID_COUNTER));
    }
}
