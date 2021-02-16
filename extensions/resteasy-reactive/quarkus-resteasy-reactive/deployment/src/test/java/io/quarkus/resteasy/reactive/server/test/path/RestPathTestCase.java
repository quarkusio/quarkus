package io.quarkus.resteasy.reactive.server.test.path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RestPathTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(HelloResource.class)
                    .addAsResource(new StringAsset("quarkus.rest.path=/foo"), "application.properties"));

    @Test
    public void testRestPath() {
        RestAssured.get("/hello")
                .then().statusCode(404);
        RestAssured.get("/foo/hello")
                .then().statusCode(200).body(Matchers.equalTo("hello"));

    }
}
