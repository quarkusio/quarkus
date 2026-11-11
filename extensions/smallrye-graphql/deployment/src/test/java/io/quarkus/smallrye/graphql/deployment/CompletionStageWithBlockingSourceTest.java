package io.quarkus.smallrye.graphql.deployment;

import static io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest.getPropertyAsString;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;

/**
 * Reproducer for https://github.com/quarkusio/quarkus/issues/44762
 *
 * When a {@code @Query} returns {@link CompletionStage} via
 * {@link CompletableFuture#supplyAsync}, the future completes on
 * {@code ForkJoinPool.commonPool} where {@code Vertx.currentContext()}
 * is null. Blocking {@code @Source} field resolvers that run as
 * downstream continuations then NPE in
 * {@code BlockingHelper.runBlocking} because the Vert.x context is null.
 * <p>
 * The same null context on an event-loop thread
 * (https://github.com/quarkusio/quarkus/issues/58283) must run that resolver on a worker.
 */
public class CompletionStageWithBlockingSourceTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PersonGraphQLApi.class, Person.class, Score.class)
                    .addAsResource(new StringAsset(getPropertyAsString()), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testAsyncQueryWithBlockingSource() {
        String request = getPayload("{\n" +
                "  person(id: 0) {\n" +
                "    name\n" +
                "    score {\n" +
                "      name\n" +
                "      value\n" +
                "    }\n" +
                "  }\n" +
                "}");

        RestAssured.given()
                .when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString("Alice"),
                        CoreMatchers.containsString("math"),
                        CoreMatchers.containsString("95"));
    }

    @Test
    public void testAsyncQueryWithBlockingBatchSource() {
        String request = getPayload("{\n" +
                "  people {\n" +
                "    name\n" +
                "    score {\n" +
                "      name\n" +
                "      value\n" +
                "    }\n" +
                "  }\n" +
                "}");

        RestAssured.given()
                .when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString("Alice"),
                        CoreMatchers.containsString("Bob"),
                        CoreMatchers.containsString("math"),
                        CoreMatchers.containsString("95"),
                        CoreMatchers.containsString("history"),
                        CoreMatchers.containsString("88"));
    }

    @Test
    public void blockingSourceOnEventLoopWithoutContextRunsOnWorker() {
        String request = getPayload("{ personFromEventLoop { callback thread blockingUniThread } }");

        RestAssured.given()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("data.personFromEventLoop.callback", CoreMatchers.equalTo("true:true"))
                .body("data.personFromEventLoop.thread", CoreMatchers.startsWith("executor-thread-"))
                .body("data.personFromEventLoop.blockingUniThread", CoreMatchers.startsWith("executor-thread-"));
    }

    @GraphQLApi
    public static class PersonGraphQLApi {

        @Inject
        Vertx vertx;

        @Query
        public CompletionStage<Person> getPerson(long id) {
            return supplyAsync(() -> Person.ALL.get((int) id));
        }

        @Query
        public CompletionStage<List<Person>> getPeople() {
            return supplyAsync(() -> new ArrayList<>(Person.ALL));
        }

        @Query
        public Uni<Person> personFromEventLoop() {
            Person person = new Person("Carol");
            // Subscribe on a worker, then complete from a raw Netty task so the callback runs on an
            // event-loop thread with no Vert.x context. execute() from that event loop would run inline.
            return Uni.createFrom().item(person)
                    .emitOn(Infrastructure.getDefaultWorkerPool())
                    .flatMap(ignored -> Uni.createFrom().emitter(em -> ((VertxInternal) vertx).nettyEventLoopGroup().next()
                            .execute(() -> {
                                person.callback = Context.isOnEventLoopThread() + ":" + (Vertx.currentContext() == null);
                                em.complete(person);
                            })));
        }

        // Completes the future on a non-Vert.x thread (ForkJoinPool) with a
        // delay so graphql-java attaches its continuations first — the source
        // field resolvers then run where Vertx.currentContext() is null.
        // The TCCL is propagated to avoid classloading issues in the test env.
        private static <T> CompletionStage<T> supplyAsync(java.util.function.Supplier<T> supplier) {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            return CompletableFuture.supplyAsync(() -> {
                Thread.currentThread().setContextClassLoader(tccl);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return supplier.get();
            });
        }

        public String thread(@Source Person person) {
            return Thread.currentThread().getName();
        }

        @Name("blockingUniThread")
        @Blocking
        public Uni<String> blockingUniThread(@Source Person person) {
            return Uni.createFrom().item(() -> Thread.currentThread().getName());
        }

        public Score getScore(@Source Person person) {
            return Score.forPerson(person.name);
        }

        public List<Score> getScore(@Source List<Person> people) {
            List<Score> scores = new ArrayList<>();
            for (Person person : people) {
                scores.add(Score.forPerson(person.name));
            }
            return scores;
        }
    }

    public static class Person {
        static final List<Person> ALL = List.of(
                new Person("Alice"),
                new Person("Bob"));

        public String name;
        public String callback;

        public Person() {
        }

        public Person(String name) {
            this.name = name;
        }
    }

    public static class Score {
        public String name;
        public int value;

        public Score() {
        }

        public Score(String name, int value) {
            this.name = name;
            this.value = value;
        }

        static Score forPerson(String personName) {
            return switch (personName) {
                case "Alice" -> new Score("math", 95);
                case "Bob" -> new Score("history", 88);
                default -> new Score("unknown", 0);
            };
        }
    }
}
