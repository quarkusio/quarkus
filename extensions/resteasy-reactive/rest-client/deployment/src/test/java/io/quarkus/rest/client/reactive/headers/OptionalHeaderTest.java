package io.quarkus.rest.client.reactive.headers;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Optional;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class OptionalHeaderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void test() {
        Client client = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        String result = client.send(Optional.empty(), "q", Optional.of("h2"), Optional.of(3));
        assertThat(result).isEqualTo("query=q/Header2=h2,Header3=3");
    }

    @Path("test")
    public interface Client {

        @GET
        String send(@HeaderParam("header1") Optional<String> header1, @RestQuery String query,
                @RestHeader Optional<String> header2, @RestHeader Optional<Integer> header3);
    }

    @Path("test")
    public static class Resource {

        @GET
        public String test(@RestQuery String query, @RestHeader String header1,
                @RestHeader String header2,
                @RestHeader Integer header3) {
            StringBuilder result = new StringBuilder("query=");
            result.append(query);
            result.append("/");
            if (header1 != null) {
                result.append("Header1");
                result.append("=");
                result.append(header1);
                result.append(",");
            }
            if (header2 != null) {
                result.append("Header2");
                result.append("=");
                result.append(header2);
                result.append(",");
            }
            if (header3 != null) {
                result.append("Header3");
                result.append("=");
                result.append(header3);
            }
            return result.toString();
        }
    }
}
