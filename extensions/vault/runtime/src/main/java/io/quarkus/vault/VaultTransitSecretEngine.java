package io.quarkus.vault;

import java.util.List;
import java.util.Map;

import io.quarkus.vault.transit.ClearData;
import io.quarkus.vault.transit.DecryptionRequest;
import io.quarkus.vault.transit.EncryptionRequest;
import io.quarkus.vault.transit.RewrappingRequest;
import io.quarkus.vault.transit.SigningInput;
import io.quarkus.vault.transit.SigningRequest;
import io.quarkus.vault.transit.TransitContext;
import io.quarkus.vault.transit.VaultDecryptionBatchException;
import io.quarkus.vault.transit.VaultEncryptionBatchException;
import io.quarkus.vault.transit.VaultRewrappingBatchException;
import io.quarkus.vault.transit.VaultSigningBatchException;
import io.quarkus.vault.transit.VaultVerificationBatchException;
import io.quarkus.vault.transit.VerificationRequest;

/**
 * A service that interacts with Hashicorp's Vault Transit secret engine to encrypt, decrypt and sign arbitrary data.
 * 
 * @see <a href="https://www.vaultproject.io/docs/secrets/transit/index.html#transit-secrets-engine">Transit Secrets Engine</a>
 */
public interface VaultTransitSecretEngine {

    /**
     * Encrypt a regular string with a Vault key configured in the transit secret engine.
     * Equivalent to:
     * {@code encrypt(keyName, ClearData.from(clearData), null);}
     * <p>
     * This method is usually used in conjunction with {@link #decrypt(String, String)}
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#encrypt-data">encrypt data</a>
     * @param keyName the key to encrypt the data with
     * @param clearData the string to encrypt
     * @return cipher text
     */
    String encrypt(String keyName, String clearData);

    /**
     * Encrypt a regular string with a Vault key configured in the transit secret engine.
     * If the key does not exist, and the policy specifies a create capability the key will be lazily created
     * (i.e. upsert). The key can be further customized by specifying transit encryption-key configuration
     * properties.
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#encrypt-data">encrypt data</a>
     * @param keyName the key to encrypt the data with
     * @param clearData the data to encrypt
     * @param transitContext optional transit context used for key derivation
     * @return cipher text
     */
    String encrypt(String keyName, ClearData clearData, TransitContext transitContext);

    /**
     * Encrypt a list of elements. This will return a list of cipher texts.
     * Each element shall specify the data to encrypt, an optional key version
     * and an optional transit context, used for key derivation if applicable.
     * If any error occurs, the service will throw a {@link VaultEncryptionBatchException}
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#encrypt-data">encrypt data</a>
     * @param keyName the key to encrypt the data with
     * @param requests the list of elements to encrypt
     * @return a map of each request and its corresponding cipher text
     */
    Map<EncryptionRequest, String> encrypt(String keyName, List<EncryptionRequest> requests);

    /**
     * Decrypt the encrypted data with the specified key, and return unencrypted data.
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#decrypt-data">decrypt data</a>
     * @param keyName the key that was used to encrypt the original data
     * @param ciphertext the encrypted data
     * @return the unencrypted data
     */
    ClearData decrypt(String keyName, String ciphertext);

    /**
     * Decrypt the encrypted data with the specified key and a transit context used for key derivation.
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#decrypt-data">decrypt data</a>
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#derived">create key derived attribute</a>
     * @param keyName the key that was used to encrypt the original data
     * @param ciphertext data to decrypt
     * @param transitContext optional transit context used for key derivation
     * @return the unencrypted data
     */
    ClearData decrypt(String keyName, String ciphertext, TransitContext transitContext);

    /**
     * Decrypt a list of encrypted data items. Each item shall specify the encrypted data plus an optional transit
     * context used for key derivation (if applicable).
     * If any error occurs, the service will throw a {@link VaultDecryptionBatchException}
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#decrypt-data">decrypt data</a>
     * @param keyName the key that was used to encrypt the original data
     * @param requests the list of encrypted data items
     * @return a map of each request with its corresponding decrypted data item
     */
    Map<DecryptionRequest, ClearData> decrypt(String keyName, List<DecryptionRequest> requests);

