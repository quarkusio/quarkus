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
        @ResourceArg(name = LogCollectingTestResource.INCLUDE, value = "org\\.hibernate\\..*"),
        // Ignore logs about schema management:
        // they are unfortunate (https://github.com/quarkusio/quarkus/issues/16204)
        // but for now we have to live with them.
        @ResourceArg(name = LogCollectingTestResource.EXCLUDE, value = "org\\.hibernate\\.tool\\.schema.*")
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
        assertThat(LogCollectingTestResource.current().getRecords())
                // There shouldn't be any warning or error
                .as("Startup logs (warning or higher)")
                .extracting(LogCollectingTestResource::format)
                .satisfiesExactlyInAnyOrder(
                        // Final classes cannot be proxied
                        m -> assertThat(m).contains(
                                "Could not create proxy factory", CompanyCustomer.class.getName(),
                                "this class is final", "Your application might perform better if this class was non-final.")
                // Importantly, we don't expect any other warning about proxies!
                );
    }

}
