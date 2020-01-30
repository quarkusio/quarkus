package io.quarkus.vault.runtime.client.dto.transit;

import io.quarkus.vault.runtime.Base64String;
import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitDecryptBatchInput implements VaultModel {

    public String ciphertext;
    public Base64String context;

    public VaultTransitDecryptBatchInput(String ciphertext, Base64String context) {
        this.ciphertext = ciphertext;
        this.context = context;
    }

}
