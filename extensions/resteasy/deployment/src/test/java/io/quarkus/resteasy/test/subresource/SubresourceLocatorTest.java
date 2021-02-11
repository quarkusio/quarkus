package io.quarkus.resteasy.test.subresource;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SubresourceLocatorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PingResource.class, PingsResource.class, MyService.class, SuperPingResource.class));

    @Test
    public void testSubresourceLocator() {
        RestAssured.when().get("/pings/do").then().body(Matchers.is("pong"));
        RestAssured.when().get("/pings/super").then().body(Matchers.is("superpong"));
    }

}
