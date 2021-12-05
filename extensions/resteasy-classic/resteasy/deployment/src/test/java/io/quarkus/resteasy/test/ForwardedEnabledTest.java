package io.quarkus.resteasy.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ForwardedEnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class)
                    .addAsResource(new StringAsset("quarkus.http.proxy.proxy-address-forwarding=true\n" +
                            "quarkus.http.proxy.enable-forwarded-host=true\n"),
                            "application.properties"));

    @Test
    public void test() {
        RestAssured.get("/test").then().statusCode(200).body(Matchers.equalToIgnoringCase("hello"));
        RestAssured.given().header("Host", "").get("/test").then().statusCode(200).body(Matchers.equalToIgnoringCase("hello"));
    }

    @Path("/test")
    public static class TestResource {

        @GET
        public String get() {
            return "hello";
        }
    }

}
