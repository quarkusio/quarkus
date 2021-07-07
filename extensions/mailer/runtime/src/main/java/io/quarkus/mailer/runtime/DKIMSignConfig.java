package io.quarkus.mailer.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DKIMSignConfig {

    /**
     * Configure the signing algorithm.
     * Accepted values are {@code rsa-sha256} (default) and {@code rsa-sha1}
     */
    @ConfigItem(defaultValue = "rsa-sha256")
    public String signAlgo;

    /**
     * Gets the private key used to sigh the email. Must use the PKCS#8 format.
     */
    @ConfigItem
    public Optional<String> privateKey;

    /**
     * Gets the private key used to sigh the email. Must use the PKCS#8 format.
     * Only used if {@link #privateKey} is not set.
     */
    @ConfigItem
    public Optional<String> privateKeyPath;

    /**
     * The comma-separated list of headers used to sign.
     * Default is: {@code From,Reply-To,Subject,Date,To,Cc,Content-Type,Message-ID}
     */
    @ConfigItem
    public Optional<String> signedHeaders;

    /**
     * Set the Signing Domain Identifier (sdid).
     */
    @ConfigItem
    public Optional<String> sdid;

    /**
     * Set the selected used to query the public key.
     */
    @ConfigItem
    public Optional<String> selector;

    /**
     * Set the the canonicalization algorithm for signed headers.
     * Accepted values are: {@code SIMPLE} (default), {@code RELAXED}.
     */
    @ConfigItem(defaultValue = "SIMPLE")
    public Optional<String> headerCanonAlgo;

    /**
     * Set the the canonicalization algorithm for mail body.
     * Accepted values are: {@code SIMPLE} (default), {@code RELAXED}.
     */
    @ConfigItem(defaultValue = "SIMPLE")
    public Optional<String> bodyCanonAlgo;

    /**
     * Set the Agent or User Identifier(AUID).
     */
    @ConfigItem
    public Optional<String> auid;

    /**
     * Set the body limit to sign.
     */
    @ConfigItem
    public OptionalInt bodyLimit;

    /**
     * Enable or disable whether it adds the signature sign timestamp.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableSignatureTimestamp;

    /**
     * Set the expiration time of the signature sign.
     */
    @ConfigItem
    public Optional<Duration> expirationTime;

    /**
     * Sets the copied headers used in DKIM as comma-separated list.
     */
    @ConfigItem
    public Optional<String> copiedHeaders;

}
