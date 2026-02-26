package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.client.api.LoggingScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ClientMaskedHeaderLogTest {

    public static class NoMaskedHeadersTest {
        @RegisterExtension
        static final QuarkusUnitTest config = new QuarkusUnitTest()
                .withApplicationRoot((jar) -> jar
                        .addClass(ClientMaskedHeaderLogTest.Resource.class)
                        .addClass(ClientMaskedHeaderLogTest.class))
                .setLogRecordPredicate(record -> record.getLoggerName()
                        .equalsIgnoreCase("org.jboss.resteasy.reactive.client.logging.DefaultClientLogger"))
                .withConfiguration("""
                        quarkus.rest-client.my-client.url=http://localhost:${quarkus.http.test-port:8081}
                        quarkus.rest-client.logging.scope=request-response
                        """).assertLogRecords(records -> {
                    Formatter formatter = new PatternFormatter("[%p] %m");
                    List<String> lines = records.stream().map(formatter::format).map(String::trim).collect(Collectors.toList());

                    assertThat(lines).containsExactly(
                            "[INFO] Request: GET http://localhost:8081/resource/hello Headers[Accept=text/plain;charset=UTF-8 Authorization=123 User-Agent=Quarkus REST Client x-requested-locale=en-US], Empty body",
                            "[INFO] Response: GET http://localhost:8081/resource/hello, Status[200 OK], Headers[x-locale=de-DE x-secret=super-sensitive-value content-length=0], Body:");
                });

        @RestClient
        Client client;

        @Test
        public void test() {
            client.hello("123", "en-US");
        }
    }

    public static class GlobalConfigMaskedHeadersTest {
        @RegisterExtension
        static final QuarkusUnitTest config = new QuarkusUnitTest()
                .withApplicationRoot((jar) -> jar
                        .addClass(ClientMaskedHeaderLogTest.Resource.class)
                        .addClass(ClientMaskedHeaderLogTest.class))
                .setLogRecordPredicate(record -> record.getLoggerName()
                        .equalsIgnoreCase("org.jboss.resteasy.reactive.client.logging.DefaultClientLogger"))
                .withConfiguration("""
                        quarkus.rest-client.my-client.url=http://localhost:${quarkus.http.test-port:8081}
                        quarkus.rest-client.logging.scope=request-response
                        quarkus.rest-client.logging.masked-headers=x-secret,Authorization
                        """).assertLogRecords(records -> {
                    Formatter formatter = new PatternFormatter("[%p] %m");
                    List<String> lines = records.stream().map(formatter::format).map(String::trim).collect(Collectors.toList());

                    assertThat(lines).containsExactly(
                            "[INFO] Request: GET http://localhost:8081/resource/hello Headers[Accept=text/plain;charset=UTF-8 Authorization=**** User-Agent=Quarkus REST Client x-requested-locale=en-US], Empty body",
                            "[INFO] Response: GET http://localhost:8081/resource/hello, Status[200 OK], Headers[x-locale=de-DE x-secret=**** content-length=0], Body:");
                });

        @RestClient
        Client client;

        @Test
        public void test() {
            client.hello("123", "en-US");
        }
    }

    public static class ClientBuilderConfigMaskedHeadersTest {
        @RegisterExtension
        static final QuarkusUnitTest config = new QuarkusUnitTest()
                .withApplicationRoot((jar) -> jar
                        .addClass(ClientMaskedHeaderLogTest.Resource.class)
                        .addClass(ClientMaskedHeaderLogTest.class))
                .setLogRecordPredicate(record -> record.getLoggerName()
                        .equalsIgnoreCase("org.jboss.resteasy.reactive.client.logging.DefaultClientLogger"))
                .withConfiguration("""
                        quarkus.rest-client.my-client.url=http://localhost:${quarkus.http.test-port:8081}
                        """).assertLogRecords(records -> {
                    Formatter formatter = new PatternFormatter("[%p] %m");
                    List<String> lines = records.stream().map(formatter::format).map(String::trim).collect(Collectors.toList());

                    assertThat(lines).containsExactly(
                            "[INFO] Request: GET http://localhost:8081/resource/hello Headers[Accept=text/plain;charset=UTF-8 Authorization=**** User-Agent=Quarkus REST Client x-requested-locale=en-US], Empty body",
                            "[INFO] Response: GET http://localhost:8081/resource/hello, Status[200 OK], Headers[x-locale=de-DE x-secret=**** content-length=0], Body:");
                });

        @ConfigProperty(name = "quarkus.http.test-port", defaultValue = "8081")
        int testPort;

        @Test
        public void test() {
            Client client = QuarkusRestClientBuilder.newBuilder().baseUri(URI.create("http://localhost:%s".formatted(testPort)))
                    .loggingScope(LoggingScope.REQUEST_RESPONSE).loggingMaskedHeaders(Set.of("x-secret", "Authorization"))
                    .build(Client.class);

            client.hello("123", "en-US");
        }
    }

    public static class RestClientConfigMaskedHeadersTest {
        @RegisterExtension
        static final QuarkusUnitTest config = new QuarkusUnitTest()
                .withApplicationRoot((jar) -> jar
                        .addClass(ClientMaskedHeaderLogTest.Resource.class)
                        .addClass(ClientMaskedHeaderLogTest.class))
                .setLogRecordPredicate(record -> record.getLoggerName()
                        .equalsIgnoreCase("org.jboss.resteasy.reactive.client.logging.DefaultClientLogger"))
                .withConfiguration("""
                        quarkus.rest-client.my-client.url=http://localhost:${quarkus.http.test-port:8081}
                        quarkus.rest-client.my-client.logging.scope=request-response
                        quarkus.rest-client.my-client.logging.masked-headers=x-secret,Authorization
                        """).assertLogRecords(records -> {
                    Formatter formatter = new PatternFormatter("[%p] %m");
                    List<String> lines = records.stream().map(formatter::format).map(String::trim).collect(Collectors.toList());

                    assertThat(lines).containsExactly(
                            "[INFO] Request: GET http://localhost:8081/resource/hello Headers[Accept=text/plain;charset=UTF-8 Authorization=**** User-Agent=Quarkus REST Client x-requested-locale=en-US], Empty body",
                            "[INFO] Response: GET http://localhost:8081/resource/hello, Status[200 OK], Headers[x-locale=de-DE x-secret=**** content-length=0], Body:");
                });

        @RestClient
        Client client;

        @Test
        public void test() {
            client.hello("123", "en-US");
        }
    }

    @Path("resource")
    @RegisterRestClient(configKey = "my-client")
    public interface Client {

        @Path("/hello")
        @GET
        String hello(@HeaderParam("Authorization") String auth, @HeaderParam("x-requested-locale") String requestedLocale);
    }

    @Path("resource")
    public static class Resource {

        @GET
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public RestResponse<Object> hello() {
            return RestResponse.ResponseBuilder.ok().header("x-secret", "super-sensitive-value")
                    .header("x-locale", "de-DE").build();
        }
    }
}
