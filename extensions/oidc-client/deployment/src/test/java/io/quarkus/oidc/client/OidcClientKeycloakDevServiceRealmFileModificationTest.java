package io.quarkus.oidc.client;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * This test uses Dev Services for Keycloak and a realm configured via a realm path property.
 * We verify that when the realm file is not modified, the dev service is not restarted on the app restart.
 * However, if the realm file is modified, we expect that the dev service is restarted.
 */
class OidcClientKeycloakDevServiceRealmFileModificationTest {

    private static final Logger LOG = Logger.getLogger(OidcClientKeycloakDevServiceRealmFileModificationTest.class);

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> {

                // this must be a file as we cannot test detection of the file modification time on a classpath resource
                // so create a file from the realm resource
                File realmFileModifiedDirectory = new File("target/realm-file-modified");
                realmFileModifiedDirectory.mkdirs();
                Path quarkusRealmFilePath = realmFileModifiedDirectory.toPath().resolve("quarkus-realm.json");
                try (InputStream in = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("quarkus-realm.json")) {
                    Files.copy(in, quarkusRealmFilePath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                jar
                        .addClasses(DefaultOidcClientTokenResource.class)
                        .addAsResource(new StringAsset("""
                                quarkus.oidc.enabled=false
                                quarkus.keycloak.devservices.realm-path=target/realm-file-modified/quarkus-realm.json
                                quarkus.oidc-client.client-id=backend-service
                                quarkus.oidc-client.grant.type=password
                                quarkus.oidc-client.grant-options.password.username=alice
                                quarkus.oidc-client.grant-options.password.password=alice
                                """), "application.properties");
            })
            .setLogRecordPredicate(lr -> true);

    @Test
    void testRealmFileModification() {
        assertGetAccessTokenWorks("Hello", "smart");
        assertKeycloakDevServiceStarter(1);
        changeGreeting("Hello ", "Hi ");
        assertGetAccessTokenWorks("Hi", "smart");
        assertKeycloakDevServiceStarter(1);
        changeAliceRealmRoles("smart", "clever");
        assertGetAccessTokenWorks("Hi", "clever");
        assertKeycloakDevServiceStarter(2);
        changeGreeting("Hi ", "Hey ");
        assertGetAccessTokenWorks("Hey", "clever");
        assertKeycloakDevServiceStarter(2);
    }

    private static void changeGreeting(String oldGreeting, String newGreeting) {
        test.modifySourceFile(DefaultOidcClientTokenResource.class,
                classContent -> classContent.replace(oldGreeting, newGreeting));
    }

    private static void changeAliceRealmRoles(String oldRole, String newRole) {
        var realmFilePath = Path.of("target/realm-file-modified/quarkus-realm.json");
        try {
            var realmFileContent = Files.readString(realmFilePath);
            var newRealmFileContent = realmFileContent.replace(oldRole, newRole);
            Files.writeString(realmFilePath, newRealmFileContent, StandardOpenOption.TRUNCATE_EXISTING);
            LOG.infof("Changed 'alice' realm access roles from '%s' to '%s'", oldRole, newRole);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertKeycloakDevServiceStarter(int count) {
        Awaitility.await().atMost(Duration.ofSeconds(15)).until(() -> getKeycloakServiceStartedCount() == count);
    }

    private static void assertGetAccessTokenWorks(String greeting, String expectedRealmRoles) {
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            String[] greetingToToken = RestAssured.given()
                    .get("/client/token")
                    .then()
                    .statusCode(200)
                    .body(notNullValue())
                    .extract().asString().trim().split(" ");
            String accessToken = greetingToToken[1];
            var decodedToken = OidcCommonUtils.decodeJwtContent(accessToken);
            assertNotNull(decodedToken);
            assertEquals("alice", decodedToken.getString("preferred_username"));
            String actualRealmRoles = decodedToken.getJsonObject("realm_access").getString("roles");
            assertEquals("[" + expectedRealmRoles + "]", actualRealmRoles);
            assertEquals(greeting, greetingToToken[0]);
        });
    }

    private static long getKeycloakServiceStartedCount() {
        return test.getLogRecords().stream().filter(m -> m.getMessage().contains("Dev Services for Keycloak started."))
                .count();
    }

}
