package io.quarkus.virtual.graphql;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;
import io.restassured.RestAssured;

@QuarkusTest
@VirtualThreadUnit
@ShouldNotPin
class RunOnVirtualThreadTest extends AbstractGraphQLTest {

    @Test
    public void testAnnotatedBlockingObject() {

        String fooRequest = getPayload("{\n" +
                "  annotatedRunOnVirtualThreadObject {\n" +
                "    name\n" +
                "    priority\n" +
                "    state\n" +
                "    group\n" +
                "    vertxContextClassName\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(fooRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.annotatedRunOnVirtualThreadObject.name", Matchers.startsWith("quarkus-virtual-thread"))
                .and()
                .body("data.annotatedRunOnVirtualThreadObject.vertxContextClassName",
                        Matchers.equalTo("io.vertx.core.impl.DuplicatedContext"));
    }

    @Test
    public void testAnnotatedBlockingMutationObject() {

        String fooRequest = getPayload("mutation{\n" +
                "  annotatedRunOnVirtualThreadMutationObject(test:\"test\") {\n" +
                "    name\n" +
                "    priority\n" +
                "    state\n" +
                "    group\n" +
                "    vertxContextClassName\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(fooRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.annotatedRunOnVirtualThreadMutationObject.name", Matchers.startsWith("quarkus-virtual-thread"))
                .and()
                .body("data.annotatedRunOnVirtualThreadMutationObject.vertxContextClassName",
                        Matchers.equalTo("io.vertx.core.impl.DuplicatedContext"));
    }

    @Test
    @Disabled
    public void testPiningThread() {

        String fooRequest = getPayload("{\n" +
                "  pinThread {\n" +
                "    name\n" +
                "    priority\n" +
                "    state\n" +
                "    group\n" +
                "    vertxContextClassName\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(fooRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("data.pinThread.name", Matchers.startsWith("quarkus-virtual-thread"))
                .and()
                .body("data.pinThread.vertxContextClassName",
                        Matchers.equalTo("io.vertx.core.impl.DuplicatedContext"));
    }

}
