package io.quarkus.vault.runtime.client.dto.pki;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPKIGenerateCertificateData implements VaultModel {

    public String certificate;

    @JsonProperty("issuing_ca")
    public String issuingCA;

    @JsonProperty("ca_chain")
    public List<String> caChain;

    @JsonProperty("private_key")
    public String privateKey;

    @JsonProperty("private_key_type")
    public String privateKeyType;

    @JsonProperty("serial_number")
    public String serialNumber;

}
