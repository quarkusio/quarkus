package io.quarkus.rest.client.reactive.provider;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.HelloClient2;
import io.quarkus.rest.client.reactive.HelloResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ProviderDisabledAutodiscoveryTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloResource.class, HelloClient2.class, HelloClient.class, GlobalRequestFilter.class,
                            GlobalResponseFilter.class)
                    .addAsResource(
                            new StringAsset(setUrlForClass(HelloClient.class)
                                    + setUrlForClass(HelloClient2.class)
                                    + "quarkus.rest-client-reactive.provider-autodiscovery=false"),
                            "application.properties"));

    @RestClient
    HelloClient helloClient;

    @TestHTTPResource
    URI baseUri;

    @Test
    void shouldNotUseGlobalFilterForInjectedClient() {
        Response response = helloClient.echo("Michał");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldNotUseGlobalFilterForBuiltClient() {
        Response response = helloClient().echo("Michał");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private HelloClient helloClient() {
        return RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(HelloClient.class);
    }
}
