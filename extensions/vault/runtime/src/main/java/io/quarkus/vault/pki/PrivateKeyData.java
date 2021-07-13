package io.quarkus.vault.pki;

import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public interface PrivateKeyData {

    /**
     * Format of {@link #getData()} property.
     */
    DataFormat getFormat();

    /**
     * Data in {@link DataFormat#DER} or {@link DataFormat#PEM} format.
     *
     * @see #getFormat()
     */
    Object getData();

    /**
     * Is {@link #getData() data} encoded in PKCS8 format?
     */
    boolean isPKCS8();

    /**
     * Parse and generate {@link KeySpec} from {@link #getData()}.
     *
     * @implNote This currently only works with PKCS8 encoded data.
     * @throws IllegalStateException When called on non-PKCS8 encoded data.
     */
    KeySpec getKeySpec();

    /**
     * {@link DataFormat#DER} implementation of {@link PrivateKeyData}
     */
    class DER implements PrivateKeyData {

        private final byte[] derData;
        private final boolean pkcs8;

        public DER(byte[] derData, boolean pkcs8) {
            this.derData = derData;
            this.pkcs8 = pkcs8;
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
        public boolean isPKCS8() {
            return pkcs8;
        }

        @Override
        public KeySpec getKeySpec() {
            if (!pkcs8) {
                throw new IllegalStateException("Key must be PKCS8 encoded");
            }
            return new PKCS8EncodedKeySpec(derData);
        }
    }

    /**
     * {@link DataFormat#PEM} implementation of {@link PrivateKeyData}
     */
    class PEM implements PrivateKeyData {

        private final String pemData;
        private final boolean pkcs8;

        public PEM(String pemData, boolean pkcs8) {
            this.pemData = pemData;
            this.pkcs8 = pkcs8;
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
        public boolean isPKCS8() {
            return pkcs8;
        }

        @Override
        public KeySpec getKeySpec() {
            if (!pkcs8) {
                throw new IllegalStateException("Key must be PKCS8 encoded");
            }

            String base64Data = pemData
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "");

            byte[] derData = Base64.getMimeDecoder().decode(base64Data);

            return new PKCS8EncodedKeySpec(derData);
        }
    }
}
