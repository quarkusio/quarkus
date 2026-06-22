package io.quarkus.smallrye.graphql.deployment;

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

        @Inject
        TransactionManager tm;

        @Query
        public boolean blockingQuery() {
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

        public boolean inTransaction(@Source Greeting greeting) {
            return isTransactionActive();
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
