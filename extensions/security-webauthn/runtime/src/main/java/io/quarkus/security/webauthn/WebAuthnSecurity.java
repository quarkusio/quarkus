package io.quarkus.security.webauthn;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.webauthn4j.async.WebAuthnAsyncManager;
import com.webauthn4j.async.anchor.KeyStoreTrustAnchorAsyncRepository;
import com.webauthn4j.async.anchor.TrustAnchorAsyncRepository;
import com.webauthn4j.async.metadata.FidoMDS3MetadataBLOBAsyncProvider;
import com.webauthn4j.async.metadata.HttpAsyncClient;
import com.webauthn4j.async.metadata.anchor.MetadataBLOBBasedTrustAnchorAsyncRepository;
import com.webauthn4j.async.verifier.attestation.statement.androidkey.AndroidKeyAttestationStatementAsyncVerifier;
import com.webauthn4j.async.verifier.attestation.statement.androidsafetynet.AndroidSafetyNetAttestationStatementAsyncVerifier;
import com.webauthn4j.async.verifier.attestation.statement.apple.AppleAnonymousAttestationStatementAsyncVerifier;
import com.webauthn4j.async.verifier.attestation.statement.packed.PackedAttestationStatementAsyncVerifier;
import com.webauthn4j.async.verifier.attestation.statement.tpm.TPMAttestationStatementAsyncVerifier;
import com.webauthn4j.async.verifier.attestation.statement.u2f.FIDOU2FAttestationStatementAsyncVerifier;
import com.webauthn4j.async.verifier.attestation.trustworthiness.certpath.DefaultCertPathTrustworthinessAsyncVerifier;
import com.webauthn4j.async.verifier.attestation.trustworthiness.self.DefaultSelfAttestationTrustworthinessAsyncVerifier;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticatorSelectionCriteria;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialDescriptor;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import com.webauthn4j.data.PublicKeyCredentialRpEntity;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.PublicKeyCredentialUserEntity;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.data.extension.client.AuthenticationExtensionsClientInputs;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.util.Base64UrlUtil;

import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.webauthn.WebAuthnRunTimeConfig.Attestation;
import io.quarkus.security.webauthn.WebAuthnRunTimeConfig.AuthenticatorAttachment;
import io.quarkus.security.webauthn.WebAuthnRunTimeConfig.COSEAlgorithm;
import io.quarkus.security.webauthn.WebAuthnRunTimeConfig.ResidentKey;
import io.quarkus.security.webauthn.WebAuthnRunTimeConfig.UserVerification;
import io.quarkus.security.webauthn.impl.VertxHttpAsyncClient;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.vertx.http.runtime.security.PersistentLoginManager.RestoreResult;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.impl.CertificateHelper;
import io.vertx.ext.auth.impl.CertificateHelper.CertInfo;
import io.vertx.ext.auth.impl.jose.JWS;
import io.vertx.ext.auth.prng.VertxContextPRNG;
import io.vertx.ext.web.RoutingContext;

/**
 * Utility class that allows users to manually login or register users using WebAuthn
 */
@ApplicationScoped
public class WebAuthnSecurity {

    /*
     * Android Keystore Root is not published anywhere.
     * This certificate was extracted from one of the attestations
     * The last certificate in x5c must match this certificate
     * This needs to be checked to ensure that malicious party won't generate fake attestations
     */
    private static final String ANDROID_KEYSTORE_ROOT = "MIICizCCAjKgAwIBAgIJAKIFntEOQ1tXMAoGCCqGSM49BAMCMIGYMQswCQYDVQQG" +
            "EwJVUzETMBEGA1UECAwKQ2FsaWZvcm5pYTEWMBQGA1UEBwwNTW91bnRhaW4gVmll" +
            "dzEVMBMGA1UECgwMR29vZ2xlLCBJbmMuMRAwDgYDVQQLDAdBbmRyb2lkMTMwMQYD" +
            "VQQDDCpBbmRyb2lkIEtleXN0b3JlIFNvZnR3YXJlIEF0dGVzdGF0aW9uIFJvb3Qw" +
            "HhcNMTYwMTExMDA0MzUwWhcNMzYwMTA2MDA0MzUwWjCBmDELMAkGA1UEBhMCVVMx" +
            "EzARBgNVBAgMCkNhbGlmb3JuaWExFjAUBgNVBAcMDU1vdW50YWluIFZpZXcxFTAT" +
            "BgNVBAoMDEdvb2dsZSwgSW5jLjEQMA4GA1UECwwHQW5kcm9pZDEzMDEGA1UEAwwq" +
            "QW5kcm9pZCBLZXlzdG9yZSBTb2Z0d2FyZSBBdHRlc3RhdGlvbiBSb290MFkwEwYH" +
            "KoZIzj0CAQYIKoZIzj0DAQcDQgAE7l1ex+HA220Dpn7mthvsTWpdamguD/9/SQ59" +
            "dx9EIm29sa/6FsvHrcV30lacqrewLVQBXT5DKyqO107sSHVBpKNjMGEwHQYDVR0O" +
            "BBYEFMit6XdMRcOjzw0WEOR5QzohWjDPMB8GA1UdIwQYMBaAFMit6XdMRcOjzw0W" +
            "EOR5QzohWjDPMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgKEMAoGCCqG" +
            "SM49BAMCA0cAMEQCIDUho++LNEYenNVg8x1YiSBq3KNlQfYNns6KGYxmSGB7AiBN" +
            "C/NR2TB8fVvaNTQdqEcbY6WFZTytTySn502vQX3xvw==";

