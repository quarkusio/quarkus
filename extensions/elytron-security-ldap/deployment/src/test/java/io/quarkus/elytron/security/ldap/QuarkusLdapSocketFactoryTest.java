package io.quarkus.elytron.security.ldap;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.Socket;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.junit.jupiter.api.Test;

import io.quarkus.tls.BaseTlsConfiguration;
import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.net.ClientSSLOptions;

public class QuarkusLdapSocketFactoryTest {

    @Test
    public void testProtocolsAndCipherSuitesOfTheTlsConfigurationAreApplied() throws Exception {
        QuarkusLdapSocketFactory.configure("ldap", tlsConfiguration(Set.of("TLSv1.2"),
                Set.of("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256")));

        try (Socket socket = QuarkusLdapSocketFactory.getDefault().createSocket()) {
            SSLSocket sslSocket = (SSLSocket) socket;
            assertThat(sslSocket.getEnabledProtocols()).containsExactly("TLSv1.2");
            assertThat(sslSocket.getEnabledCipherSuites()).containsExactly("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
        }
    }

    @Test
    public void testOnlyTheConfiguredTlsConfigurationIsReloaded() throws Exception {
        QuarkusLdapSocketFactory.configure("ldap", tlsConfiguration(Set.of("TLSv1.2"), Set.of()));

        QuarkusLdapSocketFactory.reload("other", tlsConfiguration(Set.of("TLSv1.3"), Set.of()));
        try (Socket socket = QuarkusLdapSocketFactory.getDefault().createSocket()) {
            assertThat(((SSLSocket) socket).getEnabledProtocols()).containsExactly("TLSv1.2");
        }

        QuarkusLdapSocketFactory.reload("ldap", tlsConfiguration(Set.of("TLSv1.3"), Set.of()));
        try (Socket socket = QuarkusLdapSocketFactory.getDefault().createSocket()) {
            assertThat(((SSLSocket) socket).getEnabledProtocols()).containsExactly("TLSv1.3");
        }
    }

    private static TlsConfiguration tlsConfiguration(Set<String> protocols, Set<String> cipherSuites) {
        return new BaseTlsConfiguration() {
            @Override
            public SSLContext createSSLContext() throws Exception {
                return SSLContext.getDefault();
            }

            @Override
            public ClientSSLOptions getClientSSLOptions() {
                ClientSSLOptions options = new ClientSSLOptions();
                options.setEnabledSecureTransportProtocols(protocols);
                cipherSuites.forEach(options::addEnabledCipherSuite);
                return options;
            }
        };
    }
}
