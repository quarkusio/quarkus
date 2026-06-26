package io.quarkus.smallrye.graphql.deployment;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

public class GraphQLAutoTransactionTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClasses(TransactionCheckApi.class, Greeting.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-graphql.nonblocking.enabled=false"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    void blockingQueryShouldHaveTransaction() {
        String request = getPayload("{ blockingQuery }");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.blockingQuery", Matchers.is(true));
    }

    @Test
    void blockingMutationShouldHaveTransaction() {
        String request = getPayload("mutation { blockingMutation }");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.blockingMutation", Matchers.is(true));
    }

    @Test
    void reactiveQueryShouldNotHaveTransaction() {
        String request = getPayload("{ reactiveQuery }");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.reactiveQuery", Matchers.is(false));
    }

    @Test
    void nonBlockingQueryShouldNotHaveTransaction() {
        String request = getPayload("{ nonBlockingQuery }");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.nonBlockingQuery", Matchers.is(false));
    }

    @Test
    void explicitTransactionalShouldBePreserved() {
        String request = getPayload("{ explicitTransactional }");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.explicitTransactional", Matchers.is(false));
    }

    @Test
    void sourceResolverShouldHaveTransaction() {
        String request = getPayload("{ greeting { message inTransaction } }");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.greeting.inTransaction", Matchers.is(true));
    }

    @Test
    void concurrentSourceResolversShouldEachHaveTransaction() {
        TransactionCheckApi.sourceTransactionResults.clear();

        String request = getPayload("{ greetings { message inTransaction } }");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.greetings[0].inTransaction", Matchers.is(true))
                .body("data.greetings[1].inTransaction", Matchers.is(true))
                .body("data.greetings[2].inTransaction", Matchers.is(true));

        org.junit.jupiter.api.Assertions.assertFalse(TransactionCheckApi.sourceTransactionResults.isEmpty(),
                "Source resolvers should have recorded transaction results");
        org.junit.jupiter.api.Assertions.assertTrue(
                TransactionCheckApi.sourceTransactionResults.stream().allMatch(b -> b),
                "All concurrent source resolvers should have had an active transaction");
    }

    @Test
    void multipleBlockingFieldsShouldEachHaveTransaction() {
        String request = getPayload("{ blockingQuery blockingQuery2 }");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.blockingQuery", Matchers.is(true))
                .body("data.blockingQuery2", Matchers.is(true));
    }

    public static class Greeting {
        private String message;

        public Greeting() {
        }

        public Greeting(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @GraphQLApi
    public static class TransactionCheckApi {

        static final Set<Boolean> sourceTransactionResults = ConcurrentHashMap.newKeySet();

        @Inject
        TransactionManager tm;

        @Query
        public boolean blockingQuery() {
            return isTransactionActive();
        }

        @Query
        public boolean blockingQuery2() {
            return isTransactionActive();
        }

        @Mutation
        public boolean blockingMutation() {
            return isTransactionActive();
        }

        @Query
        public Uni<Boolean> reactiveQuery() {
            return Uni.createFrom().item(isTransactionActive());
        }

        @NonBlocking
        @Query
        public boolean nonBlockingQuery() {
            return isTransactionActive();
        }

        @jakarta.transaction.Transactional(jakarta.transaction.Transactional.TxType.NOT_SUPPORTED)
        @Query
        public boolean explicitTransactional() {
            return isTransactionActive();
        }

        @Query
        public Greeting greeting() {
            return new Greeting("hello");
        }

        @Query
        public List<Greeting> greetings() {
            return List.of(new Greeting("hello"), new Greeting("world"), new Greeting("test"));
        }

        public boolean inTransaction(@Source Greeting greeting) {
            boolean active = isTransactionActive();
            sourceTransactionResults.add(active);
            return active;
        }

        private boolean isTransactionActive() {
            try {
                return tm.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
