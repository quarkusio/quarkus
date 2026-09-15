package io.quarkus.rest.client.reactive.subresource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class SubResourceRegisterProviderTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RootClient.class, SubClient.class, SubSubClient.class, Resource.class,
                            RootFilter.class, SubFilter.class, SubNotFoundMapper.class, SubNotFoundException.class));

    @TestHTTPResource
    URI baseUri;

    @RestClient
    RootClient injectedClient;

    @Test
    void providerOnSubResourceInterfaceAppliesToSubResourceCalls() {
        Response response = injectedClient.sub("mthd").post("entity");
        assertThat(response.readEntity(String.class)).isEqualTo("rt/mthd:entity:null");
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertThat(headers.getFirst("fromSubProvider")).isEqualTo("SubFilter");
        assertThat(headers.getFirst("fromRootProvider")).isEqualTo("RootFilter");
    }

    @Test
    void providerOnSubResourceInterfaceDoesNotApplyToRootCalls() {
        Response response = injectedClient.post("mthd", "entity");
        assertThat(response.readEntity(String.class)).isEqualTo("rt/mthd:entity:null");
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertThat(headers.getFirst("fromRootProvider")).isEqualTo("RootFilter");
        assertThat(headers.containsKey("fromSubProvider")).isFalse();
    }

    @Test
    void providerOnSubResourceInterfaceAppliesToNestedSubResourceCalls() {
        RootClient rootClient = RestClientBuilder.newBuilder().baseUri(baseUri).build(RootClient.class);
        Response response = rootClient.sub("mthd").sub().post("entity");
        assertThat(response.readEntity(String.class)).isEqualTo("rt/mthd/sub:entity:null");
        assertThat(response.getHeaders().getFirst("fromSubProvider")).isEqualTo("SubFilter");
    }

    @Test
    void exceptionMapperOnSubResourceInterfaceAppliesToSubResourceCallsOnly() {
        assertThatThrownBy(() -> injectedClient.sub("mthd").notFound())
                .isInstanceOf(SubNotFoundException.class);
        assertThatThrownBy(() -> injectedClient.notFound("mthd"))
                .isInstanceOf(WebApplicationException.class)
                .isNotInstanceOf(SubNotFoundException.class);
    }

    @Path("/path/rt")
    @RegisterRestClient(baseUri = "http://localhost:8081")
    @RegisterProvider(RootFilter.class)
    interface RootClient {

        @Path("/{methodParam}")
        SubClient sub(@PathParam("methodParam") String methodParam);

        @POST
        @Path("/{methodParam}")
        Response post(@PathParam("methodParam") String methodParam, String entity);

        @GET
        @Path("/{methodParam}/no/such/path/here")
        String notFound(@PathParam("methodParam") String methodParam);
    }

    @RegisterProvider(SubFilter.class)
    @RegisterProvider(SubNotFoundMapper.class)
    interface SubClient {

        @POST
        Response post(String entity);

        @GET
        @Path("/no/such/path/here")
        String notFound();

        @Path("/sub")
        SubSubClient sub();
    }

    interface SubSubClient {

        @POST
        Response post(String entity);
    }

    public static class RootFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) {
            requestContext.getHeaders().putSingle("fromRootProvider", "RootFilter");
        }
    }

    public static class SubFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) {
            requestContext.getHeaders().putSingle("fromSubProvider", "SubFilter");
        }
    }

    public static class SubNotFoundMapper implements ResponseExceptionMapper<SubNotFoundException> {

        @Override
        public boolean handles(int status, MultivaluedMap<String, Object> headers) {
            return status == 404;
        }

        @Override
        public SubNotFoundException toThrowable(Response response) {
            return new SubNotFoundException();
        }
    }

    public static class SubNotFoundException extends RuntimeException {
    }
}
