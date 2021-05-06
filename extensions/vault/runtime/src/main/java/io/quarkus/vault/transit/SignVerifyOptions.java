package io.quarkus.vault.transit;

public class SignVerifyOptions {

    private String signatureAlgorithm;
    private String hashAlgorithm;
    private Boolean prehashed;
    private String marshalingAlgorithm;

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public SignVerifyOptions setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
        return this;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public SignVerifyOptions setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
        return this;
    }

    public Boolean getPrehashed() {
        return prehashed;
    }

    public SignVerifyOptions setPrehashed(Boolean prehashed) {
        this.prehashed = prehashed;
        return this;
    }

    public String getMarshalingAlgorithm() {
        return marshalingAlgorithm;
    }

    public SignVerifyOptions setMarshalingAlgorithm(String marshalingAlgorithm) {
        this.marshalingAlgorithm = marshalingAlgorithm;
        return this;
    }
}
