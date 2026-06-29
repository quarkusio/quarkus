package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.Identifier;

public class ElasticsearchUserProvidedClientTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(UserProvidedClientProducer.class))
            .overrideConfigKey("quarkus.elasticsearch.hosts", "localhost:19200");

    @ApplicationScoped
    public static class UserProvidedClientProducer {
        static boolean producerCalled = false;

        @Produces
        @Identifier("user-managed")
        @Singleton
        RestClient createClient() {
            producerCalled = true;
            return RestClient.builder(new HttpHost("localhost", 19201)).build();
        }
    }

    @Inject
    RestClient defaultClient;

    @Inject
    @Identifier("user-managed")
    RestClient userManagedClient;

    @Test
    void userProvidedBeanIsUsedWithoutConflict() {
        assertNotNull(defaultClient);
        assertNotNull(userManagedClient);
        assertTrue(UserProvidedClientProducer.producerCalled,
                "The user-provided producer should have been called");
    }

    @Test
    void healthCheckOnlyReportsDefaultClient() {
        RestAssured.when().get("/q/health/ready")
                .then()
                .body("checks.find { it.name == 'Elasticsearch cluster health check' }.data.'reason(<default>)'",
                        notNullValue())
                .body("checks.find { it.name == 'Elasticsearch cluster health check' }.data.'reason(user-managed)'",
                        nullValue())
                .body("checks.find { it.name == 'Elasticsearch cluster health check' }.data.'status(user-managed)'",
                        nullValue());
    }
}
