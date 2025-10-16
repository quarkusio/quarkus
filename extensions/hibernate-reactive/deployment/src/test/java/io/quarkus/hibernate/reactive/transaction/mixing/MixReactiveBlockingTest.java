package io.quarkus.hibernate.reactive.transaction.mixing;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.reactive.transaction.Hero;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class MixReactiveBlockingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Hero.class))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion()) // this triggers Agroal
            ));

    @Inject
    EntityManager entityManager;

    @Inject
    Mutiny.SessionFactory reactiveSessionFactory;

    @Test
    @RunOnVertxContext
    @Disabled("WIP")
    public void avoidMixingTransactionalAndWithTransactionTest(UniAsserter asserter) {
        avoidMixingBlockingEMWithReactiveSessionFactory();
        // assert with Vertx
    }

    @Transactional
    public Uni<Object> avoidMixingBlockingEMWithReactiveSessionFactory() {
        Hero heroBlocking = new Hero("heroName");
        Uni<Object> uni = Uni.createFrom()
                .item(() -> {
                    // We're nesting @Transactional calls but we cannot reuse the same transaction
                    // As they're of two different kinds
                    // We're not sure this is testable, it might be in the blocking code we don't have the
                    // State of the context we need to check this
                    return blockingOp(heroBlocking);
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

        return uni;
    }

    @Transactional
    public @Nullable Object blockingOp(Hero heroBlocking) {
        entityManager.persist(heroBlocking);
        return null;
    }

    @Test
    @Disabled("WIP")
    public void avoidMixingTransactionalAndWithTransactionTest() throws Throwable {
        Void v = fromBlockingToReactive();

        // Assert blocking
    }

    // https://quarkus.io/guides/vertx#executing-asynchronous-code-from-a-blocking-thread
    @Transactional
    public Void fromBlockingToReactive() throws Throwable {
        Hero heroBlocking = new Hero("heroName");
        entityManager.persist(heroBlocking);

        Hero heroReactive = new Hero("heroName");

        return VertxContextSupport.subscribeAndAwait(() -> {
            return persistReactive(heroReactive);
        });

    }

    @Transactional
    public Uni<Void> persistReactive(Hero heroReactive) {
        return reactiveSessionFactory.withSession(session -> session.persist(heroReactive));
    }
}
