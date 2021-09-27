package io.quarkus.vault.runtime.client.dto.pki;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPKIConfigURLsData implements VaultModel {

    @JsonProperty("issuing_certificates")
    public List<String> issuingCertificates;

    @JsonProperty("crl_distribution_points")
    public List<String> crlDistributionPoints;

    @JsonProperty("ocsp_servers")
    public List<String> ocspServers;

}
