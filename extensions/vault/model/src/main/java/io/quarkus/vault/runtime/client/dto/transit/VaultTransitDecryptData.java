package io.quarkus.vault.runtime.client.dto.transit;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitDecryptData implements VaultModel {

    @JsonProperty("batch_results")
    public List<VaultTransitDecryptDataBatchResult> batchResults;

}
