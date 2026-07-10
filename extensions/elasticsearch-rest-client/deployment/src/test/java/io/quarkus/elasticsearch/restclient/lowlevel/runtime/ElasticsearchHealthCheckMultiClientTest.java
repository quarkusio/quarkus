package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.elasticsearch.restclient.lowlevel.runtime.health.ElasticsearchHealthCheckCondition;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.Identifier;

public class ElasticsearchHealthCheckMultiClientTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(ClientConsumer.class))
            .overrideConfigKey("quarkus.elasticsearch.hosts", "localhost:19200")
            .overrideConfigKey("quarkus.elasticsearch.\"second-client\".hosts", "localhost:19201");

    @ApplicationScoped
    public static class ClientConsumer {
        @Inject
        RestClient defaultClient;
        @Inject
        @Identifier("second-client")
        RestClient secondClient;
    }

    @Test
    void healthCheckReportsAllClients() {
        assertEquals(2, Arc.container().listAll(ElasticsearchHealthCheckCondition.class).size(),
                "Health check conditions should be created for both the default and named client");

        RestAssured.when().get("/q/health/ready")
                .then()
                .body("status", equalTo("DOWN"))
                .body("checks.find { it.name == 'Elasticsearch cluster health check' }.status", equalTo("DOWN"))
                .body("checks.find { it.name == 'Elasticsearch cluster health check' }.data.'reason(<default>)'",
                        notNullValue())
                .body("checks.find { it.name == 'Elasticsearch cluster health check' }.data.'reason(second-client)'",
                        notNullValue());
    }
}
