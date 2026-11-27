package io.quarkus.security.webauthn.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.vertx.core.json.JsonObject;

/**
 * Replaces the {@code none} attestation of a {@link WebAuthnHardware} registration with a {@code packed} attestation
 * signed by an attestation certificate from {@link AttestationCertificates}.
 */
final class PackedAttestation {

    /**
     * An attestation certificate issued by {@link AttestationCertificates#CA}.
     */
    static PackedAttestation trusted() {
        return new PackedAttestation(AttestationCertificates.trustedCertificate(),
                AttestationCertificates.trustedKey());
    }

    /**
     * A self-signed attestation certificate.
     */
    static PackedAttestation untrusted() {
        return new PackedAttestation(AttestationCertificates.untrustedCertificate(),
                AttestationCertificates.untrustedKey());
    }

    private final X509Certificate certificate;
    private final PrivateKey privateKey;

    private PackedAttestation(X509Certificate certificate, PrivateKey privateKey) {
        this.certificate = certificate;
        this.privateKey = privateKey;
    }

    JsonObject attest(JsonObject registration) {
        try {
            JsonObject response = registration.getJsonObject("response");
            byte[] authData = authData(Base64.getUrlDecoder().decode(response.getString("attestationObject")));
            byte[] clientDataHash = MessageDigest.getInstance("SHA-256")
                    .digest(Base64.getUrlDecoder().decode(response.getString("clientDataJSON")));

            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(authData);
            signature.update(clientDataHash);

            ByteArrayOutputStream attestationObject = new ByteArrayOutputStream();
            try (JsonGenerator generator = new CBORFactory().createGenerator(attestationObject)) {
                generator.writeStartObject();
                generator.writeStringField("fmt", "packed");
                generator.writeObjectFieldStart("attStmt");
                generator.writeNumberField("alg", -7); // ES256
                generator.writeBinaryField("sig", signature.sign());
                generator.writeArrayFieldStart("x5c");
                generator.writeBinary(certificate.getEncoded());
                generator.writeEndArray();
                generator.writeEndObject();
                generator.writeBinaryField("authData", authData);
                generator.writeEndObject();
            }
            JsonObject attested = registration.copy();
            attested.getJsonObject("response").put("attestationObject",
                    Base64.getUrlEncoder().encodeToString(attestationObject.toByteArray()));
            return attested;
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] authData(byte[] attestationObject) throws IOException {
        try (JsonParser parser = new CBORFactory().createParser(attestationObject)) {
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.FIELD_NAME && parser.currentName().equals("authData")) {
                    parser.nextToken();
                    return parser.getBinaryValue();
                }
            }
        }
        throw new IllegalArgumentException("No authData in the attestation object");
    }
}
