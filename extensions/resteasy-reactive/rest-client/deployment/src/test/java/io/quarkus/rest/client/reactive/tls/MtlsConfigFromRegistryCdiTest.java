package io.quarkus.rest.client.reactive.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.ext.web.RoutingContext;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "mtls-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }, client = true))
public class MtlsConfigFromRegistryCdiTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Resource.class)
                    .addAsResource(new File("target/certs/mtls-test-keystore.p12"), "server-keystore.p12")
                    .addAsResource(new File("target/certs/mtls-test-server-truststore.p12"), "server-truststore.p12")
                    .addAsResource(new File("target/certs/mtls-test-client-keystore.p12"), "client-keystore.p12")
                    .addAsResource(new File("target/certs/mtls-test-client-truststore.p12"), "client-truststore.p12"))

            .overrideConfigKey("quarkus.tls.server.key-store.p12.path", "server-keystore.p12")
            .overrideConfigKey("quarkus.tls.server.key-store.p12.password", "secret")
            .overrideConfigKey("quarkus.tls.server.trust-store.p12.path", "server-truststore.p12")
            .overrideConfigKey("quarkus.tls.server.trust-store.p12.password", "secret")
            .overrideConfigKey("quarkus.http.tls-configuration-name", "server")

            .overrideConfigKey("quarkus.tls.rest-client.key-store.p12.path", "client-keystore.p12")
            .overrideConfigKey("quarkus.tls.rest-client.key-store.p12.password", "secret")
            .overrideConfigKey("quarkus.tls.rest-client.trust-store.p12.path", "client-truststore.p12")
            .overrideConfigKey("quarkus.tls.rest-client.trust-store.p12.password", "secret")
            .overrideConfigKey("quarkus.rest-client.rc.url", "https://localhost:${quarkus.http.test-ssl-port:8444}")
            .overrideConfigKey("quarkus.rest-client.rc.tls-configuration-name", "rest-client");

    @RestClient
    Client client;

    @Test
    void shouldHello() {
        assertThat(client.echo("w0rld")).isEqualTo("hello, w0rld");
    }

    @Path("/hello")
    @RegisterRestClient(configKey = "rc")
    public interface Client {
        @POST
        String echo(String name);
    }

    @Path("/hello")
    public static class Resource {
        @POST
        public String echo(String name, @Context RoutingContext rc) {
            Assertions.assertThat(rc.request().connection().isSsl()).isTrue();
            Assertions.assertThat(rc.request().isSSL()).isTrue();
            Assertions.assertThat(rc.request().connection().sslSession()).isNotNull();
            return "hello, " + name;
        }
    }
}
