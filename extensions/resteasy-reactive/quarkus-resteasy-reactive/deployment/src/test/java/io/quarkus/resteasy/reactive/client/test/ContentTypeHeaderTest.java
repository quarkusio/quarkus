package io.quarkus.resteasy.reactive.client.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.client.impl.ClientImpl;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

// TODO: pull out RuntimeDelegateImpl to common and move the tests to client's deployment
class ContentTypeHeaderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SomeClient.class, Endpoint.class));

    @TestHTTPResource("/foo")
    URI uri;

    @Test
    void shouldUseDefaultContentType() {
        ClientImpl client = (ClientImpl) ClientBuilder.newClient();
        WebTargetImpl target = (WebTargetImpl) client.target(uri);
        SomeClient someClient = target.proxy(SomeClient.class);
        assertThat(someClient.postDefaultType("some-text"))
                .isEqualToIgnoringCase("application/octet-stream");
    }

    @Test
    void shouldUseSpecifiedContentType() {
        ClientImpl client = (ClientImpl) ClientBuilder.newClient();
        WebTargetImpl target = (WebTargetImpl) client.target(uri);
        SomeClient someClient = target.proxy(SomeClient.class);
        assertThat(someClient.postHtml("some-text"))
                .isEqualToIgnoringCase("text/html");
    }

    @Test
    void shouldPassTextEntity() {
        ClientImpl client = (ClientImpl) ClientBuilder.newClient();
        WebTargetImpl target = (WebTargetImpl) client.target(uri);
        SomeClient someClient = target.proxy(SomeClient.class);
        assertThat(someClient.echoText("some-other-text"))
                .isEqualToIgnoringCase("some-other-text");
    }

    @Path("/")
    interface SomeClient {
        @POST
        String postDefaultType(String content);

        @POST
        @Path("/as-html")
        @Consumes(MediaType.TEXT_HTML)
        String postHtml(String content);

        @POST
        @Consumes("text/plain")
        @Path("/echo")
        String echoText(String content);
    }

    @Path("/foo")
    static class Endpoint {
        @POST
        @Consumes("*/*")
        public String postDefaultType(String content, @HeaderParam("Content-Type") String contentType) {
            return contentType;
        }

        @POST
        @Consumes("text/plain")
        @Path("/echo")
        public String echoText(String content) {
            return content;
        }

        @POST
        @Path("/as-html")
        @Consumes("*/*")
        public String postHtml(String content, @HeaderParam("Content-Type") String contentType) {
            return contentType;
        }
    }
}
