package org.jboss.shamrock.jwt.runtime.auth;

public class PublicKeyProxy {
    private byte[] content;

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
