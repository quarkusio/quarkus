package io.quarkus.rest.test.stream;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class StreamTestCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(StreamResource.class));

    @Test
    public void testStreaming() throws Exception {
        RestAssured.get("/stream/text/stream")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));
        RestAssured.get("/stream/text/collect")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));

        RestAssured.get("/stream/byte-arrays/stream")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));
        RestAssured.get("/stream/byte-arrays/collect")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));

        RestAssured.get("/stream/char-arrays/stream")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));
        RestAssured.get("/stream/char-arrays/collect")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));

        RestAssured.get("/stream/buffer/stream")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));
        RestAssured.get("/stream/buffer/collect")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));
    }
}
