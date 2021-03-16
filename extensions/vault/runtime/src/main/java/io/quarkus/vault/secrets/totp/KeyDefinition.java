package io.quarkus.vault.secrets.totp;

import java.util.Objects;

public class KeyDefinition {

    private String barcode;
    private String url;

    public KeyDefinition() {
    }

    public KeyDefinition(String barcode, String url) {
        this.barcode = barcode;
        this.url = url;
    }

    /**
     * QR code in base64-formatteed PNG bytes.
     * 
     * @return Barcode.
     */
    public String getBarcode() {
        return barcode;
    }

    /**
     * URL in otpauth format (ie
     * otpauth://totp/Google:test@gmail.com?algorithm=SHA1&digits=6&issuer=Google&period=30&secret=HTXT7KJFVNAJUPYWQRWMNVQE5AF5YZI2)
     * 
     * @return URL in otpauth format.
     */
    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final KeyDefinition that = (KeyDefinition) o;
        return Objects.equals(barcode, that.barcode) &&
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(barcode, url);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("KeyDefinition{");
        sb.append("barcode='").append(barcode).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
