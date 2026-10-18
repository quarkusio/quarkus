package io.quarkus.rest.client.reactive.subresource;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class SubResourceMatrixParamTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, RootClient.class, SubClient.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void clientSendsMatrixParamOnSubResource() {
        RootClient client = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).build(RootClient.class);
        assertThat(client.sub("users").greet("world")).isEqualTo("users/hello world");
    }

    @Path("/")
    public static class Resource {

        @Path("{root}")
        public SubResource sub(@PathParam("root") String root) {
            return new SubResource(root);
        }

        public static class SubResource {
            private final String root;

            public SubResource(String root) {
                this.root = root;
            }

            @GET
            @Path("greet")
            public String greet(@MatrixParam("name") String name) {
                return root + "/hello " + name;
            }
        }
    }

    public interface RootClient {
        @Path("{root}")
        SubClient sub(@PathParam("root") String root);
    }

    public interface SubClient {
        @GET
        @Path("greet")
        String greet(@MatrixParam("name") String name);
    }
}
