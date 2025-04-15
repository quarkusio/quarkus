package io.quarkus.resteasy.reactive.server.test.path;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class RootPathTestHTTPResourceTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.http.root-path", "app/")
            .withApplicationRoot((jar) -> jar
                    .addClass(HelloResource.class));

    @TestHTTPEndpoint(HelloResource.class)
    @TestHTTPResource
    String url;

    @Test
    public void testRestAssured() {
        RestAssured.basePath = "/";
        when().get("/app/hello").then().statusCode(200).body(Matchers.is("hello"));
        when().get("/app/hello/nested").then().statusCode(200).body(Matchers.is("world hello"));
    }

    @Test
    public void testTestHTTPResource() {
        assertThat(url).isEqualTo("http://localhost:8081/app/hello");
    }
}
