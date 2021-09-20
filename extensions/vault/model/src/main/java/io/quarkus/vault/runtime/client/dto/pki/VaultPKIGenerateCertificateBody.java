package io.quarkus.vault.runtime.client.dto.pki;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPKIGenerateCertificateBody implements VaultModel {

    @JsonProperty("common_name")
    public String subjectCommonName;

    @JsonProperty("alt_names")
    public String subjectAlternativeNames;

    @JsonProperty("ip_sans")
    public String ipSubjectAlternativeNames;

    @JsonProperty("uri_sans")
    public String uriSubjectAlternativeNames;

    @JsonProperty("other_sans")
    public List<String> otherSubjectAlternativeNames;

    @JsonProperty("ttl")
    public String timeToLive;

    public String format;

    @JsonProperty("private_key_format")
    public String privateKeyFormat;

    @JsonProperty("exclude_cn_from_sans")
    public Boolean excludeCommonNameFromSubjectAlternativeNames;

}
