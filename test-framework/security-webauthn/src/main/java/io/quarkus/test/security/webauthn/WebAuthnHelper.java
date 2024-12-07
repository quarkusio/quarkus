package io.quarkus.test.security.webauthn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;
import com.webauthn4j.util.Base64UrlUtil;

import io.quarkus.security.webauthn.WebAuthnLoginResponse;
import io.quarkus.security.webauthn.WebAuthnRegisterResponse;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class WebAuthnHelper {
    public static class PrettyPrinter {
        private int indent;

        private void indent() {
            for (int i = 0; i < indent; i++) {
                System.err.print(" ");
            }
        }

        public void handleToken(CBORParser parser, JsonToken t) throws IOException {
            switch (t) {
                case END_ARRAY:
                    endArray();
                    break;
                case END_OBJECT:
                    endObject();
                    break;
                case FIELD_NAME:
                    fieldName(parser.currentName());
                    break;
                case NOT_AVAILABLE:
                    break;
                case START_ARRAY:
                    startArray();
                    break;
                case START_OBJECT:
                    startObject();
                    break;
                case VALUE_EMBEDDED_OBJECT:
                    Object embeddedObject = parser.getEmbeddedObject();
                    if (parser.currentName().equals("authData")) {
                        dumpAuthData((byte[]) embeddedObject);
                    } else {
                        System.err.println(embeddedObject);
                    }
                    break;
                case VALUE_FALSE:
                    falseConstant();
                    break;
                case VALUE_NULL:
                    nullConstant();
                    break;
                case VALUE_NUMBER_FLOAT:
                    floatValue(parser.getFloatValue());
                    break;
                case VALUE_NUMBER_INT:
                    intValue(parser.getNumberValue());
                    break;
                case VALUE_STRING:
                    stringValue(parser.getValueAsString());
                    break;
                case VALUE_TRUE:
                    trueConstant();
                    break;
                default:
                    break;

            }

        }

        private void floatValue(float floatValue) {
            System.err.println(floatValue);
        }

        private void intValue(Number numberValue) {
            System.err.println(numberValue);
        }

        private void stringValue(String value) {
            System.err.println("\"" + value + "\"");
        }

        private void nullConstant() {
            System.err.println("null");
        }

        private void trueConstant() {
            System.err.println("true");
        }

        private void falseConstant() {
            System.err.println("false");
        }

        private void startObject() {
            indent();
            System.err.println("{");
            indent++;
        }

        private void startArray() {
            indent();
            System.err.println("[");
            indent++;
        }

        public void fieldName(String name) {
            indent();
            System.err.print("\"");
            System.err.print(name);
            System.err.print("\": ");
        }

        public void endObject() {
            indent();
            System.err.println("}");
            indent--;
        }

        public void endArray() {
            indent();
            System.err.println("]");
            indent--;
        }

        private void dumpAuthData(byte[] embeddedObject) throws IOException {
            Buffer buf = Buffer.buffer(embeddedObject);
            startObject();
            int current = 0;
            byte[] rpIdHash = buf.getBytes(0, 32);
            current += 32;
            fieldName("rpIdHash");
            stringValue("<some hash>"); // TODO
            byte flags = buf.getByte(current);
            current += 1;
            fieldName("flags");
            intValue(flags); // TODO in binary
            long counter = buf.getUnsignedInt(current);
            current += 4;
            fieldName("counter");
            intValue(counter);
            if (embeddedObject.length > current) {
                fieldName("attestedCredentialData");
                startObject();
                byte[] aaguid = buf.getBytes(current, current + 16);
                current += 16;
                fieldName("aaguid");
                stringValue(Base64UrlUtil.encodeToString(aaguid));

                int credentialIdLength = buf.getUnsignedShort(current);
                current += 2;
                fieldName("credentialIdLength");
                intValue(credentialIdLength);

                byte[] credentialId = buf.getBytes(current, current + credentialIdLength);
                current += credentialIdLength;
                fieldName("credentialId");
                stringValue(Base64UrlUtil.encodeToString(credentialId));

                fieldName("credentialPublicKey");
                current += readCBOR(embeddedObject, current);

                endObject();
            }
            // TODO: there's more
            endObject();
        }

        private int readCBOR(byte[] bytes, int offset) throws IOException {
            CBORFactory factory = CBORFactory.builder().build();
            long lastReadByte = offset;
            try (CBORParser parser = factory.createParser(bytes, offset, bytes.length - offset)) {
                JsonToken t;
                while ((t = parser.nextToken()) != null) {
                    //					System.err.println("Token: "+t);
                    handleToken(parser, t);
                }
                lastReadByte = parser.currentLocation().getByteOffset();
            }
            return (int) (lastReadByte - offset);
        }
    }

    public static void dumpWebAuthnRequest(JsonObject json) throws IOException {
        System.err.println(json.encodePrettily());
        JsonObject response = json.getJsonObject("response");
        if (response != null) {
            String attestationObject = response.getString("attestationObject");
            if (attestationObject != null) {
                System.err.println("Attestation object:");
                dumpAttestationObject(attestationObject);
            }
            String authenticatorData = response.getString("authenticatorData");
            if (authenticatorData != null) {
                System.err.println("Authenticator Data:");
                dumpAuthenticatorData(authenticatorData);
            }
            String clientDataJSON = response.getString("clientDataJSON");
            if (clientDataJSON != null) {
                System.err.println("Client Data JSON:");
                String encoded = new String(Base64UrlUtil.decode(clientDataJSON), StandardCharsets.UTF_8);
                System.err.println(new JsonObject(encoded).encodePrettily());
            }
        }
    }

    private static void dumpAttestationObject(String attestationObject) throws IOException {
        CBORFactory factory = CBORFactory.builder().build();
        PrettyPrinter printer = new PrettyPrinter();
        try (CBORParser parser = factory.createParser(Base64UrlUtil.decode(attestationObject))) {
            JsonToken t;
            while ((t = parser.nextToken()) != null) {
                //				System.err.println("Token: "+t);
                printer.handleToken(parser, t);
            }
        }
    }

    private static void dumpAuthenticatorData(String authenticatorData) throws IOException {
        PrettyPrinter printer = new PrettyPrinter();
        byte[] bytes = Base64UrlUtil.decode(authenticatorData);
        printer.dumpAuthData(bytes);
    }

    public static void dumpWebAuthnRequest(WebAuthnRegisterResponse response) {
        try {
            dumpWebAuthnRequest(response.toJsonObject());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void dumpWebAuthnRequest(WebAuthnLoginResponse response) {
        try {
            dumpWebAuthnRequest(response.toJsonObject());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //		WebAuthnRegisterResponse response = new WebAuthnRegisterResponse();
        //		response.webAuthnId = "N3P8WalYEtlUPMcD8q7C8hfY9tZ-DZBl7oPZNGMBxjk";
        //		response.webAuthnRawId = "N3P8WalYEtlUPMcD8q7C8hfY9tZ-DZBl7oPZNGMBxjk";
        //		response.webAuthnResponseAttestationObject = "v2NmbXRkbm9uZWdhdHRTdG10v_9oYXV0aERhdGFYxUmWDeWIDoxodDQXD2R2YFuP5K65ooYyx5lc87qDHZdjQQAAAAEAAAAAAAAAAAAAAAAAAAAAACA3c_xZqVgS2VQ8xwPyrsLyF9j21n4NkGXug9k0YwHGOb9hMQJhMyZiLTEBYi0yeCxGR0hxMHlCTWJ5X1RuOGpmWlU4XzZSTDlFNFg4ZnhJSkVOY05NN3UtSEFRPWItM3gsQUtNVEtFRG5DSzhnVVNxamRtdU45bnVzbTRRRXJNY0pBNjV6OWhJOW5TWlP__w==";
        //		response.webAuthnResponseClientDataJSON = "eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoibk9uVHRac1diSE5rRWNhOEZYY29NVUdIanJOY1c4S1BybWg0REFPQXFxaUVvRDNYdHhVT09TcXFiVXFndHlEbkEzU1VCM25YS21PRUp2WGNFZTBfVnciLCJvcmlnaW4iOiJodHRwOi8vbG9jYWxob3N0IiwiY3Jvc3NPcmlnaW4iOmZhbHNlfQ==";
        //		response.webAuthnType = "public-key";
        //		dumpWebAuthnRequest(response);
        WebAuthnLoginResponse response = new WebAuthnLoginResponse();
        response.webAuthnId = "cmokxFnWpNiqBDgI8qL41usvkUCeZC_J8EVS_jD0Brw";
        response.webAuthnRawId = "cmokxFnWpNiqBDgI8qL41usvkUCeZC_J8EVS_jD0Brw";
        response.webAuthnResponseAuthenticatorData = "SZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2NBAAAAAgAAAAAAAAAAAAAAAAAAAAAAIHJqJMRZ1qTYqgQ4CPKi-NbrL5FAnmQvyfBFUv4w9Aa8v2ExAmEzJmItMQFiLTJ4LFhCbHRrY25LZ2xjTU94bmZYSnAydE1xc2RESFBhNVB5YnIvaFJUY2tSU3c9Yi0zeCxjZWQvdHRvZGdaQjhmUGdHZ0NIM3lIUUU5NjUzVk5GdTNET2JqNFNtZ2dRPf8=";
        response.webAuthnResponseSignature = "MEUCIQDlT0NRyeElINrF59m54fsAhjVh09ykApfKzUsFw1qCVQIgHHkrPCQedlVo_fWcb7p8ch7tAT8mt3h3-GihBUP8s4o=";
        response.webAuthnResponseClientDataJSON = "eyJ0eXBlIjoid2ViYXV0aG4uZ2V0IiwiY2hhbGxlbmdlIjoiQTdYTDFvOTlINEdRZXBESDI5MnAzSUdwU0NNRHU3cUVlRVEwY3dWMU5BYm82ZzFjbC1yc2pGRHZuaVdwbE5hdk1rX1Z4YkJVcG8wdE00c2V2bXpaQUEiLCJvcmlnaW4iOiJodHRwOi8vbG9jYWxob3N0OjgwODEvIiwiY3Jvc3NPcmlnaW4iOmZhbHNlfQ==";
        response.webAuthnType = "public-key";
        dumpWebAuthnRequest(response);
    }
}
