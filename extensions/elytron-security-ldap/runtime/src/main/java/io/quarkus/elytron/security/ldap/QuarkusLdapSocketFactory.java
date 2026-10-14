package io.quarkus.elytron.security.ldap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Set;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.net.ClientSSLOptions;

/**
 * Creates the sockets of the LDAP connections from the TLS configuration named by
 * {@code quarkus.security.ldap.dir-context.tls-configuration-name}.
 * <p>
 * JNDI instantiates the socket factory from its class name, through {@link #getDefault()}, for the initial context
 * and for every context it creates later on, such as the ones used to follow referrals. The TLS settings are
 * therefore kept in a static field, and replaced when the TLS configuration is reloaded.
 */
public class QuarkusLdapSocketFactory extends SocketFactory {

    private static final QuarkusLdapSocketFactory INSTANCE = new QuarkusLdapSocketFactory();

    private static volatile String tlsConfigurationName;
    private static volatile Settings settings;

    private record Settings(SSLSocketFactory socketFactory, String[] protocols, String[] cipherSuites) {
    }

    public static SocketFactory getDefault() {
        return INSTANCE;
    }

    static void configure(String name, TlsConfiguration tlsConfiguration) {
        SSLSocketFactory socketFactory;
        try {
            socketFactory = tlsConfiguration.createSSLContext().getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create the SSL context of the TLS configuration '" + name
                    + "' for the LDAP connections", e);
        }
        ClientSSLOptions options = tlsConfiguration.getClientSSLOptions();
        tlsConfigurationName = name;
        settings = new Settings(socketFactory, toArray(options.getEnabledSecureTransportProtocols()),
                toArray(options.getEnabledCipherSuites()));
    }

    static void reload(String name, TlsConfiguration tlsConfiguration) {
        if (name.equals(tlsConfigurationName)) {
            configure(name, tlsConfiguration);
        }
    }

    private static String[] toArray(Set<String> values) {
        return values == null || values.isEmpty() ? null : values.toArray(new String[0]);
    }

    @Override
    public Socket createSocket() throws IOException {
        Settings current = settings();
        return configure(current.socketFactory().createSocket(), current);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Settings current = settings();
        return configure(current.socketFactory().createSocket(host, port), current);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        Settings current = settings();
        return configure(current.socketFactory().createSocket(host, port, localHost, localPort), current);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Settings current = settings();
        return configure(current.socketFactory().createSocket(host, port), current);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        Settings current = settings();
        return configure(current.socketFactory().createSocket(address, port, localAddress, localPort), current);
    }

    private static Settings settings() {
        Settings current = settings;
        if (current == null) {
            throw new IllegalStateException("No TLS configuration has been set for the LDAP connections");
        }
        return current;
    }

    private static Socket configure(Socket socket, Settings current) {
        if (socket instanceof SSLSocket sslSocket) {
            if (current.protocols() != null) {
                sslSocket.setEnabledProtocols(current.protocols());
            }
            if (current.cipherSuites() != null) {
                sslSocket.setEnabledCipherSuites(current.cipherSuites());
            }
        }
        return socket;
    }
}
