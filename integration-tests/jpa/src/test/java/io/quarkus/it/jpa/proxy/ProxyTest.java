package io.quarkus.it.jpa.proxy;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ProxyTest {

    @Test
    public void testBasicProxies() {
        RestAssured.when().get("/jpa-test/proxy/basic").then().body(is("OK"));
    }

    @Test
    public void testProxyInheritance() {
        RestAssured.when().get("/jpa-test/proxy/inheritance").then().body(is("OK"));
    }

    @Test
    public void testEnhancedProxies() {
        RestAssured.when().get("/jpa-test/proxy/enhanced").then().body(is("OK"));
    }

}
