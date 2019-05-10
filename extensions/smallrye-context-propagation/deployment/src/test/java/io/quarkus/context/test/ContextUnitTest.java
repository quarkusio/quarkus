package io.quarkus.context.test;

import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ContextUnitTest {
    private static Class[] testClasses = {
            ContextEndpoint.class, RequestBean.class, ContextEntity.class, TestResources.class, CompletionExceptionMapper.class, TransactionalBean.class
    };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addAsResource("application.properties"));

    @Test()
    public void testRESTEasyContextPropagation() {
        RestAssured.when().get("/context/resteasy").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testThreadContextPropagation() {
        RestAssured.when().get("/context/thread-context").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testArcContextPropagation() {
        RestAssured.when().get("/context/arc").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testArcContextPropagationDisabled() {
        RestAssured.when().get("/context/noarc").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testTransactionContextPropagation() {
        RestAssured.when().get("/context/transaction").then()
                .statusCode(Response.Status.OK.getStatusCode());
        RestAssured.when().get("/context/transaction2").then()
                .statusCode(Response.Status.CONFLICT.getStatusCode());
        RestAssured.when().get("/context/transaction3").then()
                .statusCode(Response.Status.CONFLICT.getStatusCode());
        RestAssured.when().get("/context/transaction4").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testTransactionNewContextPropagation() {
        RestAssured.when().get("/context/transaction-new").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }
}
