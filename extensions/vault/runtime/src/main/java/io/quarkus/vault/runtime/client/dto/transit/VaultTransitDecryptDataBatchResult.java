package io.quarkus.vault.runtime.client.dto.transit;

import io.quarkus.vault.runtime.Base64String;
import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitDecryptDataBatchResult implements VaultModel {

    public Base64String plaintext;
    public String error;

}
