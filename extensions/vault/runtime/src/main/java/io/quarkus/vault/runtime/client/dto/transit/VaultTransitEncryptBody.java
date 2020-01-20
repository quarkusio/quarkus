package io.quarkus.vault.runtime.client.dto.transit;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitEncryptBody implements VaultModel {

    public String type;
    @JsonProperty("convergent_encryption")
    public String convergentEncryption;

    @JsonProperty("batch_input")
    public List<VaultTransitEncryptBatchInput> batchInput;

}
