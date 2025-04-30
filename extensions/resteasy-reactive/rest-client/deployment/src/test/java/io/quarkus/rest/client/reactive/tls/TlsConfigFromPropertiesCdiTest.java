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

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "tls-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class TlsConfigFromPropertiesCdiTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Resource.class)
                    .addAsResource(new File("target/certs/tls-test-keystore.jks"), "keystore.jks")
                    .addAsResource(new File("target/certs/tls-test-truststore.jks"), "truststore.jks"))
            .overrideConfigKey("quarkus.tls.key-store.jks.path", "keystore.jks")
            .overrideConfigKey("quarkus.tls.key-store.jks.password", "secret")

            .overrideConfigKey("quarkus.rest-client.rc.url", "https://localhost:${quarkus.http.test-ssl-port:8444}")
            .overrideConfigKey("quarkus.rest-client.rc.trust-store", "classpath:truststore.jks")
            .overrideConfigKey("quarkus.rest-client.rc.trust-store-password", "secret");

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
