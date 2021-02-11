package io.quarkus.vault.transit;

import static io.quarkus.vault.runtime.StringHelper.bytesToString;
import static io.quarkus.vault.runtime.StringHelper.stringToBytes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Arbitrary unencrypted data that can be created from raw bytes, or a {@link String}, which will be
 * encoded using {@link StandardCharsets#UTF_8}
 */
public class ClearData {

    private byte[] value;

    /**
     * Create a {@link ClearData} from raw bytes
     * 
     * @param value the unencrypted data to wrap
     */
    public ClearData(byte[] value) {
        this.value = value;
    }

    /**
     * Create a {@link ClearData} from a {@link String}, which will be encoded using {@link StandardCharsets#UTF_8}
     * 
     * @param value the unencrypted data to wrap
     */
    public ClearData(String value) {
        this(stringToBytes(value));
    }

    /**
     * @return a {@link String} representation of the unencrypted data created from raw bytes decoded using
     *         {@link StandardCharsets#UTF_8}
     */
    public String asString() {
        return bytesToString(value);
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ClearData data = (ClearData) o;
        return Arrays.equals(value, data.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
