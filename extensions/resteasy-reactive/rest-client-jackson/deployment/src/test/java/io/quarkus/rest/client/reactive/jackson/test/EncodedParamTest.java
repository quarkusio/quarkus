package io.quarkus.rest.client.reactive.jackson.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class EncodedParamTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(GitLabClient.class, MyResource.class))
            .overrideConfigKey("quarkus.rest-client.my-client.url", "http://localhost:${quarkus.http.test-port:8081}");

    @RestClient
    GitLabClient gitLabClient;

    @Test
    void shouldEncodeParam() {
        String expected = "src/main/resources/messages/test1_en_GB.properties";
        assertEquals(expected, gitLabClient.getRawFile(123, expected));

    }

    @Path("/api/v4")
    @RegisterRestClient(configKey = "my-client")
    public interface GitLabClient {
        @GET
        @Path("/projects/{projectId}/repository/files/{filePath:.+}/raw")
        @Produces(MediaType.TEXT_PLAIN)
        String getRawFile(@PathParam("projectId") Integer projectId, @PathParam("filePath") @Encoded String filePath);
    }

    @Path("/api/v4")
    public static class MyResource {

        @GET
        @Path("/projects/{projectId}/repository/files/{filePath:.+}/raw")
        @Produces(MediaType.TEXT_PLAIN)
        public String getRawFile(@PathParam("projectId") Integer projectId, @PathParam("filePath") @Encoded String filePath) {
            return filePath;
        }

    }
}
