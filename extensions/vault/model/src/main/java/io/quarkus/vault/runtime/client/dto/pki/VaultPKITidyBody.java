package io.quarkus.vault.runtime.client.dto.pki;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPKITidyBody implements VaultModel {

    @JsonProperty("tidy_cert_store")
    public Boolean tidyCertStore;

    @JsonProperty("tidy_revoked_certs")
    public Boolean tidyRevokedCerts;

    @JsonProperty("safety_buffer")
    public String safetyBuffer;

}
