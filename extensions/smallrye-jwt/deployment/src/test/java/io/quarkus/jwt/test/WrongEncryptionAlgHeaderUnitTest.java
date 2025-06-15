package io.quarkus.jwt.test;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class WrongEncryptionAlgHeaderUnitTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(DefaultGroupsEndpoint.class).addAsResource("rsa-oaep.jwk")
                    .addAsResource("applicationEncryptWrongAlgorithm.properties", "application.properties"));

    @Test
    public void echoGroups() {
        String token = "eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4R0NNIn0"
                + ".CuUuY9PH2wWjuLXd5O9LLFanwyt5-y-NzEpy9rC3A63tFsvdp8GWP1kRt1d3zd0bGqakwls623VQxzxqQ25j5gdHh8dKMl67xTLHt1Qlg36nI9Ukn7syq25VrzfrRRwy0k7isqMncHpzuBQlmfzPrszW7d13z7_ex0Uha869RaP-W2NNBfHYw26xIXcCSVIPg8jTLA7h6QmOetEej-NXXcWrRKQgBRapYy4iWrij9Vr3JzAGSHVtIID74tFOm01FdJj4s1M4IXegDbvAdQb6Vao1Ln5GolnTki4IGvH5FDssDHz6MS2JG5QBcITzfuXU81vDC00xzNEuMat0AngmOw"
                + ".UjPQbnakkZYUdoDa" + ".vcbS" + ".WQ_bOPiGKjPSq-qyGOIfjA";
        RestAssured.given().auth().oauth2(token).get("/endp/echo").then().assertThat().statusCode(401)
                .body(Matchers.emptyString());
    }
}
