package io.quarkus.restclient.registerprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RegisterProviderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EchoResource.class, EchoClient.class, MyFilter.class, MethodsCollector.class,
                            MyRequestBean.class)
                    .addAsResource(new StringAsset("io.quarkus.restclient.registerprovider.EchoClient/mp-rest/url=${test.url}"),
                            "application.properties"));

    @RestClient
    EchoClient client;

    @Inject
    MethodsCollector collector;

    @Test
    public void testProvider() {
        collector.getMethods().clear();
        assertEquals("ping", client.echo("ping"));
        assertEquals(1, collector.getMethods().size());
        assertEquals("GET", collector.getMethods().get(0));
    }

    @Test
    public void testContextPropagation() throws InterruptedException, ExecutionException {
        assertEquals("OK", client.callClient());
        assertEquals("OK", client.asyncCallClient().toCompletableFuture().get());
    }
}
