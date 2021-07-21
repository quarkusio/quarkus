package io.quarkus.vault.runtime.client.dto.pki;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPKIConfigCABody implements VaultModel {

    @JsonProperty("pem_bundle")
    public String pemBundle;

}
