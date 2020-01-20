package io.quarkus.vault.runtime.client.dto.transit;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitEncryptDataBatchResult implements VaultModel {

    public String ciphertext;
    public String error;

}
