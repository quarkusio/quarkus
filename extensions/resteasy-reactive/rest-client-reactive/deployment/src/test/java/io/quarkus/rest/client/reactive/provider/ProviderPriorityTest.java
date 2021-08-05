package io.quarkus.rest.client.reactive.provider;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

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

public class ProviderPriorityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloResource.class,
                            HelloClient.class,
                            HelloClient2.class,
                            HelloClientWithFilter.class,
                            ResponseFilterLowestPrio.class,
                            GlobalResponseFilter.class,
                            GlobalResponseFilterLowPrio.class)
                    .addAsResource(
                            new StringAsset(setUrlForClass(HelloClient.class) + setUrlForClass(HelloClient2.class)
                                    + setUrlForClass(HelloClientWithFilter.class)),
                            "application.properties"));

    @RestClient
    HelloClient helloClient;

    @RestClient
    HelloClientWithFilter helloClientWithFilter;

    @AfterEach
    void cleanUp() {
        GlobalResponseFilterLowPrio.skip = false;
    }

    @Test
    void shouldApplyLocalLowestPrioFilterLast() {
        Response response = helloClientWithFilter.echo("foo");
        assertThat(response.getStatus()).isEqualTo(ResponseFilterLowestPrio.STATUS);
    }

    @Test
    void shouldApplyLowPrioFilterLast() {
        Response response = helloClient.echo("foo");
        assertThat(response.getStatus()).isEqualTo(GlobalResponseFilterLowPrio.STATUS);
    }

    @Test
    void shouldApplyHighPrioFilter() {
        GlobalResponseFilterLowPrio.skip = true;
        Response response = helloClient.echo("foo");
        assertThat(response.getStatus()).isEqualTo(GlobalResponseFilter.STATUS);
    }
}
