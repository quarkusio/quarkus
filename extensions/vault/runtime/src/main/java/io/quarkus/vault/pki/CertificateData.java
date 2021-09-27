package io.quarkus.vault.pki;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public interface CertificateData {

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
     * Parse and generate {@link java.security.cert.X509Certificate} from {@link #getData()}.
     */
    X509Certificate getCertificate() throws CertificateException;

    /**
     * {@link DataFormat#DER} implementation of {@link CertificateData}
     */
    class DER implements CertificateData {

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
        public X509Certificate getCertificate() throws CertificateException {
            return X509Parsing.parseDERCertificate(derData);
        }
    }

    /**
     * {@link DataFormat#PEM} implementation of {@link CertificateData}
     */
    class PEM implements CertificateData {

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
        public X509Certificate getCertificate() throws CertificateException {
            return X509Parsing.parsePEMCertificate(pemData);
        }
    }

}
