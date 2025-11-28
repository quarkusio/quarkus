package io.quarkus.hibernate.reactive.panache.test.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.common.WithSessionOnDemand;
import io.quarkus.hibernate.reactive.runtime.transaction.AfterWorkTransactionStrategy;
import io.quarkus.reactive.transaction.TransactionalInterceptorRequired;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class MixWithSessionOnDemandTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TransactionalInterceptorRequired.class, AfterWorkTransactionStrategy.class));

    @Test
    @RunOnVertxContext
    public void testTransactionalCallingSessionOnDemand(UniAsserter asserter) {
        asserter.assertFailedWith(
                () -> methodAnnotatedWithTransactionalCallingSessionOnDemand(),
                t -> assertThat(t)
                        .hasMessageContaining(
                                "Cannot call a method annotated with @WithSessionOnDemand from a method annotated with @Transactional"));
    }

    @Transactional
    public Uni<?> methodAnnotatedWithTransactionalCallingSessionOnDemand() {
        Uni<?> a = Uni.createFrom().item("transactional_method");
        return a.flatMap(b -> methodAnnotatedWithSessionOnDemand());
    }

    @WithSessionOnDemand
    public Uni<String> methodAnnotatedWithSessionOnDemand() {
        return Uni.createFrom().item("whatever");
    }

    @Test
    @RunOnVertxContext
    public void testSessionOnDemandCallingTransactional(UniAsserter asserter) {
        // This should tell users do not do this and migrate to @Transactional
        asserter.assertFailedWith(
                () -> methodAnnotatedWithSessionOnDemandCallingTransactional(),
                t -> assertThat(t)
                        .hasMessageContaining(
                                "Cannot call a method annotated with @Transactional from a method annotated with @WithSessionOnDemand"));
    }

    @WithSessionOnDemand
    public Uni<?> methodAnnotatedWithSessionOnDemandCallingTransactional() {
        Uni<?> a = Uni.createFrom().item("with_session_on_demand_method");
        return a.flatMap(b -> methodAnnotatedWithTransactional());
    }

    @Transactional
    public Uni<String> methodAnnotatedWithTransactional() {
        return Uni.createFrom().item("transactional_method");
    }
}