    /**
     * Reencrypt into a new cipher text a cipher text that was obtained from encryption using an old key version
     * with the last key version
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#rewrap-data">rewrap data</a>
     * @see <a href="https://www.vaultproject.io/docs/secrets/transit/index.html#working-set-management">working set
     *      management</a>
     * @param keyName the encryption key that was used for the previous encryption
     * @param ciphertext the old cipher text that needs rewrapping
     * @return the reencrypted cipher text with last key version as a new cipher text
     */
    String rewrap(String keyName, String ciphertext);

    /**
     * Reencrypt into a new cipher text a cipher text that was obtained from encryption using an old key version
     * with the last key version and an optional transit context used for key derivation
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#rewrap-data">rewrap data</a>
     * @see <a href="https://www.vaultproject.io/docs/secrets/transit/index.html#working-set-management">working set
     *      management</a>
     * @param keyName the encryption key that was used for the previous encryption
     * @param ciphertext the old cipher text that needs rewrapping
     * @param transitContext optional transit context used for key derivation
     * @return the reencrypted cipher text with last key version as a new cipher text
     */
    String rewrap(String keyName, String ciphertext, TransitContext transitContext);

    /**
     * Reencrypt a list of encrypted data items with the last version of the specified key.
     * Each item shall specify a cipher text to reencrypt, an optional key version, and an optional transit context
     * used for key derivation, if applicable.
     * If any error occurs, the service will throw a {@link VaultRewrappingBatchException}
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#rewrap-data">rewrap data</a>
     * @see <a href="https://www.vaultproject.io/docs/secrets/transit/index.html#working-set-management">working set
     *      management</a>
     * @param keyName the encryption key that was used for the previous encryptions
     * @param requests the list of items to reencrypt
     * @return a map of each request with its corresponding reencrypted data item
     */
    Map<RewrappingRequest, String> rewrap(String keyName, List<RewrappingRequest> requests);

    /**
     * Sign an input string with the specified key.
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#sign-data">sign data</a>
     * @param keyName the signing key to use
     * @param input String to sign
     * @return the signature
     */
    String sign(String keyName, String input);

    /**
     * Sign the input with the specified key and an optional transit context used for key derivation, if applicable.
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#sign-data">sign data</a>
     * @param keyName the signing key to use
     * @param input data to sign
     * @param transitContext optional transit context used for key derivation
     * @return the signature
     */
    String sign(String keyName, SigningInput input, TransitContext transitContext);

    /**
     * Sign a list of inputs items. Each item shall specify the input to sign, an optional key version, and
     * an optional transit context used for ky derivation, if applicable.
     * If any error occurs, the service will throw a {@link VaultSigningBatchException}
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#sign-data">sign data</a>
     * @param keyName the signing key to use
     * @param requests the list of inputs to sign
     * @return a map of each request with its corresponding signature item
     */
    Map<SigningRequest, String> sign(String keyName, List<SigningRequest> requests);

    /**
     * Checks that the signature was obtained from signing the input with the specified key.
     * The service will throw a {@link VaultException} if this is not the case.
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#verify-signed-data">verify signed data</a>
     * @param keyName the key that was used to sign the input
     * @param signature the signature obtained from one of the sign methods
     * @param input the original input data
     */
    void verifySignature(String keyName, String signature, String input);

    /**
     * Checks that the signature was obtained from signing the input with the specified key.
     * The service will throw a {@link VaultException} if this is not the case.
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#verify-signed-data">verify signed data</a>
     * @param keyName the key that was used to sign the input
     * @param signature the signature obtained from one of the sign methods
     * @param input the original input data
     * @param transitContext optional transit context used for key derivation
     */
    void verifySignature(String keyName, String signature, SigningInput input, TransitContext transitContext);

    /**
     * Checks a list of verification requests. Each request shall specify an input and the signature we want to match
     * against, and an optional transit context used for key derivation, if applicable.
     * If the signature does not match, or if any other error occurs,
     * the service will throw a {@link VaultVerificationBatchException}
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#verify-signed-data">verify signed data</a>
     * @param keyName the key that was used to sign the input
     * @param requests a list of items specifying an input and a signature to match against
     */
    void verifySignature(String keyName, List<VerificationRequest> requests);

}
