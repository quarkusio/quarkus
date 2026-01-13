package io.quarkus.hibernate.reactive.panache.test.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.hibernate.reactive.runtime.transaction.AfterWorkTransactionStrategy;
import io.quarkus.reactive.transaction.TransactionalInterceptorRequired;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class MixReactiveTransactionalTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TransactionalInterceptorRequired.class, AfterWorkTransactionStrategy.class));

    @Test
    @RunOnVertxContext
    public void testTransactionalCallingReactiveTransactional(UniAsserter asserter) {
        asserter.assertFailedWith(
                () -> methodAnnotatedWithTransactionalCallingReactiveTransactional(),
                t -> assertThat(t)
                        .hasMessageContaining(
                                "Cannot call a method annotated with @ReactiveTransactional from a method annotated with @Transactional"));
    }

    @Transactional
    public Uni<?> methodAnnotatedWithTransactionalCallingReactiveTransactional() {
        Uni<?> a = Uni.createFrom().item("transactional_method");
        return a.flatMap(b -> methodAnnotatedWithReactiveTransactional());
    }

    @ReactiveTransactional
    public Uni<String> methodAnnotatedWithReactiveTransactional() {
        return Uni.createFrom().item("reactive_transactional");
    }

    @Test
    @RunOnVertxContext
    public void testReactiveTransactionalCallingTransactional(UniAsserter asserter) {
        asserter.assertFailedWith(
                () -> methodAnnotatedWithReactiveTransactionalCallingTransactional(),
                t -> assertThat(t)
                        .hasMessageContaining(
                                "Cannot call a method annotated with @Transactional from a method annotated with @ReactiveTransactional"));
    }

    @ReactiveTransactional
    public Uni<?> methodAnnotatedWithReactiveTransactionalCallingTransactional() {
        Uni<?> a = Uni.createFrom().item("reactive_transactional");
        return a.flatMap(b -> methodAnnotatedWithTransactional());
    }

    @Transactional
    public Uni<String> methodAnnotatedWithTransactional() {
        return Uni.createFrom().item("transactional_method");
    }
}
