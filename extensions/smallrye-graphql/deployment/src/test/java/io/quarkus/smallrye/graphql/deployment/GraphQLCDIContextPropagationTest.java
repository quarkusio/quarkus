package io.quarkus.smallrye.graphql.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Testing scenarios which require CDI context propagation to work under the hood.
 */
public class GraphQLCDIContextPropagationTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestPojo.class, ResourceThatNeedsCdiContextPropagation.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    /**
     * Call a query which needs the CDI context to be propagated, because it is on a RequestScoped bean
     * and involves retrieving a batch source field (these are retrieved asynchronously).
     */
    @Test
    public void testCdiContextPropagationForBatchSources() {
        String pingRequest = getPayload("{\n" +
                "  pojos {\n" +
                "    duplicatedMessage\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(pingRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.pojos.duplicatedMessage", Matchers.contains("AA", "BB"));
    }

    /**
     * Same as above, but the batch source returns a list of CompletionStages.
     */
    @Test
    public void testCdiContextPropagationForBatchSourcesAsync() {
        String pingRequest = getPayload("{\n" +
                "  pojos {\n" +
                "    duplicatedMessageAsync\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(pingRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.pojos.duplicatedMessageAsync", Matchers.contains("AA", "BB"));
    }

    /**
     * In this case, there is an asynchronous query and the application launches a CompletableFuture,
     * therefore the application is responsible for propagating the context to that future.
     */
    @Test
    public void testManualContextPropagation() {
        String pingRequest = getPayload("{\n" +
                "  pojo_manual_propagation {\n" +
                "    message\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(pingRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.pojo_manual_propagation.message", Matchers.equalTo("A"));
    }

    @GraphQLApi
    @ApplicationScoped
    public static class ResourceThatNeedsCdiContextPropagation {

        /**
         * This is to make sure that `getPojos` and `duplicatedMessage` are called on the same class instance - they
         * need to share the same CDI request context rather than create a new one for calling `duplicatedMessage`.
         */
        private volatile Long injectedBeanId;

        @Inject
        InjectedBean injectedBean;

        @Inject
        ManagedExecutor executor;

        @Query("pojos")
        public List<TestPojo> getPojos() {
            this.injectedBeanId = injectedBean.getId();
            List<TestPojo> pojos = new ArrayList<>();
            pojos.add(new TestPojo("A"));
            pojos.add(new TestPojo("B"));
            return pojos;
        }

        @Query("pojo_manual_propagation")
        public CompletionStage<TestPojo> getPojoWithProgrammaticContextPropagation() {
            Long id = injectedBean.getId();
            return executor.supplyAsync(
                    () -> {
                        if (!injectedBean.getId().equals(id)) {
                            throw new IllegalStateException("The future was not executed in the correct request context");
                        }
                        return new TestPojo("A");
                    });
        }

        /**
         * This source field duplicates the message of a TestPojo (repeats it twice).
         */
        @Name("duplicatedMessage")
        public List<String> duplicatedMessage(@Source List<TestPojo> pojos) {
            if (!this.injectedBean.getId().equals(this.injectedBeanId)) {
                throw new IllegalStateException("duplicatedMessage must be executed in the same request context as getPojos");
            }
            return pojos.stream()
                    .map(pojo -> pojo.getMessage() + pojo.getMessage())
                    .collect(Collectors.toList());
        }

        /**
         * This source field duplicates the message of a TestPojo (repeats it twice) and does it asynchronously.
         */
        @Name("duplicatedMessageAsync")
        public CompletionStage<List<String>> duplicatedMessageAsync(@Source List<TestPojo> pojos) {
            if (!this.injectedBean.getId().equals(this.injectedBeanId)) {
                throw new IllegalStateException(
                        "duplicatedMessageAsync must be executed in the same request context as getPojos");
            }
            return CompletableFuture.completedFuture(pojos.stream()
                    .map(pojo -> pojo.getMessage() + pojo.getMessage())
                    .collect(Collectors.toList()));
        }

    }

    @RequestScoped
    public static class InjectedBean {

        private Long id = ThreadLocalRandom.current().nextLong();

        public Long getId() {
            return id;
        }
    }

}
