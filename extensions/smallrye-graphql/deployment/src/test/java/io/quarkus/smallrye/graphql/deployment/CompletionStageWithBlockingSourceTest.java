package io.quarkus.smallrye.graphql.deployment;

import static io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest.getPropertyAsString;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Reproducer for https://github.com/quarkusio/quarkus/issues/44762
 *
 * When a {@code @Query} returns {@link CompletionStage} via
 * {@link CompletableFuture#supplyAsync}, the future completes on
 * {@code ForkJoinPool.commonPool} where {@code Vertx.currentContext()}
 * is null. Blocking {@code @Source} field resolvers that run as
 * downstream continuations then NPE in
 * {@code BlockingHelper.runBlocking} because the Vert.x context is null.
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

    @GraphQLApi
    public static class PersonGraphQLApi {

        @Query
        public CompletionStage<Person> getPerson(long id) {
            return supplyAsync(() -> Person.ALL.get((int) id));
        }

        @Query
        public CompletionStage<List<Person>> getPeople() {
            return supplyAsync(() -> new ArrayList<>(Person.ALL));
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
