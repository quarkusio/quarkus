package io.quarkus.elytron.security.ldap;

import static org.awaitility.Awaitility.await;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldif.LDIFReader;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;

import io.quarkus.elytron.security.ldap.rest.ParametrizedPathsResource;
import io.quarkus.elytron.security.ldap.rest.RolesEndpointClassLevel;
import io.quarkus.elytron.security.ldap.rest.SingleRoleSecuredServlet;
import io.quarkus.elytron.security.ldap.rest.SubjectExposingResource;
import io.quarkus.elytron.security.ldap.rest.TestApplication;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

/**
 * The trust store of the TLS configuration first holds a certificate that is not the one of the LDAPS server, and
 * is then replaced by the right one. New connections must use the reloaded TLS configuration.
 */
@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "ldap-reload-server", password = "secret", formats = { Format.PKCS12, Format.PEM }),
        @Certificate(name = "ldap-reload-other", password = "secret", formats = { Format.PEM }) })
public class LdapsTlsRegistryReloadTest {

    private static final Path TRUSTED = Path.of("target/certs/ldap-reload-trusted.crt");

    private static InMemoryDirectoryServer ldapsServer;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SingleRoleSecuredServlet.class, TestApplication.class, RolesEndpointClassLevel.class,
                            ParametrizedPathsResource.class, SubjectExposingResource.class)
                    .addAsResource("ldaps-reload/application.properties", "application.properties"))
            .setBeforeAllCustomizer(LdapsTlsRegistryReloadTest::startLdapsServer)
            .setAfterAllCustomizer(LdapsTlsRegistryReloadTest::stopLdapsServer);

    private static void startLdapsServer() {
        try {
            Files.copy(Path.of("target/certs/ldap-reload-other.crt"), TRUSTED, StandardCopyOption.REPLACE_EXISTING);
            SSLUtil sslUtil = new SSLUtil(
                    new KeyStoreKeyManager("target/certs/ldap-reload-server-keystore.p12", "secret".toCharArray(), "PKCS12",
                            null),
                    null);
            InMemoryListenerConfig listenerConfig = InMemoryListenerConfig.createLDAPSConfig("ldaps",
                    InetAddress.getLoopbackAddress(), 0, sslUtil.createSSLServerSocketFactory(), null);
            InMemoryDirectoryServerConfig serverConfig = new InMemoryDirectoryServerConfig("dc=quarkus,dc=io");
            serverConfig.setListenerConfigs(listenerConfig);
            serverConfig.addAdditionalBindCredentials("uid=admin,ou=system", "secret");
            ldapsServer = new InMemoryDirectoryServer(serverConfig);
            ldapsServer.importFromLDIF(true, new LDIFReader(ClassLoader.getSystemResourceAsStream("quarkus-io.ldif")));
            ldapsServer.startListening();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        config.overrideRuntimeConfigKey("quarkus.security.ldap.dir-context.url",
                "ldaps://localhost:" + ldapsServer.getListenPort());
    }

    private static void stopLdapsServer() {
        if (ldapsServer != null) {
            ldapsServer.shutDown(false);
            ldapsServer = null;
        }
    }

    @Test
    public void testReloadedTrustStoreIsUsedForNewConnections() throws Exception {
        RestAssured.given().auth().preemptive().basic("standardUser", "standardUserPassword")
                .when().get("/servlet-secured").then()
                .statusCode(500);

        Files.copy(Path.of("target/certs/ldap-reload-server.crt"), TRUSTED, StandardCopyOption.REPLACE_EXISTING);

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> RestAssured.given().auth().preemptive().basic("standardUser", "standardUserPassword")
                        .when().get("/servlet-secured").then()
                        .statusCode(200));
    }
}
