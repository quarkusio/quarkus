package io.quarkus.rest.client.reactive.proxy;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.rest.client.reactive.deployment.devservices.VertxHttpProxyDevServicesRestClientProxyProvider;
import io.quarkus.rest.client.reactive.spi.DevServicesRestClientProxyProvider;
import io.quarkus.test.QuarkusUnitTest;

public class HttpProxyDevServicesMultipleCustomProvidersTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(Resource.class, Client.class, Custom1DevServicesRestClientProxyProvider.class,
                            Custom2DevServicesRestClientProxyProvider.class))
            .overrideConfigKey(
                    "quarkus.rest-client.\"io.quarkus.rest.client.reactive.proxy.HttpProxyDevServicesMultipleCustomProvidersTest$Client\".local-proxy-provider",
                    "custom2")
            .overrideConfigKey(
                    "quarkus.rest-client.\"io.quarkus.rest.client.reactive.proxy.HttpProxyDevServicesMultipleCustomProvidersTest$Client\".enable-local-proxy",
                    "true")
            .overrideConfigKey(
                    "quarkus.rest-client.\"io.quarkus.rest.client.reactive.proxy.HttpProxyDevServicesMultipleCustomProvidersTest$Client\".url",
                    "http://localhost:${quarkus.http.test-port:8081}")
            .setLogRecordPredicate(record -> record.getLevel().equals(Level.INFO))
            .assertLogRecords(new Consumer<>() {
                @Override
                public void accept(List<LogRecord> logRecords) {
                    assertThat(logRecords).extracting(LogRecord::getMessage)
                            .anyMatch(message -> message.startsWith("Started custom2 HTTP proxy server") && message.endsWith(
                                    "REST Client 'io.quarkus.rest.client.reactive.proxy.HttpProxyDevServicesMultipleCustomProvidersTest$Client'"));
                }
            })
            .addBuildChainCustomizer(new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(
                                    new DevServicesRestClientProxyProvider.BuildItem(
                                            new Custom1DevServicesRestClientProxyProvider()));
                            context.produce(
                                    new DevServicesRestClientProxyProvider.BuildItem(
                                            new Custom2DevServicesRestClientProxyProvider()));
                        }
                    }).produces(DevServicesRestClientProxyProvider.BuildItem.class).build();
                }
            });

    @ConfigProperty(name = "quarkus.rest-client.\"io.quarkus.rest.client.reactive.proxy.HttpProxyDevServicesMultipleCustomProvidersTest$Client\".override-uri")
    String proxyUrl;

    @Test
    public void test() {
        Client client = QuarkusRestClientBuilder.newBuilder().baseUri(URI.create("http://unused.dev")).build(Client.class);

        // test that the proxy works as expected
        given()
                .baseUri(proxyUrl)
                .get("test/count")
                .then()
                .statusCode(200)
                .body(equalTo("10"));

        // test that the client works as expected
        long result = client.count();
        assertEquals(10, result);
    }

    @Path("test")
    public interface Client {

        @Path("count")
        @GET
        long count();
    }

    @Path("test")
    public static class Resource {

        @GET
        @Path("count")
        public long count() {
            return 10;
        }
    }

    public static class Custom1DevServicesRestClientProxyProvider extends VertxHttpProxyDevServicesRestClientProxyProvider {

        @Override
        public String name() {
            return "custom1";
        }

        @Override
        protected void logStartup(String className, Integer port) {
            log.info("Started custom1 HTTP proxy server on http://localhost:" + port + " for REST Client '" + className + "'");
        }
    }

    // this is tested by having this class provide a different startup log
    public static class Custom2DevServicesRestClientProxyProvider extends VertxHttpProxyDevServicesRestClientProxyProvider {

        @Override
        public String name() {
            return "custom2";
        }

        @Override
        protected void logStartup(String className, Integer port) {
            log.info("Started custom2 HTTP proxy server on http://localhost:" + port + " for REST Client '" + className + "'");
        }
    }
}
