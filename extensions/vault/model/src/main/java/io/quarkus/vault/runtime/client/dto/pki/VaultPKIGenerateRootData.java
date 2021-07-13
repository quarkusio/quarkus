package io.quarkus.vault.runtime.client.dto.pki;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPKIGenerateRootData implements VaultModel {

    public String certificate;

    @JsonProperty("issuing_ca")
    public String issuingCA;

    @JsonProperty("serial_number")
    public String serialNumber;

    @JsonProperty("private_key")
    public String privateKey;

    @JsonProperty("private_key_type")
    public String privateKeyType;

}
