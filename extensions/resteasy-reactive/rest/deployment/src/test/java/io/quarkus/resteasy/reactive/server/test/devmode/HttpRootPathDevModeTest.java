package io.quarkus.resteasy.reactive.server.test.devmode;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class HttpRootPathDevModeTest {
    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Resource.class)
                    .addAsResource(new StringAsset("%dev.quarkus.http.root-path=/custom-path"), "application.properties"));

    @Test
    void rootPath() {
        RestAssured.given()
                .get("/custom-path/test")
                .then()
                .statusCode(200);
    }

    @Path("/test")
    public static class Resource {
        @GET
        public String get() {
            return "Hello!";
        }
    }
}
