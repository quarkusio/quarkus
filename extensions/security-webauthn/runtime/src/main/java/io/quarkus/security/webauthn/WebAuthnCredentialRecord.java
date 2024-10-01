package io.quarkus.security.webauthn;

import static io.vertx.ext.auth.impl.Codec.base64UrlDecode;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticatorTransport;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.authenticator.EdDSACOSEKey;
import com.webauthn4j.data.attestation.authenticator.RSACOSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.CollectedClientData;
import com.webauthn4j.data.extension.client.AuthenticationExtensionsClientOutputs;
import com.webauthn4j.data.extension.client.RegistrationExtensionClientOutput;
import com.webauthn4j.util.Base64UrlUtil;

/**
 * This is the internal WebAuthn4J representation for a credential record, augmented with
 * a user name. One user name can be shared among multiple credential records, but each
 * credential record has a unique credential ID.
 */
public class WebAuthnCredentialRecord extends CredentialRecordImpl {

    private String userName;

    /*
     * This is used for registering
     */
    public WebAuthnCredentialRecord(String userName,
            AttestationObject attestationObject,
            CollectedClientData clientData,
            AuthenticationExtensionsClientOutputs<RegistrationExtensionClientOutput> clientExtensions,
            Set<AuthenticatorTransport> transports) {
        super(attestationObject, clientData, clientExtensions, transports);
        this.userName = userName;
    }

    /*
     * This is used for login
     */
    private WebAuthnCredentialRecord(String userName,
            long counter,
            AttestedCredentialData attestedCredentialData) {
        super(null, null, null, null, counter, attestedCredentialData, null, null, null, null);
        this.userName = userName;
    }

    /**
     * The increasing signature counter for usage of this credential record. See
     * https://w3c.github.io/webauthn/#signature-counter
     *
     * @return The increasing signature counter.
     */
    @Override
    public long getCounter() {
        // this method is just to get rid of deprecation warnings for users.
        return super.getCounter();
    }

    /**
     * The username for this credential record
     *
     * @return the username for this credential record
     */
    public String getUserName() {
        return userName;
    }

    /**
     * The unique credential ID for this record. This is a convenience method returning a Base64Url-encoded
     * version of <code>getAttestedCredentialData().getCredentialId()</code>
     *
     * @return The unique credential ID for this record
     */
    public String getCredentialID() {
        return Base64UrlUtil.encodeToString(getAttestedCredentialData().getCredentialId());
    }

    /**
     * Returns the fields of this credential record that are necessary to persist for your users
     * to be able to log back in using WebAuthn.
     *
     * @return the fields required to be persisted.
     */
    public RequiredPersistedData getRequiredPersistedData() {
        return new RequiredPersistedData(getUserName(),
                getCredentialID(),
                getAttestedCredentialData().getAaguid().getValue(),
                getAttestedCredentialData().getCOSEKey().getPublicKey().getEncoded(),
                getAttestedCredentialData().getCOSEKey().getAlgorithm().getValue(),
                getCounter());
    }

    /**
     * Reassembles a credential record from the given required persisted fields.
     *
     * @param persistedData the required fields to be able to log back in with WebAuthn.
     * @return the internal representation of a WebAuthn credential record.
     */
    public static WebAuthnCredentialRecord fromRequiredPersistedData(RequiredPersistedData persistedData) {
        // important
        long counter = persistedData.counter();
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(persistedData.publicKey);
        COSEAlgorithmIdentifier coseAlgorithm = COSEAlgorithmIdentifier.create(persistedData.publicKeyAlgorithm);
        COSEKey coseKey;
        try {
            switch (coseAlgorithm.getKeyType()) {
                case EC2:
                    coseKey = EC2COSEKey.create((ECPublicKey) KeyFactory.getInstance("EC").generatePublic(x509EncodedKeySpec),
                            coseAlgorithm);
                    break;
                case OKP:
                    coseKey = EdDSACOSEKey
                            .create((EdECPublicKey) KeyFactory.getInstance("EdDSA").generatePublic(x509EncodedKeySpec),
                                    coseAlgorithm);
                    break;
                case RSA:
                    coseKey = RSACOSEKey
                            .create((RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(x509EncodedKeySpec),
                                    coseAlgorithm);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid cose algorithm: " + coseAlgorithm);
            }
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Invalid public key", e);
        }
        byte[] credentialId = base64UrlDecode(persistedData.credentialId());
        AAGUID aaguid = new AAGUID(persistedData.aaguid());
        AttestedCredentialData attestedCredentialData = new AttestedCredentialData(aaguid, credentialId, coseKey);

        return new WebAuthnCredentialRecord(persistedData.userName(), counter, attestedCredentialData);
    }

    /**
     * Record holding all the required persistent fields for logging back someone over WebAuthn.
     */
    public record RequiredPersistedData(
            /**
             * The user name. A single user name may be associated with multiple WebAuthn credentials.
             */
            String userName,
            /**
             * The credential ID. This must be unique. See https://w3c.github.io/webauthn/#credential-id
             */
            String credentialId,
            /**
             * See https://w3c.github.io/webauthn/#aaguid
             */
            UUID aaguid,
            /**
             * A X.509 encoding of the public key. See https://w3c.github.io/webauthn/#credential-public-key
             */
            byte[] publicKey,
            /**
             * The COSE algorithm used for signing with the public key. See
             * https://w3c.github.io/webauthn/#typedefdef-cosealgorithmidentifier
             */
            long publicKeyAlgorithm,
            /**
             * The increasing signature counter for usage of this credential record. See
             * https://w3c.github.io/webauthn/#signature-counter
             */
            long counter) {
        /**
         * Returns a PEM-encoded representation of the public key. This is a utility method you can use as an alternate for
         * storing the
         * binary public key if you do not want to store a <code>byte[]</code> and prefer strings.
         *
         * @return a PEM-encoded representation of the public key
         */
        public String getPublicKeyPEM() {
            return "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getEncoder().encodeToString(publicKey)
                    + "\n-----END PUBLIC KEY-----\n";
        }
    }
}
