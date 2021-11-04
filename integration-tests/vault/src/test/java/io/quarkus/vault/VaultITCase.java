package io.quarkus.vault;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;
import static io.quarkus.vault.runtime.VaultAuthManager.USERPASS_WRAPPING_TOKEN_PASSWORD_KEY;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.APPROLE;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.USERPASS;
import static io.quarkus.vault.test.VaultTestExtension.APP_SECRET_PATH;
import static io.quarkus.vault.test.VaultTestExtension.DB_PASSWORD;
import static io.quarkus.vault.test.VaultTestExtension.ENCRYPTION_KEY_NAME;
import static io.quarkus.vault.test.VaultTestExtension.EXPECTED_SUB_PATHS;
import static io.quarkus.vault.test.VaultTestExtension.LIST_PATH;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_KEY;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_PATH_V1;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_PATH_V2;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_VALUE;
import static io.quarkus.vault.test.VaultTestExtension.SIGN_KEY_NAME;
import static io.quarkus.vault.test.VaultTestExtension.VAULT_AUTH_APPROLE;
import static io.quarkus.vault.test.VaultTestExtension.VAULT_AUTH_USERPASS_PASSWORD;
import static io.quarkus.vault.test.VaultTestExtension.VAULT_AUTH_USERPASS_USER;
import static io.quarkus.vault.test.VaultTestExtension.VAULT_DBROLE;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.runtime.Base64String;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalAppRoleAuthMethod;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalTokenAuthMethod;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalUserpassAuthMethod;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.dto.VaultModel;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultLookupSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserPassAuth;
import io.quarkus.vault.runtime.client.dto.database.VaultDatabaseCredentials;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvListSecrets;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV1;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2;
import io.quarkus.vault.runtime.client.dto.sys.VaultLeasesLookup;
import io.quarkus.vault.runtime.client.dto.sys.VaultRenewLease;
import io.quarkus.vault.runtime.client.dto.sys.VaultWrapResult;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitRewrapBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitRewrapBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSign;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSignBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSignBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerify;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerifyBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerifyBody;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalDatabaseSecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalKvV1SecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalKvV2SecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalTransitSecretEngine;
import io.quarkus.vault.runtime.config.VaultAuthenticationType;
import io.quarkus.vault.runtime.config.VaultConfigSource;
import io.quarkus.vault.test.VaultTestExtension;
import io.quarkus.vault.test.VaultTestLifecycleManager;
import io.quarkus.vault.test.client.TestVaultClient;
import io.quarkus.vault.test.client.dto.VaultTransitHash;
import io.quarkus.vault.test.client.dto.VaultTransitHashBody;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultITCase {

    private static final Logger log = Logger.getLogger(VaultITCase.class);

    public static final String MY_PASSWORD = "my-password";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault.properties", "application.properties"));

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @Inject
    CredentialsProvider credentialsProvider;

    @ConfigProperty(name = PASSWORD_PROPERTY_NAME)
    String someSecret;

    @ConfigProperty(name = MY_PASSWORD)
    String someSecretThroughIndirection;

    @Inject
    VaultInternalKvV1SecretEngine vaultInternalKvV1SecretEngine;
    @Inject
    VaultInternalKvV2SecretEngine vaultInternalKvV2SecretEngine;
    @Inject
    VaultInternalTransitSecretEngine vaultInternalTransitSecretEngine;
    @Inject
    VaultInternalSystemBackend vaultInternalSystemBackend;
    @Inject
    VaultInternalAppRoleAuthMethod vaultInternalAppRoleAuthMethod;
    @Inject
    VaultInternalUserpassAuthMethod vaultInternalUserpassAuthMethod;
    @Inject
    VaultInternalTokenAuthMethod vaultInternalTokenAuthMethod;
    @Inject
    VaultInternalDatabaseSecretEngine vaultInternalDatabaseSecretEngine;

    @Test
    public void credentialsProvider() {
        Map<String, String> staticCredentials = credentialsProvider.getCredentials("static");
        assertEquals("{" + PASSWORD_PROPERTY_NAME + "=" + DB_PASSWORD + "}", staticCredentials.toString());

        Map<String, String> dynamicCredentials = credentialsProvider.getCredentials("dynamic");
        String username = dynamicCredentials.get(USER_PROPERTY_NAME);
        assertTrue(username.startsWith("v-" + USERPASS.name().toLowerCase() + "-" + VAULT_DBROLE + "-"));
    }

    @Test
    public void config() {
        assertEquals(DB_PASSWORD, someSecret);

        Config config = ConfigProviderResolver.instance().getConfig();
        String value = config.getValue(PASSWORD_PROPERTY_NAME, String.class);
        assertEquals(DB_PASSWORD, value);

        int ordinal = StreamSupport.stream(config.getConfigSources().spliterator(), false)
                .filter(cs -> cs instanceof VaultConfigSource)
                .findAny()
                .orElseThrow(() -> new RuntimeException("vault config source not found"))
                .getOrdinal();

        Assertions.assertEquals(300, ordinal);
    }

    @Test
    public void configPropertyIndirection() {
        assertEquals(DB_PASSWORD, someSecretThroughIndirection);

        Config config = ConfigProviderResolver.instance().getConfig();
        String value = config.getValue(MY_PASSWORD, String.class);
        assertEquals(DB_PASSWORD, value);
    }

    @Test
    public void secret() {
        Map<String, String> secrets = kvSecretEngine.readSecret(APP_SECRET_PATH);
        assertEquals("{" + SECRET_KEY + "=" + SECRET_VALUE + "}", secrets.toString());
    }

    @Test
    public void crudSecret() {
        VaultTestExtension.assertCrudSecret(kvSecretEngine);
    }

    static class WrapExample implements VaultModel {
        public String foo = "bar";
        public String zip = "zap";
    }

    @Test
    public void httpclient() {

        String anotherWrappingToken = ConfigProviderResolver.instance().getConfig()
                .getValue("vault-test.another-password-kv-v2-wrapping-token", String.class);
        VaultKvSecretV2 unwrap = vaultInternalSystemBackend.unwrap(anotherWrappingToken, VaultKvSecretV2.class);
        assertEquals(VAULT_AUTH_USERPASS_PASSWORD, unwrap.data.data.get(USERPASS_WRAPPING_TOKEN_PASSWORD_KEY));
        try {
            vaultInternalSystemBackend.unwrap(anotherWrappingToken, VaultKvSecretV2.class);
            fail("expected error 400: wrapping token is not valid or does not exist");
        } catch (VaultClientException e) {
            // fails on second unwrap attempt
            assertEquals(400, e.getStatus());
        }

        String appRoleRoleId = ConfigProviderResolver.instance().getConfig()
                .getValue("vault-test.role-id", String.class);
        String appRoleSecretId = ConfigProviderResolver.instance().getConfig()
                .getValue("vault-test.secret-id", String.class);
        VaultAppRoleAuth vaultAppRoleAuth = vaultInternalAppRoleAuthMethod.login(appRoleRoleId, appRoleSecretId);
        String appRoleClientToken = vaultAppRoleAuth.auth.clientToken;
        assertNotNull(appRoleClientToken);
        log.info("appRoleClientToken = " + appRoleClientToken);

        assertTokenAppRole(appRoleClientToken);
        assertKvSecrets(appRoleClientToken);
        assertDynamicCredentials(appRoleClientToken, APPROLE);
        assertWrap(appRoleClientToken);

        VaultUserPassAuth vaultUserPassAuth = vaultInternalUserpassAuthMethod.login(VAULT_AUTH_USERPASS_USER,
                VAULT_AUTH_USERPASS_PASSWORD);
        String userPassClientToken = vaultUserPassAuth.auth.clientToken;
        log.info("userPassClientToken = " + userPassClientToken);
        assertNotNull(userPassClientToken);

        assertTransit(appRoleClientToken);

        assertTokenUserPass(userPassClientToken);
        assertKvSecrets(userPassClientToken);
        assertDynamicCredentials(userPassClientToken, USERPASS);
        assertWrap(userPassClientToken);
    }

    private void assertWrap(String token) {
        VaultWrapResult wrapResult = vaultInternalSystemBackend.wrap(token, 60, new WrapExample());
        WrapExample unwrapExample = vaultInternalSystemBackend.unwrap(wrapResult.wrapInfo.token, WrapExample.class);
        assertEquals("bar", unwrapExample.foo);
        assertEquals("zap", unwrapExample.zip);
    }

    private void assertTransit(String token) {

        Base64String context = Base64String.from("mycontext");

        assertTransitEncryption(token, ENCRYPTION_KEY_NAME, null);
        assertTransitEncryption(token, ENCRYPTION_KEY_NAME, context);

        assertTransitSign(token, SIGN_KEY_NAME, null);
        assertTransitSign(token, SIGN_KEY_NAME, context);

        new TestVaultClient().rotate(token, ENCRYPTION_KEY_NAME);

        assertHash(token);
    }

    private void assertHash(String token) {
        VaultTransitHashBody body = new VaultTransitHashBody();
        body.input = Base64String.from("coucou");
        body.format = "base64";
        VaultTransitHash hash = new TestVaultClient().hash(token, "sha2-512", body);
        Base64String sum = hash.data.sum;
        String expected = "4FrxOZ9PS+t5NMnxK6WpyI9+4ejvP+ehZ75Ll5xRXSQQKtkNOgdU1I/Fkw9jaaMIfmhulzLvNGDmQ5qVCJtIAA==";
        assertEquals(expected, sum.getValue());
    }

    private void assertTransitSign(String token, String keyName, Base64String context) {

        String data = "coucou";

        VaultTransitSignBody batchBody = new VaultTransitSignBody();
        batchBody.batchInput = singletonList(new VaultTransitSignBatchInput(Base64String.from(data), context));

        VaultTransitSign batchSign = vaultInternalTransitSecretEngine.sign(token, keyName, null, batchBody);

        VaultTransitVerifyBody verifyBody = new VaultTransitVerifyBody();
        VaultTransitVerifyBatchInput batchInput = new VaultTransitVerifyBatchInput(Base64String.from(data), context);
        batchInput.signature = batchSign.data.batchResults.get(0).signature;
        verifyBody.batchInput = singletonList(batchInput);

        VaultTransitVerify verify = vaultInternalTransitSecretEngine.verify(token, keyName, null, verifyBody);
        assertEquals(1, verify.data.batchResults.size());
        assertTrue(verify.data.batchResults.get(0).valid);
    }

    private void assertTransitEncryption(String token, String keyName, Base64String context) {

        String data = "coucou";

        VaultTransitEncryptBatchInput encryptBatchInput = new VaultTransitEncryptBatchInput(Base64String.from(data), context);
        VaultTransitEncryptBody encryptBody = new VaultTransitEncryptBody();
        encryptBody.batchInput = singletonList(encryptBatchInput);
        VaultTransitEncrypt encryptBatchResult = vaultInternalTransitSecretEngine.encrypt(token, keyName, encryptBody);
        String ciphertext = encryptBatchResult.data.batchResults.get(0).ciphertext;

        String batchDecryptedString = decrypt(token, keyName, ciphertext, context);
        assertEquals(data, batchDecryptedString);

        VaultTransitRewrapBatchInput rewrapBatchInput = new VaultTransitRewrapBatchInput(ciphertext, context);
        VaultTransitRewrapBody rewrapBody = new VaultTransitRewrapBody();
        rewrapBody.batchInput = singletonList(rewrapBatchInput);
        VaultTransitEncrypt rewrap = vaultInternalTransitSecretEngine.rewrap(token, keyName, rewrapBody);
        assertEquals(1, rewrap.data.batchResults.size());
        String rewrappedCiphertext = rewrap.data.batchResults.get(0).ciphertext;

        batchDecryptedString = decrypt(token, keyName, rewrappedCiphertext, context);
        assertEquals(data, batchDecryptedString);
    }

    private String decrypt(String token, String keyName, String ciphertext, Base64String context) {
        VaultTransitDecryptBatchInput decryptBatchInput = new VaultTransitDecryptBatchInput(ciphertext, context);
        VaultTransitDecryptBody decryptBody = new VaultTransitDecryptBody();
        decryptBody.batchInput = singletonList(decryptBatchInput);
        VaultTransitDecrypt decryptBatchResult = vaultInternalTransitSecretEngine.decrypt(token, keyName, decryptBody);
        return decryptBatchResult.data.batchResults.get(0).plaintext.decodeAsString();
    }

    private void assertDynamicCredentials(String clientToken, VaultAuthenticationType authType) {
        VaultDatabaseCredentials vaultDatabaseCredentials = vaultInternalDatabaseSecretEngine.generateCredentials(clientToken,
                VAULT_DBROLE);
        String username = vaultDatabaseCredentials.data.username;
        assertTrue(username.startsWith("v-" + authType.name().toLowerCase() + "-" + VAULT_DBROLE + "-"));

        VaultLeasesLookup vaultLeasesLookup = vaultInternalSystemBackend.lookupLease(clientToken,
                vaultDatabaseCredentials.leaseId);
        assertEquals(vaultDatabaseCredentials.leaseId, vaultLeasesLookup.data.id);

        VaultRenewLease vaultRenewLease = vaultInternalSystemBackend.renewLease(clientToken, vaultDatabaseCredentials.leaseId);
        assertEquals(vaultDatabaseCredentials.leaseId, vaultRenewLease.leaseId);
    }

    private void assertKvSecrets(String clientToken) {
        VaultKvSecretV1 secretV1 = vaultInternalKvV1SecretEngine.getSecret(clientToken, SECRET_PATH_V1, APP_SECRET_PATH);
        assertEquals(SECRET_VALUE, secretV1.data.get(SECRET_KEY));
        VaultKvListSecrets vaultKvListSecretsV1 = vaultInternalKvV1SecretEngine.listSecrets(clientToken, SECRET_PATH_V1,
                LIST_PATH);
        assertEquals(EXPECTED_SUB_PATHS, vaultKvListSecretsV1.data.keys.toString());

        VaultKvSecretV2 secretV2 = vaultInternalKvV2SecretEngine.getSecret(clientToken, SECRET_PATH_V2, APP_SECRET_PATH);
        assertEquals(SECRET_VALUE, secretV2.data.data.get(SECRET_KEY));
        VaultKvListSecrets vaultKvListSecretsV2 = vaultInternalKvV2SecretEngine.listSecrets(clientToken, SECRET_PATH_V2,
                LIST_PATH);
        assertEquals(EXPECTED_SUB_PATHS, vaultKvListSecretsV2.data.keys.toString());
    }

    private void assertTokenUserPass(String clientToken) {
        VaultLookupSelf vaultLookupSelf = vaultInternalTokenAuthMethod.lookupSelf(clientToken);
        assertEquals("auth/" + USERPASS.name().toLowerCase() + "/login/" + VAULT_AUTH_USERPASS_USER, vaultLookupSelf.data.path);

        VaultRenewSelf vaultRenewSelf = vaultInternalTokenAuthMethod.renewSelf(clientToken, "1h");
        assertEquals(VAULT_AUTH_USERPASS_USER, vaultRenewSelf.auth.metadata.get("username"));
    }

    private void assertTokenAppRole(String clientToken) {
        VaultLookupSelf vaultLookupSelf = vaultInternalTokenAuthMethod.lookupSelf(clientToken);
        assertEquals("auth/approle/login", vaultLookupSelf.data.path);

        VaultRenewSelf vaultRenewSelf = vaultInternalTokenAuthMethod.renewSelf(clientToken, "1h");
        assertEquals(VAULT_AUTH_APPROLE, vaultRenewSelf.auth.metadata.get("role_name"));
    }

}
