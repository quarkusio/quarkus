package io.quarkus.vault.runtime.transit;

import io.quarkus.vault.VaultException;

public class VaultTransitBatchResult<K> {

    private String error;
    private K value;

    // ---

    public VaultTransitBatchResult(K value, String error) {
        this.value = value;
        this.error = error;
    }

    public K getValue() {
        return value;
    }

    public String getError() {
        return error;
    }

    public K getValueOrElseError() {
        if (error != null) {
            throw new VaultException(error);
        }
        return value;
    }

    public boolean isInError() {
        return error != null;
    }

    public boolean isValid() {
        return error == null;
    }
}
