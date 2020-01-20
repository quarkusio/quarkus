package io.quarkus.vault.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class TransitKeyConfig {

    /**
     * Specifies the name of the key to use. By default this will be the property key alias. Used when
     * the same transit key is used with different configurations. Such as in:
     * ```
     * quarkus.vault.transit.key.my-foo-key.name=foo
     *
     * quarkus.vault.transit.key.my-foo-key-with-prehashed.name=foo
     * quarkus.vault.transit.key.my-foo-key-with-prehashed.prehashed=true
     * ...
     * transitSecretEngine.sign("my-foo-key", "my raw content");
     * or
     * transitSecretEngine.sign("my-foo-key-with-prehashed", "my already hashed content");
     * ```
     * 
     * @asciidoclet
     */
    @ConfigItem
    public Optional<String> name;

    // sign

    /**
     * Set to true when the input is already hashed.
     * Applies to sign operations.
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#prehashed">api documentation for prehashed</a>
     */
    @ConfigItem
    public Optional<Boolean> prehashed;

    /**
     * When using a RSA key, specifies the RSA signature algorithm.
     * Applies to sign operations.
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#signature_algorithm">api documentation for
     *      signature_algorithm</a>
     */
    @ConfigItem
    public Optional<String> signatureAlgorithm;

    /**
     * Specifies the hash algorithm to use for supporting key types.
     * Applies to sign operations.
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#hash_algorithm">api documentation for
     *      hash_algorithm</a>
     */
    @ConfigItem
    public Optional<String> hashAlgorithm;

    // encrypt

    /**
     * Specifies the type of key to create for the encrypt operation.
     * Applies to encrypt operations.
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#type">api documentation for type</a>
     */
    @ConfigItem
    public Optional<String> type;

    /**
     * If enabled, the key will support convergent encryption, where the same plaintext creates the same ciphertext.
     * Applies to encrypt operations.
     * 
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#convergent_encryption">api documentation for
     *      convergent_encryption</a>
     */
    @ConfigItem
    public Optional<String> convergentEncryption;

}
