package io.quarkus.vault;

import static io.quarkus.vault.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.vault.CredentialsProvider.USER_PROPERTY_NAME;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.APPROLE;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.USERPASS;
import static io.quarkus.vault.test.VaultTestExtension.APP_SECRET_PATH;
import static io.quarkus.vault.test.VaultTestExtension.DB_PASSWORD;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_KEY;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_PATH_V1;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_PATH_V2;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_VALUE;
import static io.quarkus.vault.test.VaultTestExtension.VAULT_AUTH_APPROLE;
import static io.quarkus.vault.test.VaultTestExtension.VAULT_AUTH_USERPASS_PASSWORD;
import static io.quarkus.vault.test.VaultTestExtension.VAULT_AUTH_USERPASS_USER;
import static io.quarkus.vault.test.VaultTestExtension.VAULT_DBROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.runtime.VaultManager;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.dto.VaultAppRoleAuth;
import io.quarkus.vault.runtime.client.dto.VaultDatabaseCredentials;
import io.quarkus.vault.runtime.client.dto.VaultKvSecretV1;
import io.quarkus.vault.runtime.client.dto.VaultKvSecretV2;
import io.quarkus.vault.runtime.client.dto.VaultLeasesLookup;
import io.quarkus.vault.runtime.client.dto.VaultLookupSelf;
import io.quarkus.vault.runtime.client.dto.VaultRenewLease;
import io.quarkus.vault.runtime.client.dto.VaultRenewSelf;
import io.quarkus.vault.runtime.client.dto.VaultUserPassAuth;
import io.quarkus.vault.runtime.config.VaultAuthenticationType;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS)
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultITCase {

    private static final Logger log = Logger.getLogger(VaultITCase.class);

    public static final String MY_PASSWORD = "my-password";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-vault.properties", "application.properties"));

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @Inject
    CredentialsProvider credentialsProvider;

    @ConfigProperty(name = PASSWORD_PROPERTY_NAME)
    String someSecret;

    @ConfigProperty(name = MY_PASSWORD)
    String someSecretThroughIndirection;

    @Test
    public void credentialsProvider() {
        Properties staticCredentials = credentialsProvider.getCredentials("static");
        assertEquals("{" + PASSWORD_PROPERTY_NAME + "=" + DB_PASSWORD + "}", staticCredentials.toString());

        Properties dynamicCredentials = credentialsProvider.getCredentials("dynamic");
        String username = dynamicCredentials.getProperty(USER_PROPERTY_NAME);
        assertTrue(username.startsWith("v-" + USERPASS.name().toLowerCase() + "-" + VAULT_DBROLE + "-"));
    }

    @Test
    public void config() {
        assertEquals(DB_PASSWORD, someSecret);

        Config config = ConfigProviderResolver.instance().getConfig();
        String value = config.getValue(PASSWORD_PROPERTY_NAME, String.class);
        assertEquals(DB_PASSWORD, value);
    }

    @Test
    public void configPropertyIndirection() {
        assertEquals(DB_PASSWORD, someSecretThroughIndirection);

        Config config = ConfigProviderResolver.instance().getConfig();
        String value = config.getValue(MY_PASSWORD, String.class);
        assertEquals(DB_PASSWORD, value);
    }

    @Test
    public void secretV1() {
        Map<String, String> secrets = kvSecretEngine.readSecret(APP_SECRET_PATH);
        assertEquals("{" + SECRET_KEY + "=" + SECRET_VALUE + "}", secrets.toString());
    }

    @Test
    public void httpclient() {

        VaultClient vaultClient = VaultManager.getInstance().getVaultClient();

        String appRoleRoleId = System.getProperty("quarkus.vault.authentication.app-role.role-id");
        String appRoleSecretId = System.getProperty("quarkus.vault.authentication.app-role.secret-id");
        VaultAppRoleAuth vaultAppRoleAuth = vaultClient.loginAppRole(appRoleRoleId, appRoleSecretId);
        String appRoleClientToken = vaultAppRoleAuth.auth.clientToken;
        assertNotNull(appRoleClientToken);
        log.info("appRoleClientToken = " + appRoleClientToken);

        assertTokenAppRole(vaultClient, appRoleClientToken);
        assertKvSecrets(vaultClient, appRoleClientToken);
        assertDynamicCredentials(vaultClient, appRoleClientToken, APPROLE);

        VaultUserPassAuth vaultUserPassAuth = vaultClient.loginUserPass(VAULT_AUTH_USERPASS_USER, VAULT_AUTH_USERPASS_PASSWORD);
        String userPassClientToken = vaultUserPassAuth.auth.clientToken;
        log.info("userPassClientToken = " + userPassClientToken);
        assertNotNull(userPassClientToken);

        assertTokenUserPass(vaultClient, userPassClientToken);
        assertKvSecrets(vaultClient, userPassClientToken);
        assertDynamicCredentials(vaultClient, userPassClientToken, USERPASS);
    }

    private void assertDynamicCredentials(VaultClient vaultClient, String clientToken, VaultAuthenticationType authType) {
        VaultDatabaseCredentials vaultDatabaseCredentials = vaultClient.generateDatabaseCredentials(clientToken, VAULT_DBROLE);
        String username = vaultDatabaseCredentials.data.username;
        assertTrue(username.startsWith("v-" + authType.name().toLowerCase() + "-" + VAULT_DBROLE + "-"));

        VaultLeasesLookup vaultLeasesLookup = vaultClient.lookupLease(clientToken, vaultDatabaseCredentials.leaseId);
        assertEquals(vaultDatabaseCredentials.leaseId, vaultLeasesLookup.data.id);

        VaultRenewLease vaultRenewLease = vaultClient.renewLease(clientToken, vaultDatabaseCredentials.leaseId);
        assertEquals(vaultDatabaseCredentials.leaseId, vaultRenewLease.leaseId);
    }

    private void assertKvSecrets(VaultClient vaultClient, String clientToken) {
        VaultKvSecretV1 secretV1 = vaultClient.getSecretV1(clientToken, SECRET_PATH_V1, APP_SECRET_PATH);
        assertEquals(SECRET_VALUE, secretV1.data.get(SECRET_KEY));

        VaultKvSecretV2 secretV2 = vaultClient.getSecretV2(clientToken, SECRET_PATH_V2, APP_SECRET_PATH);
        assertEquals(SECRET_VALUE, secretV2.data.data.get(SECRET_KEY));
    }

    private void assertTokenUserPass(VaultClient vaultClient, String clientToken) {
        VaultLookupSelf vaultLookupSelf = vaultClient.lookupSelf(clientToken);
        assertEquals("auth/" + USERPASS.name().toLowerCase() + "/login/" + VAULT_AUTH_USERPASS_USER, vaultLookupSelf.data.path);

        VaultRenewSelf vaultRenewSelf = vaultClient.renewSelf(clientToken, "1h");
        assertEquals(VAULT_AUTH_USERPASS_USER, vaultRenewSelf.auth.metadata.get("username"));
    }

    private void assertTokenAppRole(VaultClient vaultClient, String clientToken) {
        VaultLookupSelf vaultLookupSelf = vaultClient.lookupSelf(clientToken);
        assertEquals("auth/approle/login", vaultLookupSelf.data.path);

        VaultRenewSelf vaultRenewSelf = vaultClient.renewSelf(clientToken, "1h");
        assertEquals(VAULT_AUTH_APPROLE, vaultRenewSelf.auth.metadata.get("role_name"));
    }

}
