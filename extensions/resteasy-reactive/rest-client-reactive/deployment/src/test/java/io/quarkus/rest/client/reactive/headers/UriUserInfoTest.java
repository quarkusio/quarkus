package io.quarkus.rest.client.reactive.headers;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Base64;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class UriUserInfoTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResource.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void noUserInfo() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.call()).isNullOrEmpty();
    }

    @Test
    void withUserInfo() {
        Client client = RestClientBuilder.newBuilder().baseUri(UriBuilder.fromUri(baseUri).userInfo("foo:bar").build())
                .build(Client.class);
        assertThat(client.call()).isEqualTo("foo:bar");
    }

    @Test
    void userInfoOverridesClientHeaderParamAnnotation() {
        Client client = RestClientBuilder.newBuilder().baseUri(UriBuilder.fromUri(baseUri).userInfo("foo:bar").build())
                .build(Client.class);
        assertThat(client.call2()).isEqualTo("foo:bar");
    }

    @Test
    void userInfoDoesNotOverrideHeaderParamAnnotation() {
        Client client = RestClientBuilder.newBuilder().baseUri(UriBuilder.fromUri(baseUri).userInfo("foo:bar").build())
                .build(Client.class);
        assertThat(client.call3("Basic " + Base64.getEncoder().encodeToString(("user:pass").getBytes())))
                .isEqualTo("user:pass");
    }

    @Path("/test")
    public static class TestResource {

        @GET
        @Path("/credentials")
        public String credentials(@HeaderParam("Authorization") String authorization) {
            if ((authorization == null) || authorization.isEmpty()) {
                return null;
            }
            return new String(Base64.getDecoder().decode(authorization.substring("Basic ".length())));
        }
    }

    @Path("/test/credentials")
    public interface Client {

        @GET
        String call();

        @ClientHeaderParam(name = "Authorization", value = "whatever")
        @GET
        String call2();

        @GET
        String call3(@HeaderParam("Authorization") String authorization);
    }
}
