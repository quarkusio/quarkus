package io.quarkus.elytron.security.ldap.it;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldif.LDIFReader;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;

/**
 * Starts an LDAPS server with a self-signed certificate, trusted only through the {@code ldap} TLS configuration.
 */
public class LdapsServerTestResource implements QuarkusTestResourceLifecycleManager {

    private InMemoryDirectoryServer ldapsServer;

    @Override
    public Map<String, String> start() {
        try {
            Path certs = Path.of("target/certs").toAbsolutePath();
            Files.createDirectories(certs);
            new CertificateGenerator(certs, true).generate(new CertificateRequest().withName("ldap")
                    .withFormats(List.of(Format.PKCS12, Format.PEM)).withPassword("secret"));
            SSLUtil sslUtil = new SSLUtil(new KeyStoreKeyManager(certs.resolve("ldap-keystore.p12").toFile(),
                    "secret".toCharArray(), "PKCS12", null), null);
            InMemoryListenerConfig listenerConfig = InMemoryListenerConfig.createLDAPSConfig("ldaps",
                    InetAddress.getLoopbackAddress(), 0, sslUtil.createSSLServerSocketFactory(), null);
            InMemoryDirectoryServerConfig serverConfig = new InMemoryDirectoryServerConfig("dc=quarkus,dc=io");
            serverConfig.setListenerConfigs(listenerConfig);
            serverConfig.addAdditionalBindCredentials("uid=admin,ou=system", "secret");
            ldapsServer = new InMemoryDirectoryServer(serverConfig);
            ldapsServer.importFromLDIF(true, new LDIFReader(ClassLoader.getSystemResourceAsStream("quarkus-io.ldif")));
            ldapsServer.startListening();
            return Map.of(
                    "quarkus.security.ldap.dir-context.url", "ldaps://localhost:" + ldapsServer.getListenPort(),
                    "quarkus.security.ldap.dir-context.tls-configuration-name", "ldap",
                    "quarkus.tls.ldap.trust-store.pem.certs", certs.resolve("ldap.crt").toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void stop() {
        if (ldapsServer != null) {
            ldapsServer.shutDown(false);
            ldapsServer = null;
        }
    }
}
