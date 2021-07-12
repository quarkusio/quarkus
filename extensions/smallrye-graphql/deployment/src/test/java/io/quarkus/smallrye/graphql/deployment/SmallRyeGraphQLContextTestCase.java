package io.quarkus.smallrye.graphql.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.graphql.api.Context;

public class SmallRyeGraphQLContextTestCase extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Dummy.class, ContextPropagationResource.class));

    @Test
    public void testAsyncQuery() {
        String request = getPayload("{\n" +
                "  testAsyncQuery {\n" +
                "    stringField\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .log()
                .body(true)
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.testAsyncQuery.stringField", Matchers.equalTo("OK"));
    }

    @Test
    public void testAsyncQueryWithAsyncSource() {
        String request = getPayload("{\n" +
                "  testAsyncQuery {\n" +
                "    stringField\n" +
                "    stringBatchSourceAsync\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .log()
                .body(true)
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.testAsyncQuery.stringField", Matchers.equalTo("OK"))
                .body("data.testAsyncQuery.stringBatchSourceAsync", Matchers.equalTo("hello"));
    }

    @Test
    public void testManualPropagation() {
        String request = getPayload("{\n" +
                "  testManualPropagation {\n" +
                "    stringField\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .log()
                .body(true)
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.testManualPropagation.stringField", Matchers.equalTo("OK"));
    }

    @Test
    public void testSourceMethods() {
        String request = getPayload("{\n" +
                "  testSourceMethods {\n" +
                "    stringBatchSource,\n" +
                "    numberBatchSource\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .log()
                .body(true)
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.testSourceMethods.stringBatchSource", Matchers.equalTo("hello"));
    }

    @GraphQLApi
    @ApplicationScoped
    public static class ContextPropagationResource {

        @Inject
        Instance<Context> context;

        @Inject
        ManagedExecutor managedExecutor;

        @Query
        public CompletableFuture<Dummy> testAsyncQuery() {
            final String executionId = context.get().getExecutionId();
            assertNotNull(executionId);
            this.executionId = context.get().getExecutionId();
            assertEquals("testAsyncQuery", context.get().getFieldName());
            Dummy result = new Dummy();
            result.setStringField("OK");
            return CompletableFuture.completedFuture(result);
        }

        @Query
        public Dummy testManualPropagation() throws ExecutionException, InterruptedException {
            final String executionId = context.get().getExecutionId();
            assertNotNull(executionId);

            CompletableFuture<Integer> numberFuture = CompletableFuture.supplyAsync(() -> {
                assertNotNull(context);
                assertEquals(executionId, context.get().getExecutionId());
                assertEquals("testManualPropagation", context.get().getFieldName());
                return 42;
            }, managedExecutor);

            CompletableFuture<String> stringFuture = CompletableFuture.supplyAsync(() -> {
                assertNotNull(context, "Context must to be available inside an async task");
                assertEquals(executionId, context.get().getExecutionId(), "Execution ID must be the same inside an async task");
                assertEquals("testManualPropagation", context.get().getFieldName());
                return "OK";
            }, managedExecutor);

            Dummy result = new Dummy();
            result.setNumberField(numberFuture.get());
            result.setStringField(stringFuture.get());
            return result;
        }

        private volatile String executionId;

        @Query
        public Dummy testSourceMethods() {
            this.executionId = context.get().getExecutionId();
            assertEquals("testSourceMethods", context.get().getFieldName());
            return new Dummy();
        }

        @Name("stringBatchSource")
        public List<String> stringBatchSource(@Source List<Dummy> source) {
            assertEquals("stringBatchSource", context.get().getFieldName());
            assertEquals(this.executionId, context.get().getExecutionId(), "Wrong execution ID propagated from the root query");

            List<String> result = new ArrayList<>();
            for (Dummy dummy : source) {
                result.add("hello");
            }
            return result;
        }

        @Name("stringBatchSourceAsync")
        public CompletionStage<List<String>> stringBatchSourceAsync(@Source List<Dummy> source) {
            assertEquals("stringBatchSourceAsync", context.get().getFieldName());
            assertEquals(this.executionId, context.get().getExecutionId(), "Wrong execution ID propagated from the root query");

            List<String> result = new ArrayList<>();
            for (Dummy dummy : source) {
                result.add("hello");
            }
            return CompletableFuture.completedFuture(result);
        }

        @Name("numberBatchSource")
        public List<Integer> numberBatchSource(@Source List<Dummy> source) {
            assertEquals("numberBatchSource", context.get().getFieldName());
            assertEquals(this.executionId, context.get().getExecutionId(), "Wrong execution ID propagated from the root query");

            List<Integer> result = new ArrayList<>();
            for (Dummy dummy : source) {
                result.add(123);
            }
            return result;
        }

    }

    public static class Dummy {

        private String stringField;

        private Integer numberField;

        private Boolean booleanField;

        public String getStringField() {
            return stringField;
        }

        public void setStringField(String stringField) {
            this.stringField = stringField;
        }

        public Integer getNumberField() {
            return numberField;
        }

        public void setNumberField(Integer numberField) {
            this.numberField = numberField;
        }

        public Boolean getBooleanField() {
            return booleanField;
        }

        public void setBooleanField(Boolean booleanField) {
            this.booleanField = booleanField;
        }
    }

}
