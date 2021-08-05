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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.HelloClient2;
import io.quarkus.rest.client.reactive.HelloResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ProviderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloResource.class, HelloClient.class, GlobalRequestFilter.class, HelloClient2.class,
                            GlobalResponseFilter.class, GlobalRequestFilterConstrainedToServer.class,
                            GlobalFeature.class)
                    .addAsResource(
                            new StringAsset(setUrlForClass(HelloClient.class) + setUrlForClass(HelloClient2.class)),
                            "application.properties"));

    @RestClient
    HelloClient helloClient;

    @TestHTTPResource
    URI baseUri;

    @AfterEach
    public void cleanUp() {
        GlobalRequestFilter.abort = false;
        GlobalFeature.called = false;
    }

    @Test
    void shouldNotRegisterFeatureAutomatically() {
        Response response = helloClient.echo("Michał");
        assertThat(response.getStatus()).isEqualTo(GlobalResponseFilter.STATUS);
        assertThat(GlobalFeature.called).isFalse();
    }

    @Test
    void shouldUseGlobalRequestFilterForInjectedClient() {
        GlobalRequestFilter.abort = true;
        Response response = helloClient.echo("Michał");
        assertThat(response.getStatus()).isEqualTo(GlobalRequestFilter.STATUS);
    }

    @Test
    void shouldUseGlobalResponseFilterForInjectedClient() {
        Response response = helloClient.echo("Michał");
        assertThat(response.getStatus()).isEqualTo(GlobalResponseFilter.STATUS);
    }

    @Test
    void shouldUseGlobalRequestFilterForBuiltClient() {
        GlobalRequestFilter.abort = true;
        Response response = helloClient().echo("Michał");
        assertThat(response.getStatus()).isEqualTo(GlobalRequestFilter.STATUS);
    }

    @Test
    void shouldUseGlobalResponseFilterForBuiltClient() {
        Response response = helloClient().echo("Michał");
        assertThat(response.getStatus()).isEqualTo(GlobalResponseFilter.STATUS);
    }

    private HelloClient helloClient() {
        return RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(HelloClient.class);
    }
}
