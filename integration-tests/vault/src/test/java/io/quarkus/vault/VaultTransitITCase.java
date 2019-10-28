package io.quarkus.vault;

import static io.quarkus.vault.test.VaultTestExtension.ENCRYPTION_DERIVED_KEY_NAME;
import static io.quarkus.vault.test.VaultTestExtension.ENCRYPTION_KEY2_NAME;
import static io.quarkus.vault.test.VaultTestExtension.ENCRYPTION_KEY_NAME;
import static io.quarkus.vault.test.VaultTestExtension.SIGN_DERIVATION_KEY_NAME;
import static io.quarkus.vault.test.VaultTestExtension.SIGN_KEY2_NAME;
import static io.quarkus.vault.test.VaultTestExtension.SIGN_KEY_NAME;
import static io.quarkus.vault.transit.VaultTransitSecretEngineConstants.INVALID_SIGNATURE;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vault.runtime.VaultManager;
import io.quarkus.vault.test.client.TestVaultClient;
import io.quarkus.vault.transit.ClearData;
import io.quarkus.vault.transit.DecryptionRequest;
import io.quarkus.vault.transit.EncryptionRequest;
import io.quarkus.vault.transit.RewrappingRequest;
import io.quarkus.vault.transit.SigningInput;
import io.quarkus.vault.transit.SigningRequest;
import io.quarkus.vault.transit.TransitContext;
import io.quarkus.vault.transit.VaultVerificationBatchException;
import io.quarkus.vault.transit.VerificationRequest;

public class VaultTransitITCase {

    private static final Logger log = Logger.getLogger(VaultTransitITCase.class);

    public static final String COUCOU = "coucou";
    public static final String NEW_KEY = "new-key";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-vault.properties", "application.properties"));

    private TransitContext context = TransitContext.fromContext("my context");
    private ClearData data = new ClearData(COUCOU);
    private SigningInput input = new SigningInput(COUCOU);

    @Inject
    VaultTransitSecretEngine transitSecretEngine;

    @Test
    public void encryptionString() {
        String ciphertext = transitSecretEngine.encrypt(ENCRYPTION_KEY_NAME, COUCOU);
        ClearData decrypted = transitSecretEngine.decrypt(ENCRYPTION_KEY_NAME, ciphertext);
        assertEquals(COUCOU, decrypted.asString());
    }

    @Test
    public void encryptionBytes() {
        String ciphertext = transitSecretEngine.encrypt(ENCRYPTION_KEY_NAME, data, null);
        ClearData decrypted = transitSecretEngine.decrypt(ENCRYPTION_KEY_NAME, ciphertext, null);
        assertEquals(COUCOU, decrypted.asString());
    }

    @Test
    public void encryptionContext() {
        String ciphertext = transitSecretEngine.encrypt(ENCRYPTION_DERIVED_KEY_NAME, data, context);
        ClearData decrypted = transitSecretEngine.decrypt(ENCRYPTION_DERIVED_KEY_NAME, ciphertext, context);
        assertEquals(COUCOU, decrypted.asString());
    }

    @Test
    public void encryptionBatch() {
        List<EncryptionRequest> encryptBatch = singletonList(new EncryptionRequest(data));
        Map<EncryptionRequest, String> encryptList = transitSecretEngine.encrypt(ENCRYPTION_KEY_NAME, encryptBatch);
        String ciphertext = getSingleValue(encryptList);
        List<DecryptionRequest> decryptBatch = singletonList(new DecryptionRequest(ciphertext));
        Map<DecryptionRequest, ClearData> decryptList = transitSecretEngine.decrypt(ENCRYPTION_KEY_NAME, decryptBatch);
        assertEquals(1, decryptList.size());
        assertEquals(COUCOU, getSingleValue(decryptList).asString());
    }

    @Test
    public void rewrapBatch() {

        String ciphertext = transitSecretEngine.encrypt(ENCRYPTION_KEY_NAME, COUCOU);
        ClearData decrypted = transitSecretEngine.decrypt(ENCRYPTION_KEY_NAME, ciphertext);
        assertEquals(COUCOU, decrypted.asString());

        List<RewrappingRequest> rewrapBatch = singletonList(new RewrappingRequest(ciphertext));
        Map<RewrappingRequest, String> rewrapBatchResult = transitSecretEngine.rewrap(ENCRYPTION_KEY_NAME, rewrapBatch);
        ciphertext = getSingleValue(rewrapBatchResult);

        decrypted = transitSecretEngine.decrypt(ENCRYPTION_KEY_NAME, ciphertext);
        assertEquals(COUCOU, decrypted.asString());
    }

    @Test
    public void upsert() {
        String ciphertext = transitSecretEngine.encrypt(NEW_KEY, data, null);
        ClearData decrypted = transitSecretEngine.decrypt(NEW_KEY, ciphertext, null);
        assertEquals(COUCOU, decrypted.asString());
    }

    @Test
    public void signString() {
        String signature = transitSecretEngine.sign(SIGN_KEY_NAME, input, null);
        transitSecretEngine.verifySignature(SIGN_KEY_NAME, signature, input, null);
    }

    @Test
    public void signJws() {
        String signature = transitSecretEngine.sign("jws", input, null);
        transitSecretEngine.verifySignature("jws", signature, input, null);
    }

    @Test
    public void signBytes() {
        String signature = transitSecretEngine.sign(SIGN_KEY_NAME, input, null);
        transitSecretEngine.verifySignature(SIGN_KEY_NAME, signature, input, null);
    }

    @Test
    public void signContext() {
        String signature = transitSecretEngine.sign(SIGN_DERIVATION_KEY_NAME, input, context);
        transitSecretEngine.verifySignature(SIGN_DERIVATION_KEY_NAME, signature, input, context);
    }

