package io.quarkus.vertx.http.runtime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.Future;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.ServerSSLOptions;

class HttpCertificateUpdateEventListenerTest {

    @Test
    void failedServerUpdateDoesNotPropagateToTheSender() {
        TlsConfiguration tlsConfig = mock(TlsConfiguration.class);
        when(tlsConfig.getServerSSLOptions()).thenAnswer(i -> new ServerSSLOptions());
        HttpServer server = mock(HttpServer.class);
        HttpCertificateUpdateEventListener listener = new HttpCertificateUpdateEventListener();
        listener.register(server, "my-tls", "main", ClientAuth.NONE);
        CertificateUpdatedEvent event = new CertificateUpdatedEvent("my-tls", tlsConfig);

        when(server.updateSSLOptions(any(ServerSSLOptions.class)))
                .thenReturn(Future.failedFuture(new RuntimeException("boom")));
        // Logged, not thrown: an exception would stop the other CertificateUpdatedEvent observers (clients, OIDC...)
        assertThatCode(() -> listener.onCertificateUpdate(event)).doesNotThrowAnyException();
        verify(server, times(1)).updateSSLOptions(any(ServerSSLOptions.class));
    }
}
