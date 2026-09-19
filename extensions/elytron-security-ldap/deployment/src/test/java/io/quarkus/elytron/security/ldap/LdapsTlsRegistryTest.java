package io.quarkus.elytron.security.ldap;

import java.net.InetAddress;

import javax.net.ssl.SSLServerSocketFactory;

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
 * The LDAP server listens over LDAPS with a certificate signed by a private CA, trusted through a named TLS
 * configuration of the TLS registry. The LDAPS server is started here rather than through a test resource because
 * the module-wide {@code LdapServerTestResource} also sets the dir-context URL.
 */
@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ldap", password = "secret", formats = {
        Format.PKCS12, Format.PEM }))
public class LdapsTlsRegistryTest {

    private static InMemoryDirectoryServer ldapsServer;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SingleRoleSecuredServlet.class, TestApplication.class, RolesEndpointClassLevel.class,
                            ParametrizedPathsResource.class, SubjectExposingResource.class)
                    .addAsResource("ldaps-config/application.properties", "application.properties"))
            .setBeforeAllCustomizer(LdapsTlsRegistryTest::startLdapsServer)
            .setAfterAllCustomizer(LdapsTlsRegistryTest::stopLdapsServer);

    private static void startLdapsServer() {
        try {
            SSLUtil sslUtil = new SSLUtil(
                    new KeyStoreKeyManager("target/certs/ldap-keystore.p12", "secret".toCharArray(), "PKCS12", null),
                    null);
            SSLServerSocketFactory serverSocketFactory = sslUtil.createSSLServerSocketFactory();
            InMemoryListenerConfig listenerConfig = InMemoryListenerConfig.createLDAPSConfig("ldaps",
                    InetAddress.getLoopbackAddress(), 0, serverSocketFactory, null);
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
    public void testSecureAccessFailure() {
        RestAssured.given().redirects().follow(false).get("/servlet-secured").then().statusCode(401);
    }

    @Test
    public void testSecureAccessSuccessOverLdaps() {
        RestAssured.given().auth().preemptive().basic("standardUser", "standardUserPassword")
                .when().get("/servlet-secured").then()
                .statusCode(200);
    }

    @Test
    public void testJaxrsGetRoleSuccessOverLdaps() {
        RestAssured.given().auth().preemptive().basic("standardUser", "standardUserPassword")
                .when().get("/jaxrs-secured/roles-class").then()
                .statusCode(200);
    }
}
