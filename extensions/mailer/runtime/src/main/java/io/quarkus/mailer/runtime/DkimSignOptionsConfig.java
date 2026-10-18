package io.quarkus.mailer.runtime;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DkimSignOptionsConfig {

    /**
     * Enables DKIM signing.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Configures the PKCS#8 format private key used to sign the email.
     */
    Optional<String> privateKey();

    /**
     * Configures the PKCS#8 format private key file path.
     */
    Optional<String> privateKeyPath();

    /**
     * Configures the Agent or User Identifier (AUID).
     */
    Optional<String> auid();

    /**
     * Configures the selector used to query the public key.
     */
    Optional<String> selector();

    /**
     * Configures the Signing Domain Identifier (SDID).
     */
    Optional<String> sdid();

    /**
     * Configures the canonicalization algorithm for signed headers.
     */
    Optional<CanonicalizationAlgorithmOption> headerCanonAlgo();

    /**
     * Configures the canonicalization algorithm for mail body.
     */
    Optional<CanonicalizationAlgorithmOption> bodyCanonAlgo();

    /**
     * Configures the body limit to sign.
     *
     * Must be greater than zero.
     */
    OptionalInt bodyLimit();

    /**
     * Configures to enable or disable signature sign timestamp.
     */
    Optional<Boolean> signatureTimestamp();

    /**
     * Configures the expire time in seconds when the signature sign will be expired.
     *
     * Must be greater than zero.
     */
    OptionalLong expireTime();

    /**
     * Configures the signed headers in DKIM, separated by commas.
     *
     * The order in the list matters.
     */
    Optional<List<String>> signedHeaders();

    public enum CanonicalizationAlgorithmOption {
        SIMPLE,
        RELAXED
    }
}
