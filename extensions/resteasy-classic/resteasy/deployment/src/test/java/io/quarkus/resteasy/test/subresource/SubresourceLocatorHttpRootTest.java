package io.quarkus.resteasy.test.subresource;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SubresourceLocatorHttpRootTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PingResource.class, PingsResource.class, MyService.class, SuperPingResource.class)
                    .addAsResource(new StringAsset("quarkus.http.root-path=/foo"), "application.properties"));

    @Test
    public void testSubresourceLocator() {
        RestAssured.when().get("/pings/do").then().body(Matchers.is("pong"));
        RestAssured.when().get("/pings/super").then().body(Matchers.is("superpong"));
    }

}
