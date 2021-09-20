package io.quarkus.vault.runtime.client.dto.pki;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPKIRevokeCertificateBody implements VaultModel {

    @JsonProperty("serial_number")
    public String serialNumber;

}
