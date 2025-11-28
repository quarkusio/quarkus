package io.quarkus.hibernate.reactive.panache.test.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.hibernate.reactive.runtime.transaction.AfterWorkTransactionStrategy;
import io.quarkus.reactive.transaction.TransactionalInterceptorRequired;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class MixWithTransactionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TransactionalInterceptorRequired.class, AfterWorkTransactionStrategy.class));

    @Test
    @RunOnVertxContext
    public void testTransactionalCallingWithTransaction(UniAsserter asserter) {
        asserter.assertFailedWith(
                () -> methodAnnotatedWithTransactionalCallingWithTransaction(),
                t -> assertThat(t)
                        .hasMessageContaining(
                                "Cannot call a method annotated with @WithTransaction from a method annotated with @Transactional"));
    }

    @Transactional
    public Uni<?> methodAnnotatedWithTransactionalCallingWithTransaction() {
        Uni<?> a = Uni.createFrom().item("transactional_method");
        return a.flatMap(b -> methodAnnotatedWithTransaction());
    }

    @WithTransaction
    public Uni<String> methodAnnotatedWithTransaction() {
        return Uni.createFrom().item("with_transaction");
    }

    @Test
    @RunOnVertxContext
    public void testWithTransactionCallingTransactional(UniAsserter asserter) {
        // This should tell users do not do this and migrate to @Transactional
        asserter.assertFailedWith(
                () -> methodAnnotatedWithTransactionCallingTransactional(),
                t -> assertThat(t)
                        .hasMessageContaining(
                                "Cannot call a method annotated with @Transactional from a method annotated with @WithTransaction"));
    }

    @WithTransaction
    public Uni<?> methodAnnotatedWithTransactionCallingTransactional() {
        Uni<?> a = Uni.createFrom().item("with_transaction");
        return a.flatMap(b -> methodAnnotatedWithTransactional());
    }

    @Transactional
    public Uni<String> methodAnnotatedWithTransactional() {
        return Uni.createFrom().item("transactional_method");
    }
}
