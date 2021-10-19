package io.quarkus.resteasy.test;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ContentLengthTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ContentLengthResource.class))
            .overrideConfigKey("quarkus.resteasy.vertx.response-buffer-size", "100");

    @Test
    public void testContentLengthSet() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; ++i) {
            sb.append("Hello World");
        }
        RestAssured.given().body(sb.toString()).post("/length/cl").then()
                .header("Content-Length", Matchers.equalTo(Integer.toString(sb.length()))).and()
                .header("Transfer-Encoding", Matchers.not("chunked"));
    }

    @Test
    public void testClForSmallResponse() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; ++i) {
            sb.append("A");
        }
        RestAssured.given().body(sb.toString()).post("/length").then()
                .header("Content-Length", Matchers.equalTo(Integer.toString(sb.length()))).and()
                .header("Transfer-Encoding", Matchers.not("chunked"));
    }

    @Test
    public void testResponseBufferSize() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 101; ++i) {
            sb.append("A");
        }
        RestAssured.given().body(sb.toString()).post("/length").then()
                .header("Content-Length", Matchers.nullValue()).and()
                .header("Transfer-Encoding", "chunked");
    }
}
