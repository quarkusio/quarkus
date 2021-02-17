package io.quarkus.resteasy.reactive.client.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ReadTimeoutTest {

    @TestHTTPResource
    URI uri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Client.class, Resource.class));

    @Test
    void shouldTimeoutIfReadTimeoutSetShort() {
        Client client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .readTimeout(1, TimeUnit.SECONDS)
                .build(Client.class);

        RuntimeException exception = assertThrows(RuntimeException.class, client::slow);
        assertThat(exception).hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void shouldNotTimeoutOnFastResponse() {
        Client client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .readTimeout(1, TimeUnit.SECONDS)
                .build(Client.class);

        assertThat(client.fast()).isEqualTo("fast-response");
    }

    @Test
    void shouldNotTimeoutOnDefaultTimeout() {
        Client client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .build(Client.class);

        assertThat(client.slow()).isEqualTo("slow-response");
    }
}
