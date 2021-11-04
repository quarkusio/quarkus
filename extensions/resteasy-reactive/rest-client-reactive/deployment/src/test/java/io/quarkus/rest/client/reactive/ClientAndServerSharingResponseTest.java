package io.quarkus.rest.client.reactive;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Blocking;

public class ClientAndServerSharingResponseTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class, HeadersService.class));

    @Test
    public void test() {
        when().get("/test/client")
                .then()
                .statusCode(200)
                .body(containsString("{\"Accept\":\"application/json\"}"));
    }

    @RegisterRestClient
    public interface HeadersService {

        @POST
        @Path("test")
        @Produces(MediaType.APPLICATION_JSON)
        Response dumpHeaders();
    }

    @Path("test")
    public static class Endpoint {

        private final ObjectMapper mapper;
        private final HeadersService headersService;

        public Endpoint(ObjectMapper mapper,
                @ConfigProperty(name = "quarkus.http.test-port", defaultValue = "8081") Integer testPort)
                throws MalformedURLException {
            this.mapper = mapper;
            this.headersService = RestClientBuilder.newBuilder()
                    .baseUrl(new URL(String.format("http://localhost:%d", testPort)))
                    .readTimeout(1, TimeUnit.SECONDS)
                    .build(HeadersService.class);
            ;
        }

        @POST
        @Produces(MediaType.APPLICATION_JSON)
        public Response dumpHeaders(@Context HttpHeaders headers) {
            ArrayNode array = mapper.createArrayNode();
            for (Map.Entry<String, List<String>> header : headers.getRequestHeaders().entrySet()) {
                ObjectNode node = mapper.createObjectNode();
                List<String> strings = header.getValue();
                if ((strings == null) || strings.isEmpty()) {
                    continue;
                }
                node.put(header.getKey(), strings.get(0));
                array.add(node);
            }
            return Response.ok(array).build();
        }

        @GET
        @Path("client")
        @Produces(MediaType.APPLICATION_JSON)
        @Blocking
        public Response testCase4() {
            return headersService.dumpHeaders();
        }
    }
}
