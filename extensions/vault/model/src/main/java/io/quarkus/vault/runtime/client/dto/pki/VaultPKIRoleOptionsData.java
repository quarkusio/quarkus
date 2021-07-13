package io.quarkus.vault.runtime.client.dto.pki;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultPKIRoleOptionsData {

    @JsonProperty("ttl")
    public String timeToLive;

    @JsonProperty("max_ttl")
    public String maxTimeToLive;

    @JsonProperty("allow_localhost")
    public Boolean allowLocalhost;

    @JsonProperty("allowed_domains")
    public List<String> allowedDomains;

    @JsonProperty("allowed_domains_template")
    public Boolean allowTemplatesInAllowedDomains;

    @JsonProperty("allow_bare_domains")
    public Boolean allowBareDomains;

    @JsonProperty("allow_subdomains")
    public Boolean allowSubdomains;

    @JsonProperty("allow_glob_domains")
    public Boolean allowGlobsInAllowedDomains;

    @JsonProperty("allow_any_name")
    public Boolean allowAnyName;

    @JsonProperty("enforce_hostnames")
    public Boolean enforceHostnames;

    @JsonProperty("allow_ip_sans")
    public Boolean allowIpSubjectAlternativeNames;

    @JsonProperty("allowed_uri_sans")
    public List<String> allowedUriSubjectAlternativeNames;

    @JsonProperty("allowed_other_sans")
    public List<String> allowedOtherSubjectAlternativeNames;

    @JsonProperty("server_flag")
    public Boolean serverFlag;

    @JsonProperty("client_flag")
    public Boolean clientFlag;

    @JsonProperty("code_signing_flag")
    public Boolean codeSigningFlag;

    @JsonProperty("email_protection_flag")
    public Boolean emailProtectionFlag;

    @JsonProperty("key_type")
    public String keyType;

    @JsonProperty("key_bits")
    public Integer keyBits;

    @JsonProperty("key_usage")
    public List<String> keyUsages;

    @JsonProperty("ext_key_usage")
    public List<String> extendedKeyUsages;

    @JsonProperty("ext_key_usage_oids")
    public List<String> extendedKeyUsageOIDs;

    @JsonProperty("use_csr_common_name")
    public Boolean useCSRCommonName;

    @JsonProperty("use_csr_sans")
    public Boolean useCSRSubjectAlternativeNames;

    @JsonProperty("organization")
    public List<String> subjectOrganization;

    @JsonProperty("ou")
    public List<String> subjectOrganizationalUnit;

    @JsonProperty("street_address")
    public List<String> subjectStreetAddress;

    @JsonProperty("postal_code")
    public List<String> subjectPostalCode;

    @JsonProperty("locality")
    public List<String> subjectLocality;

    @JsonProperty("province")
    public List<String> subjectProvince;

    @JsonProperty("country")
    public List<String> subjectCountry;

    @JsonProperty("allowed_serial_numbers")
    public List<String> allowedSubjectSerialNumbers;

    @JsonProperty("generate_lease")
    public Boolean generateLease;

    @JsonProperty("no_store")
    public Boolean noStore;

    @JsonProperty("require_cn")
    public Boolean requireCommonName;

    @JsonProperty("policy_identifiers")
    public List<String> policyOIDs;

    @JsonProperty("basic_constraints_valid_for_non_ca")
    public Boolean basicConstraintsValidForNonCA;

    @JsonProperty("not_before_duration")
    public String notBeforeDuration;

}