    @Test
    public void signBatch() {
        List<SigningRequest> batch = singletonList(new SigningRequest(input));
        Map<SigningRequest, String> signatures = transitSecretEngine.sign(SIGN_KEY_NAME, batch);
        assertEquals(1, signatures.size());
        String signature = getSingleValue(signatures);
        List<VerificationRequest> batchVerify = singletonList(new VerificationRequest(signature, input));
        transitSecretEngine.verifySignature(SIGN_KEY_NAME, batchVerify);
    }

    @Test
    public void keyVersionEncryption() {

        rotate(ENCRYPTION_KEY2_NAME);

        String encryptV1 = encrypt(1);
        assertTrue(encryptV1.startsWith("vault:v1"));
        assertEquals(COUCOU, decrypt(encryptV1));

        String rewraped = transitSecretEngine.rewrap(ENCRYPTION_KEY2_NAME, encryptV1, null);
        assertTrue(rewraped.startsWith("vault:v2"));

        String encryptV2 = encrypt(2);
        assertTrue(encryptV2.startsWith("vault:v2"));
        assertEquals(COUCOU, decrypt(encryptV2));

    }

    private void rotate(String keyName) {
        VaultManager vaultManager = VaultManager.getInstance();
        String clientToken = vaultManager.getVaultAuthManager().getClientToken();
        new TestVaultClient().rotate(clientToken, keyName);
    }

    private String encrypt(int keyVersion) {
        EncryptionRequest request = new EncryptionRequest(data, keyVersion);
        List<EncryptionRequest> encryptBatch = singletonList(request);
        Map<EncryptionRequest, String> encryptList = transitSecretEngine.encrypt(ENCRYPTION_KEY2_NAME, encryptBatch);
        String ciphertext = getSingleValue(encryptList);
        return ciphertext;
    }

    private String decrypt(String ciphertext) {
        DecryptionRequest request = new DecryptionRequest(ciphertext);
        List<DecryptionRequest> decryptBatch = singletonList(request);
        Map<DecryptionRequest, ClearData> decryptList = transitSecretEngine.decrypt(ENCRYPTION_KEY2_NAME, decryptBatch);
        return getSingleValue(decryptList).asString();
    }

    @Test
    public void keyVersionSign() {

        rotate(SIGN_KEY2_NAME);

        String sign1 = sign(1);
        assertTrue(sign1.startsWith("vault:v1"));
        transitSecretEngine.verifySignature(SIGN_KEY2_NAME, sign1, input, null);

        String sign2 = sign(2);
        assertTrue(sign2.startsWith("vault:v2"));
        transitSecretEngine.verifySignature(SIGN_KEY2_NAME, sign2, input, null);
    }

    @Test
    public void keyVersionSignBatch() {

        SigningRequest signingRequest1 = new SigningRequest(input, 1);
        SigningRequest signingRequest2 = new SigningRequest(input, 2);
        List<SigningRequest> signingRequests = Arrays.asList(signingRequest1, signingRequest2);

        Map<SigningRequest, String> signatures = transitSecretEngine.sign(SIGN_KEY2_NAME, signingRequests);

        assertEquals(2, signatures.size());
        String sign1 = signatures.get(signingRequest1);
        String sign2 = signatures.get(signingRequest2);
        assertTrue(sign1.startsWith("vault:v1"));
        assertTrue(sign2.startsWith("vault:v2"));

        VerificationRequest verificationRequest1 = new VerificationRequest(sign1, input);
        VerificationRequest verificationRequest2 = new VerificationRequest(sign2, input);
        List<VerificationRequest> verificationRequests = Arrays.asList(verificationRequest1, verificationRequest2);

        transitSecretEngine.verifySignature(SIGN_KEY2_NAME, verificationRequests);
    }

    private String sign(int keyVersion) {
        SigningRequest request = new SigningRequest(input, keyVersion);
        Map<SigningRequest, String> signingResults = transitSecretEngine.sign(SIGN_KEY2_NAME, singletonList(request));
        String signature = getSingleValue(signingResults);
        return signature;
    }

    @Test
    public void verifySignatureInvalid() {

        String signature = transitSecretEngine.sign(SIGN_KEY_NAME, input, null);
        SigningInput otherInput = new SigningInput("some other input");

        try {
            transitSecretEngine.verifySignature(SIGN_KEY_NAME, signature, otherInput, null);
            fail();
        } catch (VaultException e) {
            assertEquals(INVALID_SIGNATURE, e.getMessage());
        }

        VerificationRequest request = new VerificationRequest(signature, otherInput);
        try {
            transitSecretEngine.verifySignature(SIGN_KEY_NAME, Arrays.asList(request));
            fail();
        } catch (VaultVerificationBatchException e) {
            assertTrue(e.getValid().isEmpty());
            assertEquals(1, e.getErrors().size());
            assertEquals(INVALID_SIGNATURE, e.getErrors().get(request));
        }
    }

    @Test
    public void bigSignBatch() {

        List<SigningRequest> signingRequests = IntStream.range(0, 1000)
                .mapToObj(i -> new SigningRequest(new SigningInput("coucou" + i)))
                .collect(toList());

        Map<SigningRequest, String> signatures = transitSecretEngine.sign(SIGN_KEY_NAME, signingRequests);

        List<VerificationRequest> verificationRequests = signatures.entrySet().stream()
                .map(e -> new VerificationRequest(e.getValue(), e.getKey().getInput()))
                .collect(toList());

        transitSecretEngine.verifySignature(SIGN_KEY_NAME, verificationRequests);
    }

    private <K, V> V getSingleValue(Map<K, V> map) {
        assertEquals(1, map.size());
        return map.values().stream().findFirst().get();
    }

}
