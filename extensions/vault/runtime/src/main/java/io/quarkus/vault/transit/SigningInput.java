package io.quarkus.vault.transit;

import static io.quarkus.vault.runtime.StringHelper.bytesToString;
import static io.quarkus.vault.runtime.StringHelper.stringToBytes;

import java.util.Arrays;

import io.quarkus.vault.VaultTransitSecretEngine;

/**
 * Data wrapper used in the sign methods of the {@link VaultTransitSecretEngine}
 */
public class SigningInput {

    private byte[] value;

    /**
     * Create a {@link SigningInput} from a byte array
     * 
     * @param value the wrapped data
     */
    public SigningInput(byte[] value) {
        this.value = value;
    }

    /**
     * Create a {@link SigningInput} from a {@link String} that will be
     * encoded using {@link java.nio.charset.StandardCharsets#UTF_8}
     * 
     * @param value the wrapped data
     */
    public SigningInput(String value) {
        this(stringToBytes(value));
    }

    /**
     * Decode the internal byte array using {@link java.nio.charset.StandardCharsets#UTF_8}
     * 
     * @return the value as a {@link String}
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
        SigningInput data = (SigningInput) o;
        return Arrays.equals(value, data.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
