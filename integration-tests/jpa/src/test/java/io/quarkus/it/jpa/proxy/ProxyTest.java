package io.quarkus.it.jpa.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.LogCollectingTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(value = LogCollectingTestResource.class, restrictToAnnotatedClass = true, initArgs = {
        @ResourceArg(name = LogCollectingTestResource.LEVEL, value = "WARNING"),
        @ResourceArg(name = LogCollectingTestResource.INCLUDE, value = "org\\.hibernate\\..*")
})
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

    @Test
    public void testAbstractClassProxies() {
        RestAssured.when().get("/jpa-test/proxy/abstract").then().body(is("OK"));
    }

    @Test
    // When running as integration test, we cannot easily spy on logs.
    @DisabledOnIntegrationTest
    public void testProxyWarningsOnStartup() {
        // ORM 8 bytecode enhancement strips final from entity classes (HHH-20512),
        // so CompanyCustomer is no longer final at runtime and gets a proxy.
        // No warnings expected.
        assertThat(LogCollectingTestResource.current().getRecords())
                .as("Startup logs (warning or higher)")
                .extracting(LogCollectingTestResource::format)
                .isEmpty();
    }

}
