package io.quarkus.vault.pki;

/**
 * Options for tidying up the engine's storage.
 */
public class TidyOptions {

    /**
     * Tidy up the certificate store?
     */
    public Boolean tidyCertStore;

    /**
     * Tidy up the revoked certificates?
     */
    public Boolean tidyRevokedCerts;

    /**
     * A duration used as a safety buffer to ensure certificates are not expunged prematurely; as an example, this
     * can keep certificates from being removed from the CRL that, due to clock skew, might still be considered valid
     * on other hosts.
     * <p>
     * Value is specified as a string duration with time suffix. Hour is the largest supported suffix.
     */
    public String safetyBuffer;

    public TidyOptions setTidyCertStore(Boolean tidyCertStore) {
        this.tidyCertStore = tidyCertStore;
        return this;
    }

    public TidyOptions setTidyRevokedCerts(Boolean tidyRevokedCerts) {
        this.tidyRevokedCerts = tidyRevokedCerts;
        return this;
    }

    public TidyOptions setSafetyBuffer(String safetyBuffer) {
        this.safetyBuffer = safetyBuffer;
        return this;
    }
}
