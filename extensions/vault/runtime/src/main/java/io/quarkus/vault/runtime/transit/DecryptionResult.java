package io.quarkus.vault.runtime.transit;

import io.quarkus.vault.transit.ClearData;

public class DecryptionResult extends VaultTransitBatchResult<ClearData> {

    public DecryptionResult(ClearData data, String error) {
        super(data, error);
    }

}
