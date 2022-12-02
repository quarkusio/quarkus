package io.quarkus.test.security.webauthn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Random;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.impl.Codec;
import io.vertx.ext.auth.webauthn.impl.AuthData;

/**
 * Provides an emulation of a WebAuthn hardware token, suitable for generating registration
 * and login JSON objects that you can send to the Quarkus WebAuthn Security extension.
 *
 * The public/private key and id/credID are randomly generated and different for every instance,
 * and the origin is always for http://localhost
 */
public class WebAuthnHardware {

    private KeyPair keyPair;
    private String id;
    private byte[] credID;
    private int counter = 1;

    public WebAuthnHardware() {
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        keyPair = generator.generateKeyPair();
        // This can be a random number, I think
        Random random = new Random();
        credID = new byte[32];
        random.nextBytes(credID);
        id = Base64.getUrlEncoder().withoutPadding().encodeToString(credID);
    }

    /**
     * Creates a registration JSON object for the given challenge
     *
     * @param challenge the server-sent challenge
     * @return a registration JSON object
     */
    public JsonObject makeRegistrationJson(String challenge) {
        JsonObject clientData = new JsonObject()
                .put("type", "webauthn.create")
                .put("challenge", challenge)
                .put("origin", "http://localhost")
                .put("crossOrigin", false);
        String clientDataEncoded = Base64.getUrlEncoder().encodeToString(clientData.encode().getBytes(StandardCharsets.UTF_8));

        byte[] authBytes = makeAuthBytes();
        /*
         * {"fmt": "none", "attStmt": {}, "authData": h'DATAAAAA'}
         */
        CBORFactory cborFactory = new CBORFactory();
        ByteArrayOutputStream byteWriter = new ByteArrayOutputStream();
        try {
            JsonGenerator generator = cborFactory.createGenerator(byteWriter);
            generator.writeStartObject();
            generator.writeStringField("fmt", "none");
            generator.writeObjectFieldStart("attStmt");
            generator.writeEndObject();
            generator.writeBinaryField("authData", authBytes);
            generator.writeEndObject();
            generator.close();
        } catch (IOException t) {
            throw new RuntimeException(t);
        }
        String attestationObjectEncoded = Base64.getUrlEncoder().encodeToString(byteWriter.toByteArray());

        return new JsonObject()
                .put("id", id)
                .put("rawId", id)
                .put("response", new JsonObject()
                        .put("attestationObject", attestationObjectEncoded)
                        .put("clientDataJSON", clientDataEncoded))
                .put("type", "public-key");
    }

    /**
     * Creates a login JSON object for the given challenge
     *
     * @param challenge the server-sent challenge
     * @return a login JSON object
     */
    public JsonObject makeLoginJson(String challenge) {
        JsonObject clientData = new JsonObject()
                .put("type", "webauthn.get")
                .put("challenge", challenge)
                .put("origin", "http://localhost")
                .put("crossOrigin", false);
        byte[] clientDataBytes = clientData.encode().getBytes(StandardCharsets.UTF_8);
        String clientDataEncoded = Base64.getUrlEncoder().encodeToString(clientDataBytes);

        byte[] authBytes = makeAuthBytes();
        String authenticatorData = Base64.getUrlEncoder().encodeToString(authBytes);

        // sign the authbytes + hash(client data json)
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] clientDataHash = md.digest(clientDataBytes);
        byte[] signedBytes = new byte[authBytes.length + clientDataHash.length];
        System.arraycopy(authBytes, 0, signedBytes, 0, authBytes.length);
        System.arraycopy(clientDataHash, 0, signedBytes, authBytes.length, clientDataHash.length);
        byte[] signatureBytes;
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(this.keyPair.getPrivate());
            signature.update(signedBytes);
            signatureBytes = signature.sign();
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            throw new RuntimeException(e);
        }
        String signatureEncoded = Base64.getUrlEncoder().encodeToString(signatureBytes);

        return new JsonObject()
                .put("id", id)
                .put("rawId", id)
                .put("response", new JsonObject()
                        .put("authenticatorData", authenticatorData)
                        .put("clientDataJSON", clientDataEncoded)
                        .put("signature", signatureEncoded))
                .put("type", "public-key");
    }

    private byte[] makeAuthBytes() {
        Buffer buffer = Buffer.buffer();

        String rpDomain = "localhost";
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] rpIdHash = md.digest(rpDomain.getBytes(StandardCharsets.UTF_8));
        buffer.appendBytes(rpIdHash);

        byte flags = AuthData.ATTESTATION_DATA | AuthData.USER_PRESENT;
        buffer.appendByte(flags);

        long signCounter = counter++;
        buffer.appendUnsignedInt(signCounter);

        // Attested Data is present
        String aaguidString = "00000000-0000-0000-0000-000000000000";
        String aaguidStringShort = aaguidString.replace("-", "");
        byte[] aaguid = Codec.base16Decode(aaguidStringShort);
        buffer.appendBytes(aaguid);

        buffer.appendUnsignedShort(credID.length);
        buffer.appendBytes(credID);

        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        Encoder urlEncoder = Base64.getUrlEncoder();
        String x = urlEncoder.encodeToString(publicKey.getW().getAffineX().toByteArray());
        String y = urlEncoder.encodeToString(publicKey.getW().getAffineY().toByteArray());

        CBORFactory cborFactory = new CBORFactory();
        ByteArrayOutputStream byteWriter = new ByteArrayOutputStream();
        try {
            JsonGenerator generator = cborFactory.createGenerator(byteWriter);
            generator.writeStartObject();
            // see CWK and https://tools.ietf.org/html/rfc8152#section-7.1
            generator.writeNumberField("1", 2); // kty: "EC"
            generator.writeNumberField("3", -7); // alg: "ES256"
            generator.writeNumberField("-1", 1); // crv: "P-256"
            // https://tools.ietf.org/html/rfc8152#section-13.1.1
            generator.writeStringField("-2", x); // x, base64url
            generator.writeStringField("-3", y); // y, base64url
            generator.writeEndObject();
            generator.close();
        } catch (IOException t) {
            throw new RuntimeException(t);
        }
        buffer.appendBytes(byteWriter.toByteArray());

        return buffer.getBytes();
    }

}
