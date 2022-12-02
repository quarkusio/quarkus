package io.quarkus.grpc.client;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.grpc.client.tls.HelloWorldTlsEndpoint;
import io.quarkus.test.QuarkusUnitTest;

class HelloWorldTlsEndpointTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(HelloWorldTlsEndpoint.class.getPackage())
                    .addPackage(io.grpc.examples.helloworld.GreeterGrpc.class.getPackage()))
            .withConfigurationResource("grpc-client-tls-configuration.properties");

    @Test
    void shouldExposeAndConsumeTLSWithKeysFromFiles() {
        String response = get("/hello/blocking/neo").asString();
        assertThat(response).isEqualTo("Hello neo");
    }
}
