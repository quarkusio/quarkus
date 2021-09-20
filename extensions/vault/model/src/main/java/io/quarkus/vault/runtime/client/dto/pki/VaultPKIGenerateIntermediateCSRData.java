package io.quarkus.vault.runtime.client.dto.pki;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPKIGenerateIntermediateCSRData implements VaultModel {

    public String csr;

    @JsonProperty("private_key")
    public String privateKey;

    @JsonProperty("private_key_type")
    public String privateKeyType;

}
