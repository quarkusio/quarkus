package io.quarkus.it.spatial;

import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class HibernateSpatialTest {
    @Test
    public void testGeolattePoint() {
        RestAssured.when().get("/hibernate-spatial-test/geolatte/point/1").then()
                .body(containsString("true"));
    }

    @Test
    public void testGeolatteLine() {
        RestAssured.when().get("/hibernate-spatial-test/geolatte/line/2").then()
                .body(containsString("LINESTRING(0 0,5 0,5 5)"));
    }

    @Test
    public void testGeolattePolygon() {
        RestAssured.when().get("/hibernate-spatial-test/geolatte/polygon/3").then()
                .body(containsString("4326"));
    }

    @Test
    public void testJtsPoint() {
        RestAssured.when().get("/hibernate-spatial-test/jts/point/4").then()
                .body(containsString("true"));
    }

    @Test
    public void testJtsLine() {
        RestAssured.when().get("/hibernate-spatial-test/jts/line/5").then()
                .body(containsString("LINESTRING(0 0,5 0,5 5)"));
    }

    @Test
    public void testJtsPolygon() {
        RestAssured.when().get("/hibernate-spatial-test/jts/polygon/6").then()
                .body(containsString("4326"));
    }
}
