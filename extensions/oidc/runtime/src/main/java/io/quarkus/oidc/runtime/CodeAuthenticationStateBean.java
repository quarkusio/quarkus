package io.quarkus.oidc.runtime;

public class CodeAuthenticationStateBean {

    private String restorePath;

    private String codeVerifier;

    private String nonce;

    public String getRestorePath() {
        return restorePath;
    }

    public void setRestorePath(String restorePath) {
        this.restorePath = restorePath;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public void setCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public boolean isEmpty() {
        return this.restorePath == null && this.codeVerifier == null && nonce == null;
    }

}
