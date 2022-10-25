package io.quarkus.resteasy.test;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MalformedURLTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(GreetingResource.class)
                    .addAsResource(new StringAsset("greeting.message=hello"), "application.properties"));

    @Test
    public void testMalformedURL() {
        // using JAX-RS client here because RestAssures seems to be doing some cleansing of the URL
        Response response = ClientBuilder
                .newClient().target("http://localhost:"
                        + ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class) + "/%FF")
                .request().get();
        Assertions.assertEquals(response.getStatus(), 400);
    }
}
