package io.quarkus.vault.transit;

import static io.quarkus.vault.runtime.StringHelper.stringToBytes;

import java.util.Arrays;

/**
 * A transit context used for key derivation, when the key supports it.
 * 
 * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#derived">derived attribute in key creation</a>
 */
public class TransitContext {

    private byte[] context;

    public static TransitContext fromContext(byte[] context) {
        return new TransitContext(context);
    }

    public static TransitContext fromContext(String context) {
        return new TransitContext(stringToBytes(context));
    }

    public TransitContext(byte[] context) {
        this.context = context;
    }

    public byte[] getContext() {
        return context;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TransitContext that = (TransitContext) o;
        return Arrays.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(context);
    }
}
