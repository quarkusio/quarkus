package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.Identifier;

public class ElasticsearchHealthCheckInactiveClientTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(ClientConsumer.class))
            .overrideConfigKey("quarkus.elasticsearch.hosts", "localhost:19200")
            .overrideConfigKey("quarkus.elasticsearch.\"second-client\".hosts", "localhost:19201")
            .overrideRuntimeConfigKey("quarkus.elasticsearch.\"second-client\".active", "false");

    @ApplicationScoped
    public static class ClientConsumer {
        @Inject
        InjectableInstance<RestClient> defaultClient;
        @Inject
        @Identifier("second-client")
        InjectableInstance<RestClient> secondClient;
    }

    @Test
    void healthCheckSkipsInactiveClient() {
        RestAssured.when().get("/q/health/ready")
                .then()
                .body("status", equalTo("DOWN"))
                .body("checks.find { it.name == 'Elasticsearch cluster health check' }.status", equalTo("DOWN"))
                .body("checks.find { it.name == 'Elasticsearch cluster health check' }.data.'reason(<default>)'",
                        notNullValue())
                .body("checks.find { it.name == 'Elasticsearch cluster health check' }.data.'reason(second-client)'",
                        nullValue());
    }
}
