package io.quarkus.it.kafka;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.KafkaCompanionResource;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
public class KafkaContextPropagationTest {

    @Nested
    // FlowerResource
    class ContextNotPropagated {
        @Test
        void testNonBlocking() {
            given().body("rose").post("/flowers").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }

        @Test
        void testNonBlockingUni() {
            given().body("rose").post("/flowers/uni").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/uni").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/uni").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }

        @Test
        void testBlocking() {
            given().body("rose").post("/flowers/blocking").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/blocking").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/blocking").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }

        @Test
        void testBlockingUni() {
            given().body("rose").post("/flowers/uni/blocking").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/uni/blocking").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/uni/blocking").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }

        @Test
        void testBlockingNamed() {
            given().body("rose").post("/flowers/blocking-named").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/blocking-named").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/blocking-named").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }

        @Test
        void testBlockingNamedUni() {
            given().body("rose").post("/flowers/uni/blocking-named").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/uni/blocking-named").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/uni/blocking-named").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }

        @Test
        @EnabledForJreRange(min = JRE.JAVA_21)
        void testVirtualThread() {
            given().body("rose").post("/flowers/virtual-thread").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/virtual-thread").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/virtual-thread").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }

        @Test
        @EnabledForJreRange(min = JRE.JAVA_21)
        void testVirtualThreadUni() {
            given().body("rose").post("/flowers/uni/virtual-thread").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/uni/virtual-thread").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/uni/virtual-thread").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }
    }

    @Nested
    // FlowerContextualResource
    class ContextPropagated {
        @Test
        void testNonBlocking() {
            given().body("rose").post("/flowers/contextual").then().statusCode(204);
            given().body("peony").post("/flowers/contextual").then().statusCode(204);
            given().body("daisy").post("/flowers/contextual").then().statusCode(204);
        }

        @Test
        void testNonBlockingUni() {
            given().body("rose").post("/flowers/contextual/uni").then().statusCode(204);
            given().body("peony").post("/flowers/contextual/uni").then().statusCode(204);
            given().body("daisy").post("/flowers/contextual/uni").then().statusCode(204);
        }

        @Test
        void testBlocking() {
            given().body("rose").post("/flowers/contextual/blocking").then().statusCode(204);
            given().body("peony").post("/flowers/contextual/blocking").then().statusCode(204);
            given().body("daisy").post("/flowers/contextual/blocking").then().statusCode(204);
        }

        @Test
        void testBlockingUni() {
            given().body("rose").post("/flowers/contextual/uni/blocking").then().statusCode(204);
            given().body("peony").post("/flowers/contextual/uni/blocking").then().statusCode(204);
            given().body("daisy").post("/flowers/contextual/uni/blocking").then().statusCode(204);
        }

        @Test
        void testBlockingNamed() {
            given().body("rose").post("/flowers/contextual/blocking-named").then().statusCode(204);
            given().body("peony").post("/flowers/contextual/blocking-named").then().statusCode(204);
            given().body("daisy").post("/flowers/contextual/blocking-named").then().statusCode(204);
        }

        @Test
        void testBlockingNamedUni() {
            given().body("rose").post("/flowers/contextual/uni/blocking-named").then().statusCode(204);
            given().body("peony").post("/flowers/contextual/uni/blocking-named").then().statusCode(204);
            given().body("daisy").post("/flowers/contextual/uni/blocking-named").then().statusCode(204);
        }

        @Test
        @EnabledForJreRange(min = JRE.JAVA_21)
        void testVirtualThread() {
            given().body("rose").post("/flowers/contextual/virtual-thread").then().statusCode(204);
            given().body("peony").post("/flowers/contextual/virtual-thread").then().statusCode(204);
            given().body("daisy").post("/flowers/contextual/virtual-thread").then().statusCode(204);
        }

        @Test
        @EnabledForJreRange(min = JRE.JAVA_21)
        void testVirtualThreadUni() {
            given().body("rose").post("/flowers/contextual/uni/virtual-thread").then().statusCode(204);
            given().body("peony").post("/flowers/contextual/uni/virtual-thread").then().statusCode(204);
            given().body("daisy").post("/flowers/contextual/uni/virtual-thread").then().statusCode(204);
        }
    }

    @Nested
    // FlowerMutinyResource
    class MutinyContextNotPropagated {
        @Test
        void testNonBlocking() {
            given().body("rose").post("/flowers/mutiny").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/mutiny").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/mutiny").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }

        @Test
        void testNonBlockingUni() {
            given().body("rose").post("/flowers/mutiny/uni").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/mutiny/uni").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/mutiny/uni").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }

        @Test
        void testBlocking() {
            given().body("rose").post("/flowers/mutiny/blocking").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/mutiny/blocking").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/mutiny/blocking").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }

        @Test
        void testBlockingUni() {
            given().body("rose").post("/flowers/mutiny/uni/blocking").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/mutiny/uni/blocking").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/mutiny/uni/blocking").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }

        @Test
        void testBlockingNamed() {
            given().body("rose").post("/flowers/mutiny/blocking-named").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/mutiny/blocking-named").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/mutiny/blocking-named").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }

        @Test
        void testBlockingNamedUni() {
            given().body("rose").post("/flowers/mutiny/uni/blocking-named").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/mutiny/uni/blocking-named").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/mutiny/uni/blocking-named").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }

        @Test
        @EnabledForJreRange(min = JRE.JAVA_21)
        void testVirtualThread() {
            given().body("rose").post("/flowers/mutiny/virtual-thread").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/mutiny/virtual-thread").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/mutiny/virtual-thread").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }

        @Test
        @EnabledForJreRange(min = JRE.JAVA_21)
        void testVirtualThreadUni() {
            given().body("rose").post("/flowers/mutiny/uni/virtual-thread").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("peony").post("/flowers/mutiny/uni/virtual-thread").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
            given().body("daisy").post("/flowers/mutiny/uni/virtual-thread").then()
                    .statusCode(500)
                    .body(assertBodyRequestScopedContextWasNotActive());
        }
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
