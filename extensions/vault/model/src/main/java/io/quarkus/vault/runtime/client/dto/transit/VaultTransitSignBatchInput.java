package io.quarkus.vault.runtime.client.dto.transit;

import io.quarkus.vault.runtime.Base64String;
import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitSignBatchInput implements VaultModel {

    public Base64String context;
    public Base64String input;

    public VaultTransitSignBatchInput(Base64String input, Base64String context) {
        this.input = input;
        this.context = context;
    }

}
