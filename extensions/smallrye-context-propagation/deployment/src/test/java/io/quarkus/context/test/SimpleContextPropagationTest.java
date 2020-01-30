package io.quarkus.context.test;

import static org.hamcrest.Matchers.equalTo;

import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Tests simple context propagation of basic contexts (Arc, RestEasy, server, transaction) via either
 * {@link org.eclipse.microprofile.context.ManagedExecutor} (ME) or
 * {@link org.eclipse.microprofile.context.ThreadContext} (TC).
 */
public class SimpleContextPropagationTest {
    private static Class[] testClasses = {
            ContextEndpoint.class, RequestBean.class, ContextEntity.class, TestResources.class, CompletionExceptionMapper.class,
            TransactionalBean.class
    };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addAsResource("application.properties"));

    @Test()
    public void testRESTEasyMEContextPropagation() {
        RestAssured.when().get("/context/resteasy").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testRESTEasyTCContextPropagation() {
        RestAssured.when().get("/context/resteasy-tc").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testServletMEContextPropagation() {
        RestAssured.when().get("/context/servlet").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testServletTCContextPropagation() {
        RestAssured.when().get("/context/servlet-tc").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testArcMEContextPropagation() {
        RestAssured.when().get("/context/arc").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testArcTCContextPropagation() {
        RestAssured.when().get("/context/arc-tc").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testArcMEContextPropagationDisabled() {
        RestAssured.when().get("/context/noarc").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testArcTCContextPropagationDisabled() {
        RestAssured.when().get("/context/noarc-tc").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testTransactionMEContextPropagation() {
        RestAssured.when().get("/context/transaction").then()
                .statusCode(Response.Status.OK.getStatusCode());
        RestAssured.when().get("/context/transaction2").then()
                .statusCode(Response.Status.CONFLICT.getStatusCode());
        RestAssured.when().get("/context/transaction3").then()
                .statusCode(Response.Status.CONFLICT.getStatusCode());
        RestAssured.when().get("/context/transaction4").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testTransactionTCContextPropagation() {
        RestAssured.when().get("/context/transaction-tc").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testTransactionNewContextPropagation() {
        RestAssured.when().get("/context/transaction-new").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testTransactionContextPropagationSingle() {
        RestAssured.when().get("/context/transaction-single").then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(equalTo("OK"));
        RestAssured.when().get("/context/transaction-single2").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test()
    public void testTransactionContextPropagationPublisher() {
        RestAssured.when().get("/context/transaction-publisher").then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(equalTo("OK"));
        RestAssured.when().get("/context/transaction-publisher2").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }
}
