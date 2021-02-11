package io.quarkus.vault.runtime.client.dto.transit;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.Base64String;
import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitRewrapBatchInput implements VaultModel {

    public String ciphertext;
    public Base64String context;
    public Base64String nonce;
    @JsonProperty("key_version")
    public Integer keyVersion;

    public VaultTransitRewrapBatchInput(String ciphertext, Base64String context) {
        this.ciphertext = ciphertext;
        this.context = context;
    }

}
