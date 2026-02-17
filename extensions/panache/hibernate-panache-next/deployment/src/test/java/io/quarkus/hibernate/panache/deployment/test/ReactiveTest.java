package io.quarkus.hibernate.panache.deployment.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class ReactiveTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(MyReactiveEntity.class, MyReactiveEntity_.class,
                            MyReactiveEntity_.ManagedReactiveQueries_.class));

    @WithTransaction
    Uni<Void> createOne() {
        return MyReactiveEntity_.managedReactive().count()
                .flatMap(count -> {
                    Assertions.assertEquals(0l, count);
                    MyReactiveEntity entity = new MyReactiveEntity();
                    entity.foo = "bar";
                    return entity.persist();
                }).flatMap(v -> MyReactiveEntity_.managedReactive().count())
                .onItem().invoke(count -> Assertions.assertEquals(1l, count))
                .replaceWithVoid();
    }

    @WithTransaction
    Uni<Void> modifyOne() {
        return MyReactiveEntity_.managedReactive().listAll()
                .onItem().invoke(list -> {
                    Assertions.assertEquals(1, list.size());
                    MyReactiveEntity entity = list.get(0);
                    Assertions.assertEquals("bar", entity.foo);
                    entity.foo = "gee";
                })
                .replaceWithVoid();
    }

    @WithTransaction
    Uni<Void> modifyOneCheck() {
        return MyReactiveEntity_.managedReactive().listAll()
                .flatMap(list -> {
                    Assertions.assertEquals(1, list.size());
                    MyReactiveEntity entity = list.get(0);
                    Assertions.assertEquals("gee", entity.foo);
                    return MyReactiveEntity_.managedReactive().count();
                })
                .replaceWithVoid();
    }

    @WithTransaction(stateless = true)
    Uni<Void> modifyOneStatelessNoUpdate() {
        return MyReactiveEntity_.statelessReactive().listAll()
                .onItem().invoke(list -> {
                    Assertions.assertEquals(1, list.size());
                    MyReactiveEntity entity = list.get(0);
                    Assertions.assertEquals("gee", entity.foo);
                    // should be ignored: not managed and no update called
                    entity.foo = "fu";
                })
                .replaceWithVoid();
    }

    @WithTransaction(stateless = true)
    Uni<Void> modifyOneStateless() {
        return MyReactiveEntity_.statelessReactive().listAll()
                .flatMap(list -> {
                    Assertions.assertEquals(1, list.size());
                    MyReactiveEntity entity = list.get(0);
                    // last update to fu should have been ignored
                    Assertions.assertEquals("gee", entity.foo);
                    entity.foo = "fu";
                    // not ignored this time
                    return entity.statelessReactive().update();
                })
                .replaceWithVoid();
    }

    @WithTransaction(stateless = true)
    Uni<Void> modifyOneStatelessCheck() {
        return MyReactiveEntity_.statelessReactive().listAll()
                .onItem().invoke(list -> {
                    Assertions.assertEquals(1, list.size());
                    MyReactiveEntity entity = list.get(0);
                    Assertions.assertEquals("fu", entity.foo);
                })
                .replaceWithVoid();
    }

    @WithTransaction(stateless = true)
    Uni<Void> upsertNew() {
        return MyReactiveEntity_.statelessReactive().count()
                .flatMap(count -> {
                    Assertions.assertEquals(0, count);

                    MyReactiveEntity entity = new MyReactiveEntity();
                    entity.foo = "bar";
                    entity.id = 1L;

                    return entity.statelessReactive().upsert();
                })
                .flatMap(v -> MyReactiveEntity_.statelessReactive().count())
                .map(count -> {
                    Assertions.assertEquals(1, count);
                    return null;
                });
    }

    @WithTransaction(stateless = true)
    Uni<Void> upsertExisting() {
        return MyReactiveEntity_.statelessReactive().listAll()
                .flatMap(list -> {
                    Assertions.assertEquals(1, list.size());

                    MyReactiveEntity entity = list.get(0);
                    Assertions.assertEquals("bar", entity.foo);
                    Assertions.assertEquals(1L, entity.id);
                    entity.foo = "fu";

                    return entity.statelessReactive().upsert();
                })
                .flatMap(v -> MyReactiveEntity_.statelessReactive().count())
                .map(count -> {
                    Assertions.assertEquals(1, count);
                    return null;
                });
    }

    @WithTransaction(stateless = true)
    Uni<Void> upsertCheck() {
        return MyReactiveEntity_.statelessReactive().listAll()
                .flatMap(list -> {
                    Assertions.assertEquals(1, list.size());

                    MyReactiveEntity entity = list.get(0);
                    Assertions.assertEquals("fu", entity.foo);

                    return entity.statelessReactive().upsert();
                })
                .flatMap(v -> MyReactiveEntity_.statelessReactive().count())
                .map(count -> {
                    Assertions.assertEquals(1, count);
                    return null;
                });
    }

    @WithTransaction
    Uni<Void> clear() {
        return MyReactiveEntity_.managedReactive().deleteAll().replaceWithVoid();
    }

    @WithTransaction
    Uni<Void> runQueries() {
        return MyReactiveEntity_.managedReactiveQueries().findFoos("fu")
                .flatMap(list -> {
                    Assertions.assertEquals(1, list.size());
                    return MyReactiveEntity_.managedReactiveQueries().findFoosHQL("fu");
                })
                .flatMap(list -> {
                    Assertions.assertEquals(1, list.size());
                    return MyReactiveEntity_.managedReactiveQueries().findFoosFind("fu");
                })
                .onItem().invoke(list -> {
                    Assertions.assertEquals(1, list.size());
                })
                .replaceWithVoid();
    }

    @RunOnVertxContext
    @Test
    void testRepositories(UniAsserter asserter) {
        asserter.execute(() -> clear());
        asserter.execute(() -> createOne());
        asserter.execute(() -> modifyOne());
        asserter.execute(() -> modifyOneCheck());
        asserter.execute(() -> modifyOneStatelessNoUpdate());
        asserter.execute(() -> modifyOneStateless());
        asserter.execute(() -> modifyOneStatelessCheck());
        asserter.execute(() -> clear());
        asserter.execute(() -> upsertNew());
        asserter.execute(() -> upsertExisting());
        asserter.execute(() -> upsertCheck());
        asserter.execute(() -> runQueries());
    }

}
