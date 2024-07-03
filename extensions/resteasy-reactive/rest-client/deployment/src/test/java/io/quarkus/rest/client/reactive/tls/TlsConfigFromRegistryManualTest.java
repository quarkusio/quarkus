package io.quarkus.rest.client.reactive.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.ext.web.RoutingContext;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "tls-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class TlsConfigFromRegistryManualTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Resource.class)
                    .addAsResource(new File("target/certs/tls-test-keystore.jks"), "keystore.jks")
                    .addAsResource(new File("target/certs/tls-test-truststore.jks"), "truststore.jks"))
            .overrideConfigKey("quarkus.tls.key-store.jks.path", "keystore.jks")
            .overrideConfigKey("quarkus.tls.key-store.jks.password", "secret")
            .overrideConfigKey("quarkus.tls.rest-client.trust-store.jks.path", "truststore.jks")
            .overrideConfigKey("quarkus.tls.rest-client.trust-store.jks.password", "secret");

    @TestHTTPResource(tls = true)
    URL url;

    @Inject
    TlsConfigurationRegistry registry;

    @Test
    void shouldHello() {
        Optional<TlsConfiguration> maybeTlsConfiguration = TlsConfiguration.from(registry, Optional.of("rest-client"));
        assertThat(maybeTlsConfiguration).isPresent();
        Client client = QuarkusRestClientBuilder.newBuilder().baseUrl(url).tlsConfiguration(maybeTlsConfiguration.get())
                .build(Client.class);
        assertThat(client.echo("w0rld")).isEqualTo("hello, w0rld");
    }

    @Path("/hello")
    public interface Client {
        @POST
        String echo(String name);
    }

    @Path("/hello")
    public static class Resource {
        @POST
        public String echo(String name, @Context RoutingContext rc) {
            assertThat(rc.request().connection().isSsl()).isTrue();
            assertThat(rc.request().isSSL()).isTrue();
            assertThat(rc.request().connection().sslSession()).isNotNull();
            return "hello, " + name;
        }
    }
}
