package io.quarkus.security.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class CustomAuthEmbeddedEncryptedSha512_256Test extends CustomAuthEmbeddedBase {
    static Class[] testClasses = {
            TestSecureServlet.class, TestApplication.class, RolesEndpointClassLevel.class,
            ParametrizedPathsResource.class, SubjectExposingResource.class
    };

    // passwords generated with `echo -n username:Quarkus:password | openssl dgst -sha512-256`
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClasses(CustomAuth.class))
            .overrideConfigKey("quarkus.security.users.embedded.enabled", "true")
            .overrideRuntimeConfigKey("quarkus.security.users.embedded.algorithm", "digest-sha-512-256")
            // jb0ss
            .overrideRuntimeConfigKey("quarkus.security.users.embedded.users.scott",
                    "41338554d917c9b31a74da9223ea2fa86f482c6d65b0ee037368b01a63aedaf8")
            // test
            .overrideRuntimeConfigKey("quarkus.security.users.embedded.users.stuart",
                    "6cb0f41a5b74188abf118a31868cf0d137769644cef3ada2abaab54c378e98dd")
            // p4ssw0rd
            .overrideRuntimeConfigKey("quarkus.security.users.embedded.users.jdoe",
                    "a5dac9a072a68cdc1a10e01b9feeb9949903a90babd9169d6e0098a11663abc1")
            // n0Adm1n
            .overrideRuntimeConfigKey("quarkus.security.users.embedded.users.noadmin",
                    "d31a0659adb1c0a2c39ae9702a061197fe2e08538b71cf62307d28c1d6bb5054")
            .overrideRuntimeConfigKey("quarkus.security.users.embedded.roles.scott", "Admin,admin,Tester,user")
            .overrideRuntimeConfigKey("quarkus.security.users.embedded.roles.stuart", "admin,user")
            .overrideRuntimeConfigKey("quarkus.security.users.embedded.roles.jdoe", "NoRolesUser")
            .overrideRuntimeConfigKey("quarkus.security.users.embedded.roles.noadmin", "user");

    @Test
    public void testSecureAccessWrongPasswordFailure() {
        RestAssured.given().auth().preemptive().basic("stuart", "wrong")
                .when().get("/secure-test").then()
                .statusCode(401);
    }
}
