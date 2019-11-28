package io.quarkus.jwt.test;

import java.net.HttpURLConnection;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SmallryeJwtDisabledTest {
    private static Class<?>[] testClasses = {
            DefaultGroupsEndpoint.class
    };
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addAsResource("publicKey.pem")
                    .addAsResource("smallryeJwtDisabled.properties", "application.properties"));

    @Test
    public void serviceIsNotSecured() throws Exception {
        io.restassured.response.Response response = RestAssured.given().get("/endp/echo").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.getStatusCode());
    }
}
