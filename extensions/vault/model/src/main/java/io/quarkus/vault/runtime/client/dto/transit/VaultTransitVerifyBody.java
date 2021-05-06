package io.quarkus.vault.runtime.client.dto.transit;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitVerifyBody implements VaultModel {

    @JsonProperty("batch_input")
    public List<VaultTransitVerifyBatchInput> batchInput;
    public Boolean prehashed;
    @JsonProperty("signature_algorithm")
    public String signatureAlgorithm;
    @JsonProperty("marshaling_algorithm")
    public String marshalingAlgorithm;

}
