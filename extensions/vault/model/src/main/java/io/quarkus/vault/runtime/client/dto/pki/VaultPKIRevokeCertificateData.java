package io.quarkus.vault.runtime.client.dto.pki;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPKIRevokeCertificateData implements VaultModel {

    @JsonProperty("revocation_time")
    public OffsetDateTime revocationTime;

}
