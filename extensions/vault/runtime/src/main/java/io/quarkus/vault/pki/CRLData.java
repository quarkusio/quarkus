package io.quarkus.vault.pki;

import java.security.cert.CRLException;
import java.security.cert.X509CRL;

public interface CRLData {

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
     * Parse and generate {@link java.security.cert.X509CRL} from {@link #getData()}.
     */
    X509CRL getCRL() throws CRLException;

    /**
     * {@link DataFormat#DER} implementation of {@link CRLData}
     */
    class DER implements CRLData {

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
        public X509CRL getCRL() throws CRLException {
            return X509Parsing.parseDERCRL(derData);
        }
    }

    /**
     * {@link DataFormat#PEM} implementation of {@link CRLData}
     */
    class PEM implements CRLData {

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
        public X509CRL getCRL() throws CRLException {
            return X509Parsing.parsePEMCRL(pemData);
        }
    }

}
