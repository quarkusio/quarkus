package io.quarkus.data.hibernate.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class ReactiveAttributeMetamodelTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(MyReactiveEntity.class, MyReactiveEntity_.class, _MyReactiveEntity.class,
                            _MyReactiveEntity._ManagedReactiveQueries.class));

    @WithTransaction
    Uni<Void> setup() {
        return MyReactiveEntity_.managedReactive().deleteAll()
                .flatMap(v -> {
                    MyReactiveEntity entity1 = new MyReactiveEntity();
                    entity1.foo = "alpha";
                    entity1.bar = "one";
                    return entity1.persistAndFlush();
                })
                .flatMap(v -> {
                    MyReactiveEntity entity2 = new MyReactiveEntity();
                    entity2.foo = "beta";
                    entity2.bar = "two";
                    return entity2.persistAndFlush();
                })
                .flatMap(v -> {
                    MyReactiveEntity entity3 = new MyReactiveEntity();
                    entity3.foo = "alpha";
                    entity3.bar = "three";
                    return entity3.persistAndFlush();
                })
                .replaceWithVoid();
    }

    @WithTransaction
    Uni<Void> testFindWithJakartaDataAttribute() {
        return MyReactiveEntity_.managedReactive()
                .find(_MyReactiveEntity.foo, "alpha")
                .list()
                .onItem().invoke(results -> {
                    assertThat(results).hasSize(2);
                    assertThat(results).extracting(e -> e.foo).containsOnly("alpha");
                })
                .replaceWithVoid();
    }

    @WithTransaction
    Uni<Void> testFindWithJpaAttributeStillWorks() {
        return MyReactiveEntity_.managedReactive()
                .find(MyReactiveEntity_.FOO, "alpha")
                .list()
                .onItem().invoke(results -> {
                    assertThat(results).hasSize(2);
                    assertThat(results).extracting(e -> e.foo).containsOnly("alpha");
                })
                .replaceWithVoid();
    }

    @WithTransaction
    Uni<Void> testListWithJakartaDataAttribute() {
        return MyReactiveEntity_.managedReactive()
                .list(_MyReactiveEntity.foo, "beta")
                .onItem().invoke(results -> {
                    assertThat(results).hasSize(1);
                    assertThat(results.get(0).foo).isEqualTo("beta");
                    assertThat(results.get(0).bar).isEqualTo("two");
                })
                .replaceWithVoid();
    }

    @WithTransaction
    Uni<Void> testListWithJpaAttributeStillWorks() {
        return MyReactiveEntity_.managedReactive()
                .list(MyReactiveEntity_.FOO, "beta")
                .onItem().invoke(results -> {
                    assertThat(results).hasSize(1);
                    assertThat(results.get(0).foo).isEqualTo("beta");
                })
                .replaceWithVoid();
    }

    @WithTransaction
    Uni<Void> testCountWithJakartaDataAttribute() {
        return MyReactiveEntity_.managedReactive().count(_MyReactiveEntity.foo, "alpha")
                .onItem().invoke(count -> assertThat(count).isEqualTo(2L))
                .replaceWithVoid();
    }

    @WithTransaction
    Uni<Void> testCountWithJpaAttributeStillWorks() {
        return MyReactiveEntity_.managedReactive().count(MyReactiveEntity_.FOO, "alpha")
                .onItem().invoke(count -> assertThat(count).isEqualTo(2L))
                .replaceWithVoid();
    }

    @WithTransaction
    Uni<Void> testCountNoResults() {
        return MyReactiveEntity_.managedReactive().count(_MyReactiveEntity.foo, "nonexistent")
                .onItem().invoke(count -> assertThat(count).isEqualTo(0L))
                .replaceWithVoid();
    }

    @WithTransaction
    Uni<Void> testDeleteWithJakartaDataAttribute() {
        return MyReactiveEntity_.managedReactive().delete(_MyReactiveEntity.foo, "beta")
                .onItem().invoke(deleted -> assertThat(deleted).isEqualTo(1L))
                .flatMap(v -> MyReactiveEntity_.managedReactive().count())
                .onItem().invoke(count -> assertThat(count).isEqualTo(2L))
                .replaceWithVoid();
    }

    @WithTransaction
    Uni<Void> testDeleteWithJpaAttributeStillWorks() {
        return MyReactiveEntity_.managedReactive().delete(MyReactiveEntity_.FOO, "alpha")
                .onItem().invoke(deleted -> assertThat(deleted).isEqualTo(2L))
                .flatMap(v -> MyReactiveEntity_.managedReactive().count())
                .onItem().invoke(count -> assertThat(count).isEqualTo(1L))
                .replaceWithVoid();
    }

    @WithTransaction
    Uni<Void> testDeleteNoMatches() {
        return MyReactiveEntity_.managedReactive().delete(_MyReactiveEntity.foo, "nonexistent")
                .onItem().invoke(deleted -> assertThat(deleted).isEqualTo(0L))
                .replaceWithVoid();
    }

    @WithTransaction
    Uni<Void> cleanup() {
        return MyReactiveEntity_.managedReactive().deleteAll().replaceWithVoid();
    }

    @RunOnVertxContext
    @Test
    void testReactiveJakartaDataMetamodelSupport(UniAsserter asserter) {
        asserter.execute(() -> setup());
        asserter.execute(() -> testFindWithJakartaDataAttribute());
        asserter.execute(() -> testFindWithJpaAttributeStillWorks());
        asserter.execute(() -> testListWithJakartaDataAttribute());
        asserter.execute(() -> testListWithJpaAttributeStillWorks());
        asserter.execute(() -> testCountWithJakartaDataAttribute());
        asserter.execute(() -> testCountWithJpaAttributeStillWorks());
        asserter.execute(() -> testCountNoResults());
        asserter.execute(() -> testDeleteWithJakartaDataAttribute());
        asserter.execute(() -> cleanup());

        asserter.execute(() -> setup());
        asserter.execute(() -> testDeleteWithJpaAttributeStillWorks());
        asserter.execute(() -> cleanup());

        asserter.execute(() -> setup());
        asserter.execute(() -> testDeleteNoMatches());
        asserter.execute(() -> cleanup());
    }
}
