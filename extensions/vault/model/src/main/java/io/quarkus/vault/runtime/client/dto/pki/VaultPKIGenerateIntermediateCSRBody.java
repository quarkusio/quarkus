package io.quarkus.vault.runtime.client.dto.pki;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPKIGenerateIntermediateCSRBody implements VaultModel {

    @JsonProperty("common_name")
    public String subjectCommonName;

    @JsonProperty("organization")
    public String subjectOrganization;

    @JsonProperty("ou")
    public String subjectOrganizationalUnit;

    @JsonProperty("street_address")
    public String subjectStreetAddress;

    @JsonProperty("postal_code")
    public String subjectPostalCode;

    @JsonProperty("locality")
    public String subjectLocality;

    @JsonProperty("province")
    public String subjectProvince;

    @JsonProperty("country")
    public String subjectCountry;

    @JsonProperty("alt_names")
    public String subjectAlternativeNames;

    @JsonProperty("ip_sans")
    public String ipSubjectAlternativeNames;

    @JsonProperty("uri_sans")
    public String uriSubjectAlternativeNames;

    @JsonProperty("other_sans")
    public List<String> otherSubjectAlternativeNames;

    @JsonProperty("serial_number")
    public String subjectSerialNumber;

    public String format;

    @JsonProperty("private_key_format")
    public String privateKeyFormat;

    @JsonProperty("key_type")
    public String keyType;

    @JsonProperty("key_bits")
    public Integer keyBits;

    @JsonProperty("exclude_cn_from_sans")
    public Boolean excludeCommonNameFromSubjectAlternativeNames;

}
