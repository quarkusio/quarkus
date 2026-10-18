package ilove.quark.us.funqygooglecloudfunctions;

import io.quarkus.google.cloud.functions.test.FunctionType;
import io.quarkus.google.cloud.functions.test.WithFunction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@WithFunction(value = FunctionType.FUNQY_BACKGROUND, functionName = "helloGCSWorld")
class GreetingFunctionsStorageTest {
    @Test
    void testHelloGCSWorld() {
        given()
                .body("{\"data\":{\"name\":\"hello.txt\"}}")
                .when()
                .post()
                .then()
                .statusCode(200);
    }
}