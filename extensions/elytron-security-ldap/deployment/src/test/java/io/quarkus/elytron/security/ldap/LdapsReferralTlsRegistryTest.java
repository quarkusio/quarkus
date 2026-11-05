package io.quarkus.elytron.security.ldap;

import java.net.InetAddress;

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
 * The users branch of the first LDAPS server is a referral to a second LDAPS server. Both use a self-signed
 * certificate that is only trusted through the named TLS configuration, so following the referral works only if
 * the connection to the second server uses that configuration as well.
 */
@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ldap-referral", password = "secret", formats = {
        Format.PKCS12, Format.PEM }))
public class LdapsReferralTlsRegistryTest {

    private static InMemoryDirectoryServer referringServer;
    private static InMemoryDirectoryServer usersServer;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SingleRoleSecuredServlet.class, TestApplication.class, RolesEndpointClassLevel.class,
                            ParametrizedPathsResource.class, SubjectExposingResource.class)
                    .addAsResource("ldaps-referral/application.properties", "application.properties"))
            .overrideConfigKey("quarkus.tls.ldap.trust-store.pem.certs", "target/certs/ldap-referral.crt")
            .setBeforeAllCustomizer(LdapsReferralTlsRegistryTest::startServers)
            .setAfterAllCustomizer(LdapsReferralTlsRegistryTest::stopServers);

    private static void startServers() {
        try {
            usersServer = startLdapsServer();
            referringServer = startLdapsServer();
            referringServer.deleteSubtree("ou=Users,dc=quarkus,dc=io");
            referringServer.add("dn: ou=Users,dc=quarkus,dc=io",
                    "objectClass: top",
                    "objectClass: referral",
                    "objectClass: extensibleObject",
                    "ou: Users",
                    "ref: ldaps://localhost:" + usersServer.getListenPort() + "/ou=Users,dc=quarkus,dc=io");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        config.overrideRuntimeConfigKey("quarkus.security.ldap.dir-context.url",
                "ldaps://localhost:" + referringServer.getListenPort());
    }

    private static InMemoryDirectoryServer startLdapsServer() throws Exception {
        SSLUtil sslUtil = new SSLUtil(
                new KeyStoreKeyManager("target/certs/ldap-referral-keystore.p12", "secret".toCharArray(), "PKCS12", null),
                null);
        InMemoryListenerConfig listenerConfig = InMemoryListenerConfig.createLDAPSConfig("ldaps",
                InetAddress.getLoopbackAddress(), 0, sslUtil.createSSLServerSocketFactory(), null);
        InMemoryDirectoryServerConfig serverConfig = new InMemoryDirectoryServerConfig("dc=quarkus,dc=io");
        serverConfig.setListenerConfigs(listenerConfig);
        serverConfig.addAdditionalBindCredentials("uid=admin,ou=system", "secret");
        InMemoryDirectoryServer server = new InMemoryDirectoryServer(serverConfig);
        server.importFromLDIF(true, new LDIFReader(ClassLoader.getSystemResourceAsStream("quarkus-io.ldif")));
        server.startListening();
        return server;
    }

    private static void stopServers() {
        if (referringServer != null) {
            referringServer.shutDown(false);
            referringServer = null;
        }
        if (usersServer != null) {
            usersServer.shutDown(false);
            usersServer = null;
        }
    }

    @Test
    public void testUserBehindReferralIsAuthenticated() {
        RestAssured.given().auth().preemptive().basic("standardUser", "standardUserPassword")
                .when().get("/servlet-secured").then()
                .statusCode(200);
    }

    @Test
    public void testRolesOfUserBehindReferral() {
        RestAssured.given().auth().preemptive().basic("standardUser", "standardUserPassword")
                .when().get("/jaxrs-secured/roles-class").then()
                .statusCode(200);
    }
}