    // https://aboutssl.org/globalsign-root-certificates-licensing-and-use/
    //  Name 	gsr1
    // Thumbprint: b1:bc:96:8b:d4:f4:9d:62:2a:a8:9a:81:f2:15:01:52:a4:1d:82:9c
    //  Valid Until 	28 January 2028
    private static final String GSR1 = "MIIDdTCCAl2gAwIBAgILBAAAAAABFUtaw5QwDQYJKoZIhvcNAQEFBQAwVzELMAkG\n" +
            "A1UEBhMCQkUxGTAXBgNVBAoTEEdsb2JhbFNpZ24gbnYtc2ExEDAOBgNVBAsTB1Jv\n" +
            "b3QgQ0ExGzAZBgNVBAMTEkdsb2JhbFNpZ24gUm9vdCBDQTAeFw05ODA5MDExMjAw\n" +
            "MDBaFw0yODAxMjgxMjAwMDBaMFcxCzAJBgNVBAYTAkJFMRkwFwYDVQQKExBHbG9i\n" +
            "YWxTaWduIG52LXNhMRAwDgYDVQQLEwdSb290IENBMRswGQYDVQQDExJHbG9iYWxT\n" +
            "aWduIFJvb3QgQ0EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDaDuaZ\n" +
            "jc6j40+Kfvvxi4Mla+pIH/EqsLmVEQS98GPR4mdmzxzdzxtIK+6NiY6arymAZavp\n" +
            "xy0Sy6scTHAHoT0KMM0VjU/43dSMUBUc71DuxC73/OlS8pF94G3VNTCOXkNz8kHp\n" +
            "1Wrjsok6Vjk4bwY8iGlbKk3Fp1S4bInMm/k8yuX9ifUSPJJ4ltbcdG6TRGHRjcdG\n" +
            "snUOhugZitVtbNV4FpWi6cgKOOvyJBNPc1STE4U6G7weNLWLBYy5d4ux2x8gkasJ\n" +
            "U26Qzns3dLlwR5EiUWMWea6xrkEmCMgZK9FGqkjWZCrXgzT/LCrBbBlDSgeF59N8\n" +
            "9iFo7+ryUp9/k5DPAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNVHRMBAf8E\n" +
            "BTADAQH/MB0GA1UdDgQWBBRge2YaRQ2XyolQL30EzTSo//z9SzANBgkqhkiG9w0B\n" +
            "AQUFAAOCAQEA1nPnfE920I2/7LqivjTFKDK1fPxsnCwrvQmeU79rXqoRSLblCKOz\n" +
            "yj1hTdNGCbM+w6DjY1Ub8rrvrTnhQ7k4o+YviiY776BQVvnGCv04zcQLcFGUl5gE\n" +
            "38NflNUVyRRBnMRddWQVDf9VMOyGj/8N7yy5Y0b2qvzfvGn9LhJIZJrglfCm7ymP\n" +
            "AbEVtQwdpf5pLGkkeB6zpxxxYu7KyJesF12KwvhHhm4qxFYxldBniYUr+WymXUad\n" +
            "DKqC5JlR3XC321Y9YeRq4VzW9v493kHMB65jUr9TU/Qr6cf9tveCX4XSQRjbgbME\n" +
            "HMUfpIBvFSDJ3gyICh3WZlXi/EjJKSZp4A==";

