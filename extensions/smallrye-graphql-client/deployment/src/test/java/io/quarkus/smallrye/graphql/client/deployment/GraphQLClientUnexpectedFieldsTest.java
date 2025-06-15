package io.quarkus.smallrye.graphql.client.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.graphql.client.GraphQLClient;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

/**
 * This test verifies that if
 * <code>quarkus.smallrye-graphql-client.CLIENT_NAME.allow-unexpected-response-fields=true</code> is set, then the
 * client , instead of throwing an exception, logs a warning if it receives an unexpected top-level field in a response.
 */
public class GraphQLClientUnexpectedFieldsTest {

    static String url = "http://" + System.getProperty("quarkus.http.host", "localhost") + ":"
            + System.getProperty("quarkus.http.test-port", "8081") + "/invalid-graphql-endpoint";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addAsResource(
                    new StringAsset("quarkus.smallrye-graphql-client.client1.url=" + url + "\n"
                            + "quarkus.smallrye-graphql-client.client1.allow-unexpected-response-fields=true\n"),
                    "application.properties")
            .addClass(MockGraphQLEndpoint.class).addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Path("/invalid-graphql-endpoint")
    @Produces("application/json+graphql")
    @Consumes("application/json")
    public static class MockGraphQLEndpoint {
        @POST
        public String returnInvalidResponse() {
            return "{\n" + "  \"data\": {\n" + "    \"number\": 32\n" + "  },\n" + "  \"bugs\": {\n" + "  }\n" + "}";
        }
    }

    @Inject
    @GraphQLClient("client1")
    DynamicGraphQLClient client;

    @Test
    public void ignoringUnexpectedResponseField() throws Exception {
        Response response = client.executeSync("query {something}}");
        assertEquals(32, response.getObject(Long.class, "number"));
    }
}
