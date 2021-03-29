package io.quarkus.vault.runtime.client.dto.transit;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.Base64String;
import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitEncryptBatchInput implements VaultModel {

    public Base64String plaintext;
    public Base64String context;
    public Base64String nonce;
    @JsonProperty("key_version")
    public Integer keyVersion;

    public VaultTransitEncryptBatchInput(Base64String plaintext, Base64String context) {
        this.plaintext = plaintext;
        this.context = context;
    }

}