    /**
     * Apple WebAuthn Root CA PEM
     * <p>
     * Downloaded from https://www.apple.com/certificateauthority/Apple_WebAuthn_Root_CA.pem
     * <p>
     * Valid until 03/14/2045 @ 5:00 PM PST
     */
    private static final String APPLE_WEBAUTHN_ROOT_CA = "MIICEjCCAZmgAwIBAgIQaB0BbHo84wIlpQGUKEdXcTAKBggqhkjOPQQDAzBLMR8w" +
            "HQYDVQQDDBZBcHBsZSBXZWJBdXRobiBSb290IENBMRMwEQYDVQQKDApBcHBsZSBJ" +
            "bmMuMRMwEQYDVQQIDApDYWxpZm9ybmlhMB4XDTIwMDMxODE4MjEzMloXDTQ1MDMx" +
            "NTAwMDAwMFowSzEfMB0GA1UEAwwWQXBwbGUgV2ViQXV0aG4gUm9vdCBDQTETMBEG" +
            "A1UECgwKQXBwbGUgSW5jLjETMBEGA1UECAwKQ2FsaWZvcm5pYTB2MBAGByqGSM49" +
            "AgEGBSuBBAAiA2IABCJCQ2pTVhzjl4Wo6IhHtMSAzO2cv+H9DQKev3//fG59G11k" +
            "xu9eI0/7o6V5uShBpe1u6l6mS19S1FEh6yGljnZAJ+2GNP1mi/YK2kSXIuTHjxA/" +
            "pcoRf7XkOtO4o1qlcaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUJtdk" +
            "2cV4wlpn0afeaxLQG2PxxtcwDgYDVR0PAQH/BAQDAgEGMAoGCCqGSM49BAMDA2cA" +
            "MGQCMFrZ+9DsJ1PW9hfNdBywZDsWDbWFp28it1d/5w2RPkRX3Bbn/UbDTNLx7Jr3" +
            "jAGGiQIwHFj+dJZYUJR786osByBelJYsVZd2GbHQu209b5RCmGQ21gpSAk9QZW4B" +
            "1bWeT0vT";

    /**
     * Default FIDO2 MDS3 ROOT Certificate
     * <p>
     * Downloaded from https://valid.r3.roots.globalsign.com/
     * <p>
     * Valid until 18 March 2029
     */
    private static final String FIDO_MDS3_ROOT_CERTIFICATE = "MIIDXzCCAkegAwIBAgILBAAAAAABIVhTCKIwDQYJKoZIhvcNAQELBQAwTDEgMB4G"
            +
            "A1UECxMXR2xvYmFsU2lnbiBSb290IENBIC0gUjMxEzARBgNVBAoTCkdsb2JhbFNp" +
            "Z24xEzARBgNVBAMTCkdsb2JhbFNpZ24wHhcNMDkwMzE4MTAwMDAwWhcNMjkwMzE4" +
            "MTAwMDAwWjBMMSAwHgYDVQQLExdHbG9iYWxTaWduIFJvb3QgQ0EgLSBSMzETMBEG" +
            "A1UEChMKR2xvYmFsU2lnbjETMBEGA1UEAxMKR2xvYmFsU2lnbjCCASIwDQYJKoZI" +
            "hvcNAQEBBQADggEPADCCAQoCggEBAMwldpB5BngiFvXAg7aEyiie/QV2EcWtiHL8" +
            "RgJDx7KKnQRfJMsuS+FggkbhUqsMgUdwbN1k0ev1LKMPgj0MK66X17YUhhB5uzsT" +
            "gHeMCOFJ0mpiLx9e+pZo34knlTifBtc+ycsmWQ1z3rDI6SYOgxXG71uL0gRgykmm" +
            "KPZpO/bLyCiR5Z2KYVc3rHQU3HTgOu5yLy6c+9C7v/U9AOEGM+iCK65TpjoWc4zd" +
            "QQ4gOsC0p6Hpsk+QLjJg6VfLuQSSaGjlOCZgdbKfd/+RFO+uIEn8rUAVSNECMWEZ" +
            "XriX7613t2Saer9fwRPvm2L7DWzgVGkWqQPabumDk3F2xmmFghcCAwEAAaNCMEAw" +
            "DgYDVR0PAQH/BAQDAgEGMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0OBBYEFI/wS3+o" +
            "LkUkrk1Q+mOai97i3Ru8MA0GCSqGSIb3DQEBCwUAA4IBAQBLQNvAUKr+yAzv95ZU" +
            "RUm7lgAJQayzE4aGKAczymvmdLm6AC2upArT9fHxD4q/c2dKg8dEe3jgr25sbwMp" +
            "jjM5RcOO5LlXbKr8EpbsU8Yt5CRsuZRj+9xTaGdWPoO4zzUhw8lo/s7awlOqzJCK" +
            "6fBdRoyV3XpYKBovHd7NADdBj+1EbddTKJd+82cEHhXXipa0095MJ6RMG3NzdvQX" +
            "mcIfeg7jLQitChws/zyrVQ4PkX4268NXSb7hLi18YIvDQVETI53O9zJrlAGomecs" +
            "Mx86OyXShkDOOyyGeMlhLxS67ttVb9+E7gUJTb0o2HLO02JQZR7rkpeDMdmztcpH" +
            "WD9f";

