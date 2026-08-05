package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.Formatter;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class GlobalConfigMaskedHeadersTest extends AbstractClientMaskedHeaderLogTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(AbstractClientMaskedHeaderLogTest.Resource.class)
                    .addClass(AbstractClientMaskedHeaderLogTest.class))
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
                        "[INFO] Request: GET http://localhost:8081/resource/hello Headers[Accept=text/plain;charset=UTF-8 Authorization=<hidden> User-Agent=Quarkus REST Client x-requested-locale=en-US], Empty body",
                        "[INFO] Response: GET http://localhost:8081/resource/hello, Status[200 OK], Headers[x-locale=de-DE x-secret=<hidden> content-length=0], Body:");
            });

    @RestClient
    Client client;

    @Test
    public void test() {
        client.hello("123", "en-US");
    }
}
