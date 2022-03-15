package io.quarkus.security.webauthn;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.vertx.ext.auth.webauthn.Attestation;
import io.vertx.ext.auth.webauthn.AuthenticatorAttachment;
import io.vertx.ext.auth.webauthn.AuthenticatorTransport;
import io.vertx.ext.auth.webauthn.PublicKeyCredential;
import io.vertx.ext.auth.webauthn.UserVerification;

@ConfigRoot(name = "webauthn", phase = ConfigPhase.RUN_TIME)
public class WebAuthnRunTimeConfig {

    /**
     * FIXME
     */
    @ConfigItem
    public Optional<String> origin;

    /**
     * FIXME
     */
    @ConfigItem
    public Optional<String> domain;

    /**
     * FIXME
     */
    @ConfigItem(defaultValueDocumentation = "USB,NFC,BLE,INTERNAL")
    public Optional<List<AuthenticatorTransport>> transports;

    /**
     * FIXME
     */
    @ConfigItem
    public RelyingPartyConfig relyingParty;

    /**
     * FIXME
     */
    @ConfigItem
    public Optional<AuthenticatorAttachment> authenticatorAttachment;

    /**
     * FIXME
     */
    @ConfigItem(defaultValueDocumentation = "false")
    public Optional<Boolean> requireResidentKey;

    /**
     * FIXME
     */
    @ConfigItem(defaultValueDocumentation = "REQUIRED")
    public Optional<UserVerification> userVerification;

    /**
     * FIXME
     */
    @ConfigItem(defaultValueDocumentation = "60s")
    public Optional<Duration> timeout;

    /**
     * FIXME
     */
    @ConfigItem(defaultValueDocumentation = "NONE")
    public Optional<Attestation> attestation;

    // Needs to be a list, order is important
    /**
     * FIXME
     */
    @ConfigItem(defaultValueDocumentation = "ES256,RS256")
    public Optional<List<PublicKeyCredential>> pubKeyCredParams;

    /**
     * FIXME
     */
    @ConfigItem(defaultValueDocumentation = "64")
    public OptionalInt challengeLength;

    // FIXME
    //private JsonObject extensions;

    // FIXME
    //private Map<String, X509Certificate> rootCertificates;

    // FIXME
    //private List<X509CRL> rootCrls;

    @ConfigGroup
    public static class RelyingPartyConfig {
        /**
         * The id (or domain name of your server)
         */
        @ConfigItem
        public Optional<String> id;

        /**
         * A user friendly name for your server
         */
        @ConfigItem(defaultValue = "Quarkus server")
        public String name;

        /**
         * A URL location for an icon
         */
        @ConfigItem
        public Optional<String> icon;
    }

    // FIXME: merge with form config?

    /**
     * The login page
     */
    @ConfigItem(defaultValue = "/login.html")
    public String loginPage;

    /**
     * The inactivity (idle) timeout
     *
     * When inactivity timeout is reached, cookie is not renewed and a new login is enforced.
     */
    @ConfigItem(defaultValue = "PT30M")
    public Duration sessionTimeout;

    /**
     * How old a cookie can get before it will be replaced with a new cookie with an updated timeout, also
     * referred to as "renewal-timeout".
     *
     * Note that smaller values will result in slightly more server load (as new encrypted cookies will be
     * generated more often), however larger values affect the inactivity timeout as the timeout is set
     * when a cookie is generated.
     *
     * For example if this is set to 10 minutes, and the inactivity timeout is 30m, if a users last request
     * is when the cookie is 9m old then the actual timeout will happen 21m after the last request, as the timeout
     * is only refreshed when a new cookie is generated.
     *
     * In other words no timeout is tracked on the server side; the timestamp is encoded and encrypted in the cookie itself
     * and it is decrypted and parsed with each request.
     */
    @ConfigItem(defaultValue = "PT1M")
    public Duration newCookieInterval;

    /**
     * The cookie that is used to store the persistent session
     */
    @ConfigItem(defaultValue = "quarkus-credential")
    public String cookieName;
}