    @Inject
    TlsConfigurationRegistry certificates;

    @Inject
    WebAuthnAuthenticationMechanism authMech;

    @Inject
    WebAuthnAuthenticatorStorage storage;

    private ObjectConverter objectConverter = new ObjectConverter();
    private WebAuthnAsyncManager webAuthn;
    private VertxContextPRNG random;

    private String challengeCookie;
    private String challengeUsernameCookie;

    private List<String> origins;
    private String rpId;
    private String rpName;

    private UserVerification userVerification;
    private Boolean userPresenceRequired;
    private List<PublicKeyCredentialParameters> pubKeyCredParams;
    private ResidentKey residentKey;

    private Duration timeout;
    private int challengeLength;
    private AuthenticatorAttachment authenticatorAttachment;

    private Attestation attestation;

    public WebAuthnSecurity(WebAuthnRunTimeConfig config, Vertx vertx, WebAuthnAuthenticatorStorage database) {
        // apply config defaults
        this.rpId = config.relyingParty().id().orElse(null);
        this.rpName = config.relyingParty().name();
        this.origins = config.origins().orElse(Collections.emptyList());
        this.challengeCookie = config.challengeCookieName();
        this.challengeUsernameCookie = config.challengeUsernameCookieName();
        this.challengeLength = config.challengeLength().orElse(64);
        this.userPresenceRequired = config.userPresenceRequired().orElse(true);
        this.timeout = config.timeout().orElse(Duration.ofMinutes(5));
        if (config.publicKeyCredentialParameters().isPresent()) {
            this.pubKeyCredParams = new ArrayList<>(config.publicKeyCredentialParameters().get().size());
            for (COSEAlgorithm publicKeyCredential : config.publicKeyCredentialParameters().get()) {
                this.pubKeyCredParams.add(new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY,
                        COSEAlgorithmIdentifier.create(publicKeyCredential.coseId())));
            }
        } else {
            this.pubKeyCredParams = new ArrayList<>(2);
            this.pubKeyCredParams
                    .add(new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256));
            this.pubKeyCredParams
                    .add(new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256));
        }
        this.authenticatorAttachment = config.authenticatorAttachment().orElse(null);
        this.userVerification = config.userVerification().orElse(UserVerification.REQUIRED);
        this.residentKey = config.residentKey().orElse(ResidentKey.REQUIRED);
        this.attestation = config.attestation().orElse(Attestation.NONE);
        // create the webauthn4j manager
        this.webAuthn = makeWebAuthn(vertx, config);
        this.random = VertxContextPRNG.current(vertx);
    }

    private String randomBase64URLBuffer() {
        final byte[] buff = new byte[challengeLength];
        random.nextBytes(buff);
        return Base64UrlUtil.encodeToString(buff);
    }

    private WebAuthnAsyncManager makeWebAuthn(Vertx vertx, WebAuthnRunTimeConfig config) {
        if (config.attestation().isPresent()
                && config.attestation().get() != WebAuthnRunTimeConfig.Attestation.NONE) {
            TrustAnchorAsyncRepository something;
            // FIXME: make config name configurable?
            Optional<TlsConfiguration> webauthnTlsConfiguration = certificates.get("webauthn");
            KeyStore trustStore;
            if (webauthnTlsConfiguration.isPresent()) {
                trustStore = webauthnTlsConfiguration.get().getTrustStore();
            } else {
                try {
                    trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    trustStore.load(null, null);
                    addCert(trustStore, ANDROID_KEYSTORE_ROOT);
                    addCert(trustStore, APPLE_WEBAUTHN_ROOT_CA);
                    addCert(trustStore, FIDO_MDS3_ROOT_CERTIFICATE);
                    addCert(trustStore, GSR1);
                } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | IOException e) {
                    throw new RuntimeException("Failed to configure default WebAuthn certificates", e);
                }
            }
            Set<TrustAnchor> trustAnchors = new HashSet<>();
            try {
                Enumeration<String> aliases = trustStore.aliases();
                while (aliases.hasMoreElements()) {
                    trustAnchors.add(new TrustAnchor((X509Certificate) trustStore.getCertificate(aliases.nextElement()), null));
                }
            } catch (KeyStoreException e) {
                throw new RuntimeException("Failed to configure WebAuthn trust store", e);
            }
            // FIXME CLRs are not supported yet
            something = new KeyStoreTrustAnchorAsyncRepository(trustStore);
            if (config.loadMetadata().orElse(false)) {
                HttpAsyncClient httpClient = new VertxHttpAsyncClient(vertx);
                FidoMDS3MetadataBLOBAsyncProvider blobAsyncProvider = new FidoMDS3MetadataBLOBAsyncProvider(objectConverter,
                        FidoMDS3MetadataBLOBAsyncProvider.DEFAULT_BLOB_ENDPOINT, httpClient, trustAnchors);
                something = new MetadataBLOBBasedTrustAnchorAsyncRepository(blobAsyncProvider);
            }

            return new WebAuthnAsyncManager(
                    Arrays.asList(
                            new FIDOU2FAttestationStatementAsyncVerifier(),
                            new PackedAttestationStatementAsyncVerifier(),
                            new TPMAttestationStatementAsyncVerifier(),
                            new AndroidKeyAttestationStatementAsyncVerifier(),
                            new AndroidSafetyNetAttestationStatementAsyncVerifier(),
                            new AppleAnonymousAttestationStatementAsyncVerifier()),
                    new DefaultCertPathTrustworthinessAsyncVerifier(something),
                    new DefaultSelfAttestationTrustworthinessAsyncVerifier(),
                    objectConverter);

        } else {
            return WebAuthnAsyncManager.createNonStrictWebAuthnAsyncManager(objectConverter);
        }
    }

    private void addCert(KeyStore keyStore, String pemCertificate) throws CertificateException, KeyStoreException {
        X509Certificate cert = JWS.parseX5c(pemCertificate);
        CertInfo info = CertificateHelper.getCertInfo(cert);
        keyStore.setCertificateEntry(info.subject("CN"), cert);
    }

    private static byte[] uUIDBytes(UUID uuid) {
        Buffer buffer = Buffer.buffer(16);
        buffer.setLong(0, uuid.getMostSignificantBits());
        buffer.setLong(8, uuid.getLeastSignificantBits());
        return buffer.getBytes();
    }

    /**
     * Obtains a registration challenge for the given required userName and displayName. This will also
     * create and save a challenge in a session cookie.
     *
     * @param userName the userName for the registration
     * @param displayName the displayName for the registration
     * @param ctx the Vert.x context
     * @return the registration challenge.
     */
    @SuppressWarnings("unused")
    public Uni<PublicKeyCredentialCreationOptions> getRegisterChallenge(String userName, String displayName,
            RoutingContext ctx) {
        if (userName == null || userName.isEmpty()) {
            return Uni.createFrom().failure(new IllegalArgumentException("Username is required"));
        }
        // default displayName to userName, but it's required really
        if (displayName == null || displayName.isEmpty()) {
            displayName = userName;
        }
        String finalDisplayName = displayName;
        String challenge = randomBase64URLBuffer();
        Origin origin = Origin.create(!this.origins.isEmpty() ? this.origins.get(0) : ctx.request().absoluteURI());
        String rpId = this.rpId != null ? this.rpId : origin.getHost();

        return storage.findByUserName(userName)
                .map(credentials -> {
                    List<PublicKeyCredentialDescriptor> excluded;
                    // See https://github.com/quarkusio/quarkus/issues/44292 for why this is currently disabled
                    if (false) {
                        excluded = new ArrayList<>(credentials.size());
                        for (WebAuthnCredentialRecord credential : credentials) {
                            excluded.add(new PublicKeyCredentialDescriptor(PublicKeyCredentialType.PUBLIC_KEY,
                                    credential.getAttestedCredentialData().getCredentialId(),
                                    credential.getTransports()));
                        }
                    } else {
                        excluded = Collections.emptyList();
                    }
                    PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions = new PublicKeyCredentialCreationOptions(
                            new PublicKeyCredentialRpEntity(
                                    rpId,
                                    rpName),
                            new PublicKeyCredentialUserEntity(
                                    uUIDBytes(UUID.randomUUID()),
                                    userName,
                                    finalDisplayName),
                            new DefaultChallenge(challenge),
                            pubKeyCredParams,
                            timeout.getSeconds() * 1000,
                            excluded,
                            new AuthenticatorSelectionCriteria(
                                    authenticatorAttachment != null ? authenticatorAttachment.toWebAuthn4J() : null,
                                    residentKey == ResidentKey.REQUIRED,
                                    residentKey.toWebAuthn4J(),
                                    userVerification.toWebAuthn4J()),
                            attestation.toWebAuthn4J(),
                            new AuthenticationExtensionsClientInputs<>());

                    // save challenge to the session
                    authMech.getLoginManager().save(challenge, ctx, challengeCookie, null,
                            ctx.request().isSSL());
                    authMech.getLoginManager().save(userName, ctx, challengeUsernameCookie, null,
                            ctx.request().isSSL());

                    return publicKeyCredentialCreationOptions;
                });

    }

    /**
     * Obtains a login challenge for the given optional userName. This will also
     * create and save a challenge in a session cookie.
     *
     * @param userName the optional userName for the login
     * @param ctx the Vert.x context
     * @return the login challenge.
     */
    @SuppressWarnings("unused")
    public Uni<PublicKeyCredentialRequestOptions> getLoginChallenge(String userName, RoutingContext ctx) {
        // Username is not required with passkeys
        if (userName == null) {
            userName = "";
        }
        String finalUserName = userName;
        String challenge = randomBase64URLBuffer();
        Origin origin = Origin.create(!this.origins.isEmpty() ? this.origins.get(0) : ctx.request().absoluteURI());
        String rpId = this.rpId != null ? this.rpId : origin.getHost();

        // do not attempt to look users up if there's no user name
        Uni<List<WebAuthnCredentialRecord>> credentialsUni;
        if (userName.isEmpty()) {
            credentialsUni = Uni.createFrom().item(Collections.emptyList());
        } else {
            credentialsUni = storage.findByUserName(userName);
        }
        return credentialsUni
                .map(credentials -> {
                    List<PublicKeyCredentialDescriptor> allowedCredentials;
                    // See https://github.com/quarkusio/quarkus/issues/44292 for why this is currently disabled
                    if (false) {

                        if (credentials.isEmpty()) {
                            throw new RuntimeException("No credentials found for " + finalUserName);
                        }
                        allowedCredentials = new ArrayList<>(credentials.size());
                        for (WebAuthnCredentialRecord credential : credentials) {
                            allowedCredentials.add(new PublicKeyCredentialDescriptor(PublicKeyCredentialType.PUBLIC_KEY,
                                    credential.getAttestedCredentialData().getCredentialId(),
                                    credential.getTransports()));
                        }
                    } else {
                        allowedCredentials = Collections.emptyList();
                    }
                    PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions = new PublicKeyCredentialRequestOptions(
                            new DefaultChallenge(challenge),
                            timeout.getSeconds() * 1000,
                            rpId,
                            allowedCredentials,
                            userVerification.toWebAuthn4J(),
                            null);

                    // save challenge to the session
                    authMech.getLoginManager().save(challenge, ctx, challengeCookie, null,
                            ctx.request().isSSL());
                    authMech.getLoginManager().save(finalUserName, ctx, challengeUsernameCookie, null,
                            ctx.request().isSSL());

                    return publicKeyCredentialRequestOptions;
                });
    }

    /**
     * Registers a new WebAuthn credentials. This will check it, clear the cookies and return it in case of
     * success, but not invoke {@link WebAuthnUserProvider#store(WebAuthnCredentialRecord)}, you have to do
     * it manually in case of success. This will also not set a login cookie, you have to do it manually using
     * {@link #rememberUser(String, RoutingContext)}
     * or using any other way.
     *
     * @param response the Webauthn registration info
     * @param ctx the current request
     * @return the newly created credentials
     */
    public Uni<WebAuthnCredentialRecord> register(WebAuthnRegisterResponse response, RoutingContext ctx) {
        return register(response.toJsonObject(), ctx);
    }

    /**
     * Registers a new WebAuthn credentials. This will check it, clear the cookies and return it in case of
     * success, but not invoke {@link WebAuthnUserProvider#store(WebAuthnCredentialRecord)}, you have to do
     * it manually in case of success. This will also not set a login cookie, you have to do it manually using
     * {@link #rememberUser(String, RoutingContext)}
     * or using any other way.
     *
     * @param response the Webauthn registration info
     * @param ctx the current request
     * @return the newly created credentials
     */
    public Uni<WebAuthnCredentialRecord> register(JsonObject response, RoutingContext ctx) {
        RestoreResult challenge = authMech.getLoginManager().restore(ctx, challengeCookie);
        RestoreResult username = authMech.getLoginManager().restore(ctx, challengeUsernameCookie);
        if (challenge == null || challenge.getPrincipal() == null || challenge.getPrincipal().isEmpty()
                || username == null || username.getPrincipal() == null || username.getPrincipal().isEmpty()) {
            return Uni.createFrom().failure(new RuntimeException("Missing challenge or username"));
        }

        // input validation
        if (response == null ||
                !containsRequiredString(response, "id") ||
                !containsRequiredString(response, "rawId") ||
                !containsRequiredObject(response, "response") ||
                !containsOptionalString(response.getJsonObject("response"), "userHandle") ||
                !containsRequiredString(response, "type") ||
                !"public-key".equals(response.getString("type"))) {

            return Uni.createFrom().failure(new IllegalArgumentException(
                    "Response missing one or more of id/rawId/response[.userHandle]/type fields, or type is not public-key"));
        }
        String registrationResponseJSON = response.encode();

        ServerProperty serverProperty = makeServerProperty(challenge, ctx);
        RegistrationParameters registrationParameters = new RegistrationParameters(serverProperty, pubKeyCredParams,
                userVerification == UserVerification.REQUIRED, userPresenceRequired);

        return Uni.createFrom()
                .completionStage(webAuthn.verifyRegistrationResponseJSON(registrationResponseJSON, registrationParameters))
                .eventually(() -> {
                    removeCookie(ctx, challengeCookie);
                    removeCookie(ctx, challengeUsernameCookie);
                }).map(registrationData -> new WebAuthnCredentialRecord(
                        username.getPrincipal(),
                        registrationData.getAttestationObject(),
                        registrationData.getCollectedClientData(),
                        registrationData.getClientExtensions(),
                        registrationData.getTransports()));
    }

    private ServerProperty makeServerProperty(RestoreResult challenge, RoutingContext ctx) {
        Set<Origin> origins = new HashSet<>();
        Origin firstOrigin = null;
        if (this.origins.isEmpty()) {
            firstOrigin = Origin.create(ctx.request().absoluteURI());
            origins.add(firstOrigin);
        } else {
            for (String origin : this.origins) {
                Origin newOrigin = Origin.create(origin);
                if (firstOrigin == null) {
                    firstOrigin = newOrigin;
                    origins.add(newOrigin);
                }
            }
        }
        String rpId = this.rpId != null ? this.rpId : firstOrigin.getHost();
        DefaultChallenge challengeObject = new DefaultChallenge(challenge.getPrincipal());
        return new ServerProperty(origins, rpId, challengeObject, /* this is deprecated in Level 3, so ignore it */ null);
    }

    /**
     * Logs an existing WebAuthn user in. This will check it, clear the cookies and return the updated credentials in case of
     * success, but not invoke {@link WebAuthnUserProvider#update(String, long)}, you have to do
     * it manually in case of success. This will also not set a login cookie, you have to do it manually using
     * {@link #rememberUser(String, RoutingContext)}
     * or using any other way.
     *
     * @param response the Webauthn login info
     * @param ctx the current request
     * @return the updated credentials
     */
    public Uni<WebAuthnCredentialRecord> login(WebAuthnLoginResponse response, RoutingContext ctx) {
        return login(response.toJsonObject(), ctx);
    }

    /**
     * Logs an existing WebAuthn user in. This will check it, clear the cookies and return the updated credentials in case of
     * success, but not invoke {@link WebAuthnUserProvider#update(String, long)}, you have to do
     * it manually in case of success. This will also not set a login cookie, you have to do it manually using
     * {@link #rememberUser(String, RoutingContext)}
     * or using any other way.
     *
     * @param response the Webauthn login info
     * @param ctx the current request
     * @return the updated credentials
     */
    public Uni<WebAuthnCredentialRecord> login(JsonObject response, RoutingContext ctx) {
        RestoreResult challenge = authMech.getLoginManager().restore(ctx, challengeCookie);
        RestoreResult username = authMech.getLoginManager().restore(ctx, challengeUsernameCookie);
        if (challenge == null || challenge.getPrincipal() == null || challenge.getPrincipal().isEmpty()
        // although login can be empty, we should still have a cookie for it
                || username == null || username.getPrincipal() == null) {
            return Uni.createFrom().failure(new RuntimeException("Missing challenge or username"));
        }

        // input validation
        if (response == null ||
                !containsRequiredString(response, "id") ||
                !containsRequiredString(response, "rawId") ||
                !containsRequiredObject(response, "response") ||
                !containsOptionalString(response.getJsonObject("response"), "userHandle") ||
                !containsRequiredString(response, "type") ||
                !"public-key".equals(response.getString("type"))) {

            return Uni.createFrom().failure(new IllegalArgumentException(
                    "Response missing one or more of id/rawId/response[.userHandle]/type fields, or type is not public-key"));
        }

        String authenticationResponseJSON = response.encode();
        // validated
        String rawId = response.getString("rawId");

        ServerProperty serverProperty = makeServerProperty(challenge, ctx);

        return storage.findByCredID(rawId)
                .chain(credentialRecord -> {
                    List<byte[]> allowCredentials = List.of(Base64UrlUtil.decode(rawId));
                    AuthenticationParameters authenticationParameters = new AuthenticationParameters(serverProperty,
                            credentialRecord, allowCredentials,
                            userVerification == UserVerification.REQUIRED, userPresenceRequired);

                    return Uni.createFrom()
                            .completionStage(webAuthn.verifyAuthenticationResponseJSON(authenticationResponseJSON,
                                    authenticationParameters))
                            .eventually(() -> {
                                removeCookie(ctx, challengeCookie);
                                removeCookie(ctx, challengeUsernameCookie);
                            }).map(authenticationData -> credentialRecord);
                });
    }

    static void removeCookie(RoutingContext ctx, String name) {
        // Vert.x sends back a set-cookie with max-age and expiry but no path, so we have to set it first,
        // otherwise web clients don't clear it
        Cookie cookie = ctx.request().getCookie(name);
        if (cookie != null) {
            cookie.setPath("/");
        }
        ctx.response().removeCookie(name);
    }

    /**
     * Returns the underlying WebAuthn4J authenticator
     *
     * @return the underlying WebAuthn4J authenticator
     */
    public WebAuthnAsyncManager getWebAuthn4J() {
        return webAuthn;
    }

    /**
     * Adds a login cookie to the current request for the given user ID
     *
     * @param userID the user ID to use as {@link Principal}
     * @param ctx the current request, in order to add a cookie
     */
    public void rememberUser(String userID, RoutingContext ctx) {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(new QuarkusPrincipal(userID));
        authMech.getLoginManager().save(builder.build(), ctx, null, ctx.request().isSSL());
    }

    /**
     * Clears the login cookie on the current request
     *
     * @param ctx the current request, in order to clear the login cookie
     */
    public void logout(RoutingContext ctx) {
        authMech.getLoginManager().clear(ctx);
    }

    static boolean containsRequiredString(JsonObject json, String key) {
        try {
            if (json == null) {
                return false;
            }
            if (!json.containsKey(key)) {
                return false;
            }
            Object s = json.getValue(key);
            return (s instanceof String) && !"".equals(s);
        } catch (ClassCastException e) {
            return false;
        }
    }

    private static boolean containsOptionalString(JsonObject json, String key) {
        try {
            if (json == null) {
                return true;
            }
            if (!json.containsKey(key)) {
                return true;
            }
            Object s = json.getValue(key);
            return (s instanceof String);
        } catch (ClassCastException e) {
            return false;
        }
    }

    private static boolean containsRequiredObject(JsonObject json, String key) {
        try {
            if (json == null) {
                return false;
            }
            if (!json.containsKey(key)) {
                return false;
            }
            JsonObject s = json.getJsonObject(key);
            return s != null;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toJsonString(PublicKeyCredentialCreationOptions challenge) {
        return objectConverter.getJsonConverter().writeValueAsString(challenge);
    }

    public String toJsonString(PublicKeyCredentialRequestOptions challenge) {
        return objectConverter.getJsonConverter().writeValueAsString(challenge);
    }

    /**
     * Returns the list of allowed origins, or defaults to the current request's origin if unconfigured.
     */
    public List<String> getAllowedOrigins(RoutingContext ctx) {
        if (this.origins.isEmpty()) {
            return List.of(Origin.create(ctx.request().absoluteURI()).toString());
        } else {
            return this.origins;
        }
    }

    WebAuthnAuthenticatorStorage storage() {
        return storage;
    }
}
