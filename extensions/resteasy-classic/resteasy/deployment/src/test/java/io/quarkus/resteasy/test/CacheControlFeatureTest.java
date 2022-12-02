package io.quarkus.resteasy.test;

import static io.restassured.RestAssured.when;

import javax.ws.rs.core.HttpHeaders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CacheControlFeatureTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(CacheResource.class));

    @Test
    public void testNoCacheAnnotation() {
        when().get("/nocache").then().header(HttpHeaders.CACHE_CONTROL, "no-cache=\"foo\"");
    }

    @Test
    public void testCacheAnnotation() {
        when().get("/cache").then().header(HttpHeaders.CACHE_CONTROL, "max-age=123");
    }

}
