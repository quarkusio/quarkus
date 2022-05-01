package io.quarkus.mailer.runtime;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DkimSignOptionsConfig {

    /**
     * Enables DKIM signing.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;

    /**
     * Configures the PKCS#8 format private key used to sign the email.
     */
    @ConfigItem
    public Optional<String> privateKey;

    /**
     * Configures the PKCS#8 format private key file path.
     */
    @ConfigItem
    public Optional<String> privateKeyPath;

    /**
     * Configures the Agent or User Identifier(AUID).
     */
    @ConfigItem
    public Optional<String> auid;

    /**
     * Configures the selector used to query the public key.
     */
    @ConfigItem
    public Optional<String> selector;

    /**
     * Configures the Signing Domain Identifier.
     */
    @ConfigItem
    public Optional<String> sdid;

    /**
     * Configures the canonicalization algorithm for signed headers.
     */
    @ConfigItem
    public Optional<CanonicalizationAlgorithmOption> headerCanonAlgo;

    /**
     * Configures the canonicalization algorithm for mail body.
     */
    @ConfigItem
    public Optional<CanonicalizationAlgorithmOption> bodyCanonAlgo;

    /**
     * Configures the body limit to sign.
     *
     * Must be greater than zero.
     */
    @ConfigItem
    public OptionalInt bodyLimit;

    /**
     * Configures to enable or disable signature sign timestmap.
     */
    @ConfigItem
    public Optional<Boolean> signatureTimestamp;

    /**
     * Configures the expire time in seconds when the signature sign will be expired.
     *
     * Must be greater than zero.
     */
    @ConfigItem
    public OptionalLong expireTime;

    /**
     * Configures the signed headers in DKIM, separated by commas.
     *
     * The order in the list matters.
     */
    @ConfigItem
    public Optional<List<String>> signedHeaders;

    public enum CanonicalizationAlgorithmOption {
        SIMPLE,
        RELAXED
    }
}
