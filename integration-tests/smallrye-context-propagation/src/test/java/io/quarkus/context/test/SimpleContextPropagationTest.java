package io.quarkus.context.test;

import static org.hamcrest.Matchers.equalTo;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Tests simple context propagation of basic contexts (Arc, RestEasy, server, transaction) via either
 * {@link org.eclipse.microprofile.context.ManagedExecutor} (ME) or
 * {@link org.eclipse.microprofile.context.ThreadContext} (TC).
 *
 * Note that the transaction does not commit until after the response is on the wire,
 * this introduces an unlikely race condition where a subsequent request may not see the
 * committed tx.
 *
 * To work around this we are using awaitability
 */
public class SimpleContextPropagationTest {
    private static Class[] testClasses = {
            ContextEndpoint.class, RequestBean.class, ContextEntity.class, TestResources.class, CompletionExceptionMapper.class,
            TransactionalBean.class
    };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
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
        awaitState(() -> RestAssured.when().get("/context/transaction2").then()
                .statusCode(Response.Status.CONFLICT.getStatusCode()));
        awaitState(() -> RestAssured.when().get("/context/transaction3").then()
                .statusCode(Response.Status.CONFLICT.getStatusCode()));
        awaitState(() -> RestAssured.when().get("/context/transaction4").then()
                .statusCode(Response.Status.OK.getStatusCode()));
    }

    private void awaitState(ThrowingRunnable task) {
        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(1000, TimeUnit.MILLISECONDS)
                .untilAsserted(task);
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
        awaitState(() -> RestAssured.when().get("/context/transaction-single2").then()
                .statusCode(Response.Status.OK.getStatusCode()));
    }

    @Test()
    public void testTransactionContextPropagationPublisher() {
        RestAssured.when().get("/context/transaction-publisher").then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(equalTo("OK"));
        awaitState(() -> RestAssured.when().get("/context/transaction-publisher2").then()
                .statusCode(Response.Status.OK.getStatusCode()));
    }
}
