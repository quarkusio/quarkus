package io.quarkus.vault.runtime.client.dto.transit;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitVerifyDataBatchResult implements VaultModel {

    public boolean valid;
    public String error;

}
