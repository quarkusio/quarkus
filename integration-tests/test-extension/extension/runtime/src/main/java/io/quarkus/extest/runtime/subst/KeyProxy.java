package io.quarkus.extest.runtime.subst;

/**
 * A simple proxy for the public key encoded bytes used to serialize/deserialize a public key
 */
public class KeyProxy {
    private byte[] content;

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

}
