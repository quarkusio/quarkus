package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.LogRecord;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.MDC;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * The request and response logs of a REST Client invoked while serving a request must carry the MDC of that request.
 */
public class ClientLogMdcTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, Client.class))
            .setLogRecordPredicate(record -> record.getLoggerName()
                    .equals("org.jboss.resteasy.reactive.client.logging.DefaultClientLogger"))
            .withConfiguration("""
                    quarkus.rest-client.my-client.url=http://localhost:${quarkus.http.test-port:8081}
                    quarkus.rest-client.logging.scope=request-response
                    """)
            .assertLogRecords(records -> {
                assertThat(records).hasSize(2);
                for (LogRecord record : records) {
                    assertThat(((ExtLogRecord) record).getMdc("requestId")).isEqualTo("abc-123");
                }
            });

    @Test
    void clientLogsCarryTheMdcOfTheServedRequest() {
        RestAssured.get("/caller").then().statusCode(200).body(org.hamcrest.Matchers.is("hello"));
    }

    @Path("/")
    public static class Resource {

        @RestClient
        Client client;

        @GET
        @Path("caller")
        @Produces(MediaType.TEXT_PLAIN)
        public String caller() {
            MDC.put("requestId", "abc-123");
            try {
                return client.callee();
            } finally {
                MDC.remove("requestId");
            }
        }

        @GET
        @Path("callee")
        @Produces(MediaType.TEXT_PLAIN)
        public String callee() {
            return "hello";
        }
    }

    @RegisterRestClient(configKey = "my-client")
    @Path("/")
    public interface Client {

        @GET
        @Path("callee")
        String callee();
    }
}
