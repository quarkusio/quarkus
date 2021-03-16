package io.quarkus.vault.runtime.client.dto.auth;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultKubernetesAuthRoleData implements VaultModel {

    @JsonProperty("bound_service_account_names")
    public List<String> boundServiceAccountNames;
    @JsonProperty("bound_service_account_namespaces")
    public List<String> boundServiceAccountNamespaces;
    public String audience;
    @JsonProperty("token_ttl")
    public Integer tokenTtl;
    @JsonProperty("token_policies")
    public List<String> tokenPolicies;
    @JsonProperty("token_bound_cidrs")
    public List<String> tokenBoundCidrs;
    @JsonProperty("token_max_ttl")
    public Integer tokenMaxTtl;
    @JsonProperty("token_explicit_max_ttl")
    public Integer tokenExplicitMaxTtl;
    @JsonProperty("token_no_default_policy")
    public Boolean tokenNoDefaultPolicy;
    @JsonProperty("token_num_uses")
    public Integer tokenNumUses;
    @JsonProperty("token_period")
    public Integer tokenPeriod;
    @JsonProperty("token_type")
    public String tokenType;

    public List<String> getBoundServiceAccountNames() {
        return boundServiceAccountNames;
    }

    public VaultKubernetesAuthRoleData setBoundServiceAccountNames(List<String> boundServiceAccountNames) {
        this.boundServiceAccountNames = boundServiceAccountNames;
        return this;
    }

    public List<String> getBoundServiceAccountNamespaces() {
        return boundServiceAccountNamespaces;
    }

    public VaultKubernetesAuthRoleData setBoundServiceAccountNamespaces(List<String> boundServiceAccountNamespaces) {
        this.boundServiceAccountNamespaces = boundServiceAccountNamespaces;
        return this;
    }

    public String getAudience() {
        return audience;
    }

    public VaultKubernetesAuthRoleData setAudience(String audience) {
        this.audience = audience;
        return this;
    }

    public Integer getTokenTtl() {
        return tokenTtl;
    }

    public VaultKubernetesAuthRoleData setTokenTtl(Integer tokenTtl) {
        this.tokenTtl = tokenTtl;
        return this;
    }

    public List<String> getTokenPolicies() {
        return tokenPolicies;
    }

    public VaultKubernetesAuthRoleData setTokenPolicies(List<String> tokenPolicies) {
        this.tokenPolicies = tokenPolicies;
        return this;
    }

    public List<String> getTokenBoundCidrs() {
        return tokenBoundCidrs;
    }

    public VaultKubernetesAuthRoleData setTokenBoundCidrs(List<String> tokenBoundCidrs) {
        this.tokenBoundCidrs = tokenBoundCidrs;
        return this;
    }

    public Integer getTokenMaxTtl() {
        return tokenMaxTtl;
    }

    public VaultKubernetesAuthRoleData setTokenMaxTtl(Integer tokenMaxTtl) {
        this.tokenMaxTtl = tokenMaxTtl;
        return this;
    }

    public Integer getTokenExplicitMaxTtl() {
        return tokenExplicitMaxTtl;
    }

    public VaultKubernetesAuthRoleData setTokenExplicitMaxTtl(Integer tokenExplicitMaxTtl) {
        this.tokenExplicitMaxTtl = tokenExplicitMaxTtl;
        return this;
    }

    public Boolean getTokenNoDefaultPolicy() {
        return tokenNoDefaultPolicy;
    }

    public VaultKubernetesAuthRoleData setTokenNoDefaultPolicy(Boolean tokenNoDefaultPolicy) {
        this.tokenNoDefaultPolicy = tokenNoDefaultPolicy;
        return this;
    }

    public Integer getTokenNumUses() {
        return tokenNumUses;
    }

    public VaultKubernetesAuthRoleData setTokenNumUses(Integer tokenNumUses) {
        this.tokenNumUses = tokenNumUses;
        return this;
    }

    public Integer getTokenPeriod() {
        return tokenPeriod;
    }

    public VaultKubernetesAuthRoleData setTokenPeriod(Integer tokenPeriod) {
        this.tokenPeriod = tokenPeriod;
        return this;
    }

    public String getTokenType() {
        return tokenType;
    }

    public VaultKubernetesAuthRoleData setTokenType(String tokenType) {
        this.tokenType = tokenType;
        return this;
    }
}
