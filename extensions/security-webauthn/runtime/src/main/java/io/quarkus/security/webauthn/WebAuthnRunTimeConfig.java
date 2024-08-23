package io.quarkus.security.webauthn;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.vertx.ext.auth.webauthn.Attestation;
import io.vertx.ext.auth.webauthn.AuthenticatorAttachment;
import io.vertx.ext.auth.webauthn.AuthenticatorTransport;
import io.vertx.ext.auth.webauthn.PublicKeyCredential;
import io.vertx.ext.auth.webauthn.UserVerification;

/**
 * Webauthn runtime configuration object.
 */
@ConfigMapping(prefix = "quarkus.webauthn")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface WebAuthnRunTimeConfig {

    /**
     * SameSite attribute values for the session cookie.
     */
    enum CookieSameSite {
        STRICT,
        LAX,
        NONE
    }

    /**
     * The origin of the application. The origin is basically protocol, host and port.
     *
     * If you are calling WebAuthn API while your application is located at {@code https://example.com/login},
     * then origin will be {@code https://example.com}.
     *
     * If you are calling from {@code http://localhost:2823/test}, then the origin will be
     * {@code http://localhost:2823}.
     *
     * Please note that WebAuthn API will not work on pages loaded over HTTP, unless it is localhost,
     * which is considered secure context.
     */
    Optional<String> origin();

    /**
     * Authenticator Transports allowed by the application. Authenticators can interact with the user web browser
     * through several transports. Applications may want to restrict the transport protocols for extra security
     * hardening reasons.
     *
     * By default, all transports should be allowed. If your application is to be used by mobile phone users,
     * you may want to restrict only the {@code INTERNAL} authenticator to be allowed.
     *
     * Permitted values are:
     *
     * <ul>
     * <li>{@code USB} - USB connected authenticators (e.g.: Yubikey's)</li>
     * <li>{@code NFC} - NFC connected authenticators (e.g.: Yubikey's)</li>
     * <li>{@code BLE} - Bluetooth LE connected authenticators</li>
     * <li>{@code INTERNAL} - Hardware security chips (e.g.: Intel TPM2.0)</li>
     * </ul>
     */
    @ConfigDocDefault("USB,NFC,BLE,INTERNAL")
    Optional<List<AuthenticatorTransport>> transports();

    /**
     * Your application is a relying party. In order for the user browser to correctly present you to the user, basic
     * information should be provided that will be presented during the user verification popup messages.
     */
    RelyingPartyConfig relyingParty();

    /**
     * Kind of Authenticator Attachment allowed. Authenticators can connect to your device in two forms:
     *
     * <ul>
     * <li>{@code PLATFORM} - The Authenticator is built-in to your device (e.g.: Security chip)</li>
     * <li>{@code CROSS_PLATFORM} - The Authenticator can roam across devices (e.g.: USB Authenticator)</li>
     * </ul>
     *
     * For security reasons your application may choose to restrict to a specific attachment mode. If omitted, then
     * any mode is permitted.
     */
    Optional<AuthenticatorAttachment> authenticatorAttachment();

    /**
     * Resident key required. A resident (private) key, is a key that cannot leave your authenticator device, this
     * means that you cannot reuse the authenticator to log into a second computer.
     */
    @ConfigDocDefault("false")
    Optional<Boolean> requireResidentKey();

    /**
     * User Verification requirements. Webauthn applications may choose {@code REQUIRED} verification to assert that
     * the user is present during the authentication ceremonies, but in some cases, applications may want to reduce the
     * interactions with the user, i.e.: prevent the use of pop-ups. Valid values are:
     *
     * <ul>
     * <li>{@code REQUIRED} - User must always interact with the browser</li>
     * <li>{@code PREFERRED} - User should always interact with the browser</li>
     * <li>{@code DISCOURAGED} - User should avoid interact with the browser</li>
     * </ul>
     */
    @ConfigDocDefault("DISCOURAGED")
    Optional<UserVerification> userVerification();

    /**
     * Non-negative User Verification timeout. Authentication must occur within the timeout, this will prevent the user
     * browser from being blocked with a pop-up required user verification, and the whole ceremony must be completed
     * within the timeout period. After the timeout, any previously issued challenge is automatically invalidated.
     */
    @ConfigDocDefault("60s")
    Optional<Duration> timeout();

    /**
     * Device Attestation Preference. During registration, applications may want to attest the device. Attestation is a
     * cryptographic verification of the authenticator hardware.
     *
     * Attestation implies that the privacy of the users may be exposed and browsers might override the desired
     * configuration on the user's behalf.
     *
     * Valid values are:
     *
     * <ul>
     * <li>{@code NONE} - no attestation data is sent with registration</li>
     * <li>{@code INDIRECT} - attestation data is sent with registration, yielding anonymized data by a trusted
     * CA</li>
     * <li>{@code DIRECT} - attestation data is sent with registration</li>
     * <li>{@code ENTERPRISE} - no attestation data is sent with registration. The device AAGUID is returned
     * unaltered.</li>
     * </ul>
     */
    @ConfigDocDefault("NONE")
    Optional<Attestation> attestation();

    /**
     * Allowed Public Key Credential algorithms by preference order. Webauthn mandates that all authenticators must
     * support at least the following 2 algorithms: {@code ES256} and {@code RS256}.
     *
     * Applications may require stronger keys and algorithms, for example: {@code ES512} or {@code EdDSA}.
     *
     * Note that the use of stronger algorithms, e.g.: {@code EdDSA} may require Java 15 or a cryptographic {@code JCE}
     * provider that implements the algorithms.
     */
    @ConfigDocDefault("ES256,RS256")
    Optional<List<PublicKeyCredential>> pubKeyCredParams();

    /**
     * Length of the challenges exchanged between the application and the browser.
     * Challenges must be at least 32 bytes.
     */
    @ConfigDocDefault("64")
    OptionalInt challengeLength();

    /**
     * Extensions are optional JSON blobs that can be used during registration or authentication that can enhance the
     * user experience. Extensions are defined in
     * <a href="https://www.w3.org/TR/webauthn/#sctn-extensions">https://www.w3.org/TR/webauthn/#sctn-extensions</a>.
     *
     * TODO: don't enable this yet and with upcoming Leve3 version vert.x may break this config
     * see https://github.com/vert-x3/vertx-auth/issues/535
     */
    //private JsonObject extensions;

    /**
     * Root certificates for attestation verification of authenticators. These are the root certificates from Google,
     * Apple and FIDO Alliance. Certificates can be overridden, for example, when they are about to expire.
     */
    //private Map<String, X509Certificate> rootCertificates;

    /**
     * Root CRLs for attestation verification of authenticators. These are the root CRLs from FIDO Alliance.
     * The CRLs are required to ensure that the metda data service can be trusted.
     */
    //private List<X509CRL> rootCrls;

    @ConfigGroup
    interface RelyingPartyConfig {
        /**
         * The id (or domain name of your server)
         */
        Optional<String> id();

        /**
         * A user friendly name for your server
         */
        @WithDefault("Quarkus server")
        String name();
    }

    // FIXME: merge with form config?

    /**
     * The login page
     */
    @WithDefault("/login.html")
    String loginPage();

    /**
     * The inactivity (idle) timeout
     *
     * When inactivity timeout is reached, cookie is not renewed and a new login is enforced.
     */
    @WithDefault("PT30M")
    Duration sessionTimeout();

    /**
     * How old a cookie can get before it will be replaced with a new cookie with an updated timeout, also
     * referred to as "renewal-timeout".
     *
     * Note that smaller values will result in slightly more server load (as new encrypted cookies will be
     * generated more often); however, larger values affect the inactivity timeout because the timeout is set
     * when a cookie is generated.
     *
     * For example if this is set to 10 minutes, and the inactivity timeout is 30m, if a user's last request
     * is when the cookie is 9m old then the actual timeout will happen 21m after the last request because the timeout
     * is only refreshed when a new cookie is generated.
     *
     * That is, no timeout is tracked on the server side; the timestamp is encoded and encrypted in the cookie
     * itself, and it is decrypted and parsed with each request.
     */
    @WithDefault("PT1M")
    Duration newCookieInterval();

    /**
     * The cookie that is used to store the persistent session
     */
    @WithDefault("quarkus-credential")
    String cookieName();

    /**
     * The cookie that is used to store the challenge data during login/registration
     */
    @WithDefault("_quarkus_webauthn_challenge")
    public String challengeCookieName();

    /**
     * The cookie that is used to store the username data during login/registration
     */
    @WithDefault("_quarkus_webauthn_username")
    public String challengeUsernameCookieName();

    /**
     * SameSite attribute for the session cookie.
     */
    @WithDefault("strict")
    CookieSameSite cookieSameSite();

    /**
     * The cookie path for the session cookies.
     */
    @WithDefault("/")
    Optional<String> cookiePath();
}
