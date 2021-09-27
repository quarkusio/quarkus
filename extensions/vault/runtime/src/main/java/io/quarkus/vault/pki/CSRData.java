package io.quarkus.vault.pki;

public interface CSRData {

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
     * {@link DataFormat#DER} implementation of {@link CSRData}
     */
    class DER implements CSRData {

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
    }

    /**
     * {@link DataFormat#PEM} implementation of {@link CSRData}
     */
    class PEM implements CSRData {

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
    }

}
