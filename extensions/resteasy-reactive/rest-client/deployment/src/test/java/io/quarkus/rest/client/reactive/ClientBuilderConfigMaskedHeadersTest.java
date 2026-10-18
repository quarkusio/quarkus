package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.resteasy.reactive.client.api.LoggingScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class ClientBuilderConfigMaskedHeadersTest extends AbstractClientMaskedHeaderLogTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(AbstractClientMaskedHeaderLogTest.Resource.class)
                    .addClass(AbstractClientMaskedHeaderLogTest.class))
            .setLogRecordPredicate(record -> record.getLoggerName()
                    .equalsIgnoreCase("org.jboss.resteasy.reactive.client.logging.DefaultClientLogger"))
            .withConfiguration("""
                    quarkus.rest-client.my-client.url=http://localhost:${quarkus.http.test-port:8081}
                    """).assertLogRecords(records -> {
                Formatter formatter = new PatternFormatter("[%p] %m");
                List<String> lines = records.stream().map(formatter::format).map(String::trim).collect(Collectors.toList());

                assertThat(lines).containsExactly(
                        "[INFO] Request: GET http://localhost:8081/resource/hello Headers[Accept=text/plain;charset=UTF-8 Authorization=<hidden> User-Agent=Quarkus REST Client x-requested-locale=en-US], Empty body",
                        "[INFO] Response: GET http://localhost:8081/resource/hello, Status[200 OK], Headers[x-locale=de-DE x-secret=<hidden> content-length=0], Body:");
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
