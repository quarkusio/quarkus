package io.quarkus.vault.runtime.client.dto.transit;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitCreateKeyBody implements VaultModel {

    @JsonProperty("convergent_encryption")
    public String convergentEncryption;
    public Boolean derived;
    public Boolean exportable;
    @JsonProperty("allow_plaintext_backup")
    public Boolean allowPlaintextBackup;
    public String type;
}
