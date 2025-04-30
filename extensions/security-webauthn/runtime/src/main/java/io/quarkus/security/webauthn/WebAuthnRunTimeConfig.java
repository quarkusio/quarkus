package io.quarkus.security.webauthn;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.ResidentKeyRequirement;
import com.webauthn4j.data.UserVerificationRequirement;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Webauthn runtime configuration object.
 */
@ConfigMapping(prefix = "quarkus.webauthn")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface WebAuthnRunTimeConfig {

    /**
     * COSEAlgorithm
     * https://www.iana.org/assignments/cose/cose.xhtml#algorithms
     */
    public enum COSEAlgorithm {
        ES256(-7),
        ES384(-35),
        ES512(-36),
        PS256(-37),
        PS384(-38),
        PS512(-39),
        ES256K(-47),
        RS256(-257),
        RS384(-258),
        RS512(-259),
        RS1(-65535),
        EdDSA(-8);

        private final int coseId;

        COSEAlgorithm(int coseId) {
            this.coseId = coseId;
        }

        public static COSEAlgorithm valueOf(int coseId) {
            switch (coseId) {
                case -7:
                    return ES256;
                case -35:
                    return ES384;
                case -36:
                    return ES512;
                case -37:
                    return PS256;
                case -38:
                    return PS384;
                case -39:
                    return PS512;
                case -47:
                    return ES256K;
                case -257:
                    return RS256;
                case -258:
                    return RS384;
                case -259:
                    return RS512;
                case -65535:
                    return RS1;
                case -8:
                    return EdDSA;
                default:
                    throw new IllegalArgumentException("Unknown cose-id: " + coseId);
            }
        }

        public int coseId() {
            return coseId;
        }
    }

    /**
     * AttestationConveyancePreference
     * https://www.w3.org/TR/webauthn/#attestation-convey
     */
    public enum Attestation {
        NONE,
        INDIRECT,
        DIRECT,
        ENTERPRISE;

        AttestationConveyancePreference toWebAuthn4J() {
            switch (this) {
                case DIRECT:
                    return AttestationConveyancePreference.DIRECT;
                case ENTERPRISE:
                    return AttestationConveyancePreference.ENTERPRISE;
                case INDIRECT:
                    return AttestationConveyancePreference.INDIRECT;
                case NONE:
                    return AttestationConveyancePreference.NONE;
                default:
                    throw new IllegalStateException("Illegal enum value: " + this);
            }
        }
    }

    /**
     * UserVerificationRequirement
     * https://www.w3.org/TR/webauthn/#enumdef-userverificationrequirement
     */
    public enum UserVerification {
        REQUIRED,
        PREFERRED,
        DISCOURAGED;

        UserVerificationRequirement toWebAuthn4J() {
            switch (this) {
                case DISCOURAGED:
                    return UserVerificationRequirement.DISCOURAGED;
                case PREFERRED:
                    return UserVerificationRequirement.PREFERRED;
                case REQUIRED:
                    return UserVerificationRequirement.REQUIRED;
                default:
                    throw new IllegalStateException("Illegal enum value: " + this);
            }
        }
    }

    /**
     * AuthenticatorAttachment
     * https://www.w3.org/TR/webauthn/#enumdef-authenticatorattachment
     */
    public enum AuthenticatorAttachment {
        PLATFORM,
        CROSS_PLATFORM;

        com.webauthn4j.data.AuthenticatorAttachment toWebAuthn4J() {
            switch (this) {
                case CROSS_PLATFORM:
                    return com.webauthn4j.data.AuthenticatorAttachment.CROSS_PLATFORM;
                case PLATFORM:
                    return com.webauthn4j.data.AuthenticatorAttachment.PLATFORM;
                default:
                    throw new IllegalStateException("Illegal enum value: " + this);
            }
        }
    }

    /**
     * AuthenticatorTransport
     * https://www.w3.org/TR/webauthn/#enumdef-authenticatortransport
     */
    public enum AuthenticatorTransport {
        USB,
        NFC,
        BLE,
        HYBRID,
        INTERNAL;
    }

    /**
     * ResidentKey
     * https://www.w3.org/TR/webauthn-2/#dictdef-authenticatorselectioncriteria
     *
     * This enum is used to specify the desired behaviour for resident keys with the authenticator.
     */
    public enum ResidentKey {
        DISCOURAGED,
        PREFERRED,
        REQUIRED;

        ResidentKeyRequirement toWebAuthn4J() {
            switch (this) {
                case DISCOURAGED:
                    return ResidentKeyRequirement.DISCOURAGED;
                case PREFERRED:
                    return ResidentKeyRequirement.PREFERRED;
                case REQUIRED:
                    return ResidentKeyRequirement.REQUIRED;
                default:
                    throw new IllegalStateException("Illegal enum value: " + this);
            }
        }
    }

    /**
     * SameSite attribute values for the session cookie.
     */
    enum CookieSameSite {
        STRICT,
        LAX,
        NONE
    }

    /**
     * The origins of the application. The origin is basically protocol, host and port.
     *
     * If you are calling WebAuthn API while your application is located at {@code https://example.com/login},
     * then origin will be {@code https://example.com}.
     *
     * If you are calling from {@code http://localhost:2823/test}, then the origin will be
     * {@code http://localhost:2823}.
     *
     * Please note that WebAuthn API will not work on pages loaded over HTTP, unless it is localhost,
     * which is considered secure context.
     *
     * If unspecified, this defaults to whatever URI this application is deployed on.
     *
     * This allows more than one value if you want to allow multiple origins. See
     * https://w3c.github.io/webauthn/#sctn-related-origins
     */
    @ConfigDocDefault("The URI this application is deployed on")
    Optional<List<String>> origins();

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
     * Load the FIDO metadata for verification. See https://fidoalliance.org/metadata/. Only useful for attestations
     * different from {@code Attestation.NONE}.
     */
    @ConfigDocDefault("false")
    Optional<Boolean> loadMetadata();

    /**
     * Resident key required. A resident (private) key, is a key that cannot leave your authenticator device, this
     * means that you cannot reuse the authenticator to log into a second computer.
     */
    @ConfigDocDefault("REQUIRED")
    Optional<ResidentKey> residentKey();

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
    @ConfigDocDefault("REQUIRED")
    Optional<UserVerification> userVerification();

    /**
     * User presence requirements.
     */
    @ConfigDocDefault("true")
    Optional<Boolean> userPresenceRequired();

    /**
     * Non-negative User Verification timeout. Authentication must occur within the timeout, this will prevent the user
     * browser from being blocked with a pop-up required user verification, and the whole ceremony must be completed
     * within the timeout period. After the timeout, any previously issued challenge is automatically invalidated.
     */
    @ConfigDocDefault("5m")
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
     *
     * See https://www.w3.org/TR/webauthn-1/#dictdef-publickeycredentialparameters
     */
    @ConfigDocDefault("ES256,RS256")
    Optional<List<COSEAlgorithm>> publicKeyCredentialParameters();

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
         * The id (or domain name of your server, as obtained from the first entry of <code>origins</code> or looking
         * at where this request is being served from)
         */
        @ConfigDocDefault("The host name of the first allowed origin, or the host where this application is deployed")
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
     * SameSite attribute for the session cookie.
     */
    @WithDefault("strict")
    CookieSameSite cookieSameSite();

    /**
     * The cookie path for the session cookies.
     */
    @WithDefault("/")
    Optional<String> cookiePath();

    /**
     * Max-Age attribute for the session cookie. This is the amount of time the browser will keep the cookie.
     *
     * The default value is empty, which means the cookie will be kept until the browser is closed.
     */
    Optional<Duration> cookieMaxAge();

    /**
     * Set to <code>true</code> if you want to enable the default registration endpoint at <code>/q/webauthn/register</code>, in
     * which case
     * you should also implement the <code>WebAuthnUserProvider.store</code> method.
     */
    @WithDefault("false")
    Optional<Boolean> enableRegistrationEndpoint();

    /**
     * Set to <code>true</code> if you want to enable the default login endpoint at <code>/q/webauthn/login</code>, in which
     * case
     * you should also implement the <code>WebAuthnUserProvider.update</code> method.
     */
    @WithDefault("false")
    Optional<Boolean> enableLoginEndpoint();
}
