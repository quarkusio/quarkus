package io.quarkus.vault.pki;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

public interface CAChainData {

    /**
     * Format of {@link #getData()} property.
     */
    DataFormat getFormat();

    /**
     * Data in {@link DataFormat#PEM} or {@link DataFormat#DER} format.
     *
     * @see #getFormat()
     */
    Object getData();

    /**
     * Parse and generate {@link java.security.cert.X509Certificate}s from {@link #getData()}.
     */
    List<X509Certificate> getCertificates() throws CertificateException;

    /**
     * {@link DataFormat#DER} implementation of {@link CAChainData}
     */
    class DER implements CAChainData {

        private final byte[] derData;

        public DER(byte[] derData) {
            this.derData = derData;
        }

        @Override
        public DataFormat getFormat() {
            return DataFormat.DER;
        }

        @Override
        public byte[] getData() {
            return derData;
        }

        @Override
        public List<X509Certificate> getCertificates() throws CertificateException {
            return X509Parsing.parseDERCertificates(derData);
        }
    }

    /**
     * {@link DataFormat#PEM} implementation of {@link CAChainData}
     */
    class PEM implements CAChainData {

        private final String pemData;

        public PEM(String pemData) {
            this.pemData = pemData;
        }

        @Override
        public DataFormat getFormat() {
            return DataFormat.PEM;
        }

        @Override
        public String getData() {
            return pemData;
        }

        @Override
        public List<X509Certificate> getCertificates() throws CertificateException {
            return X509Parsing.parsePEMCertificates(pemData);
        }
    }

}
