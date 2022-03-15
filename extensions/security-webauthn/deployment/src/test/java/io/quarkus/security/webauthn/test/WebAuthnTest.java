package io.quarkus.security.webauthn.test;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.restassured.RestAssured;

public class WebAuthnTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebAuthnManualTestUserProvider.class, WebAuthnTestUserProvider.class, TestUtil.class));

    @Test
    public void testJavaScriptFile() {
        RestAssured.get("/webauthn/webauthn.js").then().statusCode(200).body(Matchers.startsWith("\"use strict\";"));
    }
}
