package io.quarkus.vault.runtime.client.dto.transit;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitSignDataBatchResult implements VaultModel {

    public String signature;
    public String publickey;
    public String error;

}
