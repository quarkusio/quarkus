package io.quarkus.resteasy.test.gzip;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class GZipResourceTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(GZipResource.class))
            .overrideConfigKey("quarkus.resteasy.gzip.enabled", "true");

    @Test
    public void testGzipEndpoint() {
        RestAssured.given().get("/gzip").then().statusCode(200)
                .header("content-encoding", "gzip")
                .header("content-length", Matchers.not(Matchers.equalTo(Integer.toString(GZipResource.BODY.length()))))
                .body(Matchers.equalTo(GZipResource.BODY));
    }

}
