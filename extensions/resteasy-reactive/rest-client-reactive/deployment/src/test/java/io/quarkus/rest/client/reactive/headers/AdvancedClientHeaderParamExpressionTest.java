package io.quarkus.rest.client.reactive.headers;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.NotBody;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class AdvancedClientHeaderParamExpressionTest {
    private static final String HEADER_VALUE = "foobar";

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, Resource.class, DummyHeaderCalculator.class)
                    .addAsResource(
                            new StringAsset("my.property-value=" + HEADER_VALUE),
                            "application.properties"));

    @Test
    void test() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.call()).isEqualTo("property-foobar-value/method-test-call/foobar-test-method-header");
        assertThat(client.call2()).isEqualTo("property-foobar-value/method-test-call/testtest2");
        assertThat(client.call3(null, "1234", null)).isEqualTo("property-foobar-value/method-test-call/Bearer 1234");
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {
        @GET
        public String returnHeaderValue(@HeaderParam("class-header") String classHeader,
                @HeaderParam("class-header2") String classHeader2,
                @HeaderParam("method-header") List<String> methodHeader) {
            return classHeader + "/" + classHeader2 + "/" + String.join("", methodHeader);
        }

    }

    @ClientHeaderParam(name = "class-header", value = "property-${my.property-value}-value")
    @ClientHeaderParam(name = "class-header2", value = "method-{calculate}-call")
    public interface Client {

        @GET
        @ClientHeaderParam(name = "method-header", value = "${my.property-value}-{calculate}-{io.quarkus.rest.client.reactive.headers.DummyHeaderCalculator.calculate2}")
        String call();

        @GET
        @ClientHeaderParam(name = "method-header", value = "{calculate2}")
        String call2();

        @GET
        @ClientHeaderParam(name = "method-header", value = "Bearer {token}{returnNull}")
        String call3(@NotBody Object unused, String token, @NotBody Object alsoUnused);

        default String calculate() {
            return "test";
        }

        default String[] calculate2() {
            return new String[] { "test", "test2" };
        }

        default String returnNull() {
            return null;
        }

    }

}
