package io.quarkus.rest.client.reactive.proxy;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class HttpProxyDevServicesDevModeRestartTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(LocalProxyDevModeResource.class, LocalProxyDevModeClient.class)
                    .addAsResource(new StringAsset("""
                            quarkus.rest-client.local-proxy.url=quarkus://vertx-http
                            quarkus.rest-client.local-proxy.enable-local-proxy=true
                            """), "application.properties"))
            .setLogRecordPredicate(record -> record.getMessage() != null
                    && record.getMessage().startsWith("Started HTTP proxy server"));

    @Test
    void reusedProxyForwardsToRestartedApplication() {
        when().get("/local-proxy/through-proxy")
                .then()
                .statusCode(200)
                .body(equalTo("before restart"));

        TEST.modifySourceFile(HttpProxyDevServicesDevModeRestartTest.class,
                source -> source.replace("before restart", "after restart"));

        when().get("/local-proxy/through-proxy")
                .then()
                .statusCode(200)
                .body(equalTo("after restart"));

        // the restart does not change the proxy, so it is reused instead of started again
        assertThat(TEST.getLogRecords()).hasSize(1);
    }

    @Path("local-proxy")
    public static class LocalProxyDevModeResource {

        @RestClient
        LocalProxyDevModeClient client;

        @GET
        @Path("value")
        public String value() {
            return "before restart";
        }

        @GET
        @Path("through-proxy")
        public String throughProxy() {
            return client.value();
        }
    }

    @Path("local-proxy")
    @RegisterRestClient(configKey = "local-proxy")
    public interface LocalProxyDevModeClient {

        @GET
        @Path("value")
        String value();
    }
}
