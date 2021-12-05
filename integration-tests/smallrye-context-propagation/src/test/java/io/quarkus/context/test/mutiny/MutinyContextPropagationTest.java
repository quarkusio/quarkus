package io.quarkus.context.test.mutiny;

import static org.hamcrest.Matchers.equalTo;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.context.test.CompletionExceptionMapper;
import io.quarkus.context.test.RequestBean;
import io.quarkus.context.test.TestResources;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Tests Mutiny context propagation of basic contexts (Arc, RestEasy, server, transaction) via either
 * {@link org.eclipse.microprofile.context.ManagedExecutor} (ME) or
 * {@link org.eclipse.microprofile.context.ThreadContext} (TC).
 */
public class MutinyContextPropagationTest {
    private static Class[] testClasses = {
            MutinyContextEndpoint.class, RequestBean.class, SomeEntity.class, SomeOtherEntity.class, Person.class,
            TestResources.class,
            CompletionExceptionMapper.class,
            MutinyTransactionalBean.class
    };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application.properties"));

    @Test
    public void testRESTEasyContextPropagation() {
        RestAssured.when().get("/mutiny-context/resteasy-uni").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testRESTEasyMEContextPropagationWithUniCreatedFromCS() {
        RestAssured.when().get("/mutiny-context/resteasy-uni-cs").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testRESTEasyTCContextPropagationWithUniCreatedFromCS() {
        RestAssured.when().get("/mutiny-context/resteasy-tc-uni-cs").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testServletContextPropagation() {
        RestAssured
                .given()
                .header("Content-Type", "acme/acme")
                .when()
                .get("/mutiny-context/servlet-uni")
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testServletMEContextPropagationWithUniCreatedFromCS() {
        RestAssured
                .given()
                .header("Content-Type", "acme/acme")
                .when()
                .get("/mutiny-context/servlet-uni-cs")
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testServletTCContextPropagationWithUniCreatedFromCS() {
        RestAssured
                .given()
                .header("Content-Type", "acme/acme")
                .when()
                .get("/mutiny-context/servlet-tc-uni-cs")
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testArcContextPropagationWithUni() {
        RestAssured.when().get("/mutiny-context/arc-uni").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testArcContextPropagationWithUniCreatedFromCSAndME() {
        RestAssured.when().get("/mutiny-context/arc-uni-cs").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testArcTCContextPropagation() {
        RestAssured.when().get("/mutiny-context/arc-tc-uni").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testTransactionPropagationWithUni() {
        RestAssured.when().get("/mutiny-context/transaction-uni").then()
                .statusCode(Response.Status.OK.getStatusCode());
        awaitState(() -> RestAssured.when().get("/mutiny-context/transaction2-uni").then()
                .statusCode(Response.Status.CONFLICT.getStatusCode()));
        awaitState(() -> RestAssured.when().get("/mutiny-context/transaction3-uni").then()
                .statusCode(Response.Status.CONFLICT.getStatusCode()));
        awaitState(() -> RestAssured.when().get("/mutiny-context/transaction4").then()
                .statusCode(Response.Status.OK.getStatusCode()));
    }

    @Test
    public void testTransactionPropagationWithUniCreatedFromCS() {
        RestAssured.when().get("/mutiny-context/transaction-uni-cs").then()
                .statusCode(Response.Status.OK.getStatusCode());
        awaitState(() -> RestAssured.when().get("/mutiny-context/transaction2-uni-cs").then()
                .statusCode(Response.Status.CONFLICT.getStatusCode()));
        awaitState(() -> RestAssured.when().get("/mutiny-context/transaction3-uni-cs").then()
                .statusCode(Response.Status.CONFLICT.getStatusCode()));
        awaitState(() -> RestAssured.when().get("/mutiny-context/transaction4-cs").then()
                .statusCode(Response.Status.OK.getStatusCode()));
    }

    @Test
    public void testTransactionTCContextPropagation() {
        RestAssured.when().get("/mutiny-context/transaction-tc-uni").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testTransactionNewContextPropagationSync() {
        RestAssured.when().get("/mutiny-context/transaction-new-sync").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testTransactionContextPropagationAsyncUni() {
        RestAssured.when().get("/mutiny-context/transaction-new-uni").then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(equalTo("OK"));
        awaitState(() -> RestAssured.when().get("/mutiny-context/transaction-uni-2").then()
                .statusCode(Response.Status.OK.getStatusCode()));
    }

    @Test
    public void testTransactionContextPropagationMulti() {
        RestAssured.when().get("/mutiny-context/transaction-multi").then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(equalTo("OK"));
        awaitState(() -> RestAssured.when().get("/mutiny-context/transaction-multi-2").then()
                .statusCode(Response.Status.OK.getStatusCode()));
    }

    private void awaitState(ThrowingRunnable task) {
        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(task);
    }

}
