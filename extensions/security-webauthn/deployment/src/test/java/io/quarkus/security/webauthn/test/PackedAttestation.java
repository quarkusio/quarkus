package io.quarkus.security.webauthn.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.vertx.core.json.JsonObject;

/**
 * Replaces the {@code none} attestation of a {@link WebAuthnHardware} registration with a {@code packed} attestation
 * signed by an attestation certificate.
 * <p>
 * The certificates and keys in {@code src/test/resources/attestation} are valid for 100 years. {@code authenticator.pem}
 * is issued by {@code ca.pem}, and {@code untrusted-authenticator.pem} is self-signed.
 */
final class PackedAttestation {

    static final PackedAttestation TRUSTED = new PackedAttestation("attestation/authenticator.pem",
            "attestation/authenticator-key.pem");
    static final PackedAttestation UNTRUSTED = new PackedAttestation("attestation/untrusted-authenticator.pem",
            "attestation/untrusted-authenticator-key.pem");

    private final String certificateResource;
    private final String privateKeyResource;

    private PackedAttestation(String certificateResource, String privateKeyResource) {
        this.certificateResource = certificateResource;
        this.privateKeyResource = privateKeyResource;
    }

    JsonObject attest(JsonObject registration) {
        try {
            JsonObject response = registration.getJsonObject("response");
            byte[] authData = authData(Base64.getUrlDecoder().decode(response.getString("attestationObject")));
            byte[] clientDataHash = MessageDigest.getInstance("SHA-256")
                    .digest(Base64.getUrlDecoder().decode(response.getString("clientDataJSON")));

            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey());
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
                generator.writeBinary(certificate().getEncoded());
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

    private X509Certificate certificate() throws IOException, GeneralSecurityException {
        try (InputStream in = resource(certificateResource)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }

    private PrivateKey privateKey() throws IOException, GeneralSecurityException {
        String pem;
        try (InputStream in = resource(privateKeyResource)) {
            pem = new String(in.readAllBytes(), StandardCharsets.US_ASCII);
        }
        byte[] der = Base64.getMimeDecoder().decode(pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", ""));
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static InputStream resource(String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }
}
