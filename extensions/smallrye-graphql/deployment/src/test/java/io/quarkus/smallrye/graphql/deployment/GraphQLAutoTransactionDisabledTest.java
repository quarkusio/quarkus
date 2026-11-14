package io.quarkus.smallrye.graphql.deployment;

import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class GraphQLAutoTransactionDisabledTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClasses(TransactionCheckApi.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-graphql.auto-transactional=false"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    void blockingQueryShouldNotHaveTransaction() {
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
                .body("data.blockingQuery", Matchers.is(false));
    }

    @GraphQLApi
    public static class TransactionCheckApi {

        @Inject
        TransactionManager tm;

        @Query
        public boolean blockingQuery() throws Exception {
            return tm.getStatus() == Status.STATUS_ACTIVE;
        }
    }
}
