package io.quarkus.vault.secrets.totp;

public class CreateKeyParameters {

    private Boolean generate;
    private Boolean exported;

    private Integer keySize;

    private String url;
    private String key;
    private String issuer;

    private String accountName;

    private String period;
    private String algorithm;
    private Integer digits;
    private Integer skew;

    private Integer qrSize;

    public CreateKeyParameters(String url) {
        this.url = url;
    }

    public CreateKeyParameters(String key, String issuer, String accountName) {
        this.key = key;
        this.issuer = issuer;
        this.accountName = accountName;
    }

    /**
     * Constructs an object with generate to true as no key is provided.
     * 
     * @param issuer to set.
     * @param accountName to set.
     */
    public CreateKeyParameters(String issuer, String accountName) {
        this.issuer = issuer;
        this.accountName = accountName;
        this.generate = true;
    }

    public void setGenerate(Boolean generate) {
        this.generate = generate;
    }

    public void setExported(Boolean exported) {
        this.exported = exported;
    }

    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setDigits(Integer digits) {
        this.digits = digits;
    }

    public void setSkew(Integer skew) {
        this.skew = skew;
    }

    public void setQrSize(Integer qrSize) {
        this.qrSize = qrSize;
    }

    public Boolean getGenerate() {
        return generate;
    }

    public Boolean getExported() {
        return exported;
    }

    public Integer getKeySize() {
        return keySize;
    }

    public String getUrl() {
        return url;
    }

    public String getKey() {
        return key;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getPeriod() {
        return period;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public Integer getDigits() {
        return digits;
    }

    public Integer getSkew() {
        return skew;
    }

    public Integer getQrSize() {
        return qrSize;
    }

}
