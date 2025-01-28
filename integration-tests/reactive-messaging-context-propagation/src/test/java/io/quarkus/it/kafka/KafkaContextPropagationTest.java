package io.quarkus.it.kafka;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.KafkaCompanionResource;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
public class KafkaContextPropagationTest {

    @Test
    void testContextPropagation() {
        given().body("rose").post("/flowers/contextual").then().statusCode(204);
    }

    @Test
    void testContextPropagationUni() {
        given().body("rose").post("/flowers/contextual/uni").then().statusCode(204);
    }

    @Test
    void testContextPropagationBlocking() {
        given().body("rose").post("/flowers/contextual/blocking").then().statusCode(204);
    }

    @Test
    void testContextPropagationBlockingUni() {
        given().body("rose").post("/flowers/contextual/uni/blocking").then().statusCode(204);
    }

    @Test
    void testContextPropagationBlockingNamed() {
        given().body("rose").post("/flowers/contextual/blocking-named").then().statusCode(204);
    }

    @Test
    void testContextPropagationBlockingNamedUni() {
        given().body("rose").post("/flowers/contextual/uni/blocking-named").then().statusCode(204);
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void testContextPropagationVirtualThread() {
        given().body("rose").post("/flowers/contextual/virtual-thread").then().statusCode(204);
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void testContextPropagationVirtualThreadUni() {
        given().body("rose").post("/flowers/contextual/uni/virtual-thread").then().statusCode(204);
    }

    @Test
    void testAbsenceOfContextPropagation() {
        given().body("rose").post("/flowers").then()
                .statusCode(500)
                .body(assertBodyRequestScopedContextWasNotActive());
    }

    @Test
    void testAbsenceOfContextPropagationUni() {
        given().body("rose").post("/flowers/uni").then()
                .statusCode(500)
                .body(assertBodyRequestScopedContextWasNotActive());
    }

    @Test
    void testAbsenceOfContextPropagationBlocking() {
        given().body("rose").post("/flowers/blocking").then()
                .statusCode(500)
                .body(assertBodyRequestScopedContextWasNotActive());
    }

    @Test
    void testAbsenceOfContextPropagationBlockingUni() {
        given().body("rose").post("/flowers/uni/blocking").then()
                .statusCode(500)
                .body(assertBodyRequestScopedContextWasNotActive());
    }

    @Test
    void testAbsenceOfContextPropagationBlockingNamed() {
        given().body("rose").post("/flowers/blocking-named").then()
                .statusCode(500)
                .body(assertBodyRequestScopedContextWasNotActive());
    }

    @Test
    void testAbsenceOfContextPropagationBlockingNamedUni() {
        given().body("rose").post("/flowers/uni/blocking-named").then()
                .statusCode(500)
                .body(assertBodyRequestScopedContextWasNotActive());
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void testAbsenceOfContextPropagationVirtualThread() {
        given().body("rose").post("/flowers/virtual-thread").then()
                .statusCode(500)
                .body(assertBodyRequestScopedContextWasNotActive());
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void testAbsenceOfContextPropagationVirtualThreadUni() {
        given().body("rose").post("/flowers/uni/virtual-thread").then()
                .statusCode(500)
                .body(assertBodyRequestScopedContextWasNotActive());
    }

    protected Matcher<String> assertBodyRequestScopedContextWasNotActive() {
        return containsString("RequestScoped context was not active");
    }

    @Test
    void testIncomingFromConnector() {
        given().body("rose").post("/flowers/produce").then()
                .statusCode(204);
        given().body("daisy").post("/flowers/produce").then()
                .statusCode(204);
        given().body("peony").post("/flowers/produce").then()
                .statusCode(204);

        await().pollDelay(5, TimeUnit.SECONDS).untilAsserted(() -> given().get("/flowers/received")
                .then().body(not(containsString("rose")),
                        not(containsString("daisy")),
                        not(containsString("peony"))));
    }

}
