package io.quarkus.vault.auth;

import java.util.List;

public class VaultKubernetesAuthRole {

    public List<String> boundServiceAccountNames;
    public List<String> boundServiceAccountNamespaces;
    public String audience;
    public Integer tokenTtl;
    public List<String> tokenPolicies;
    public List<String> tokenBoundCidrs;
    public Integer tokenMaxTtl;
    public Integer tokenExplicitMaxTtl;
    public Boolean tokenNoDefaultPolicy;
    public Integer tokenNumUses;
    public Integer tokenPeriod;
    public String tokenType;

    public List<String> getBoundServiceAccountNames() {
        return boundServiceAccountNames;
    }

    public List<String> getBoundServiceAccountNamespaces() {
        return boundServiceAccountNamespaces;
    }

    public String getAudience() {
        return audience;
    }

    public Integer getTokenTtl() {
        return tokenTtl;
    }

    public List<String> getTokenPolicies() {
        return tokenPolicies;
    }

    public List<String> getTokenBoundCidrs() {
        return tokenBoundCidrs;
    }

    public Integer getTokenMaxTtl() {
        return tokenMaxTtl;
    }

    public Integer getTokenExplicitMaxTtl() {
        return tokenExplicitMaxTtl;
    }

    public Boolean getTokenNoDefaultPolicy() {
        return tokenNoDefaultPolicy;
    }

    public Integer getTokenNumUses() {
        return tokenNumUses;
    }

    public Integer getTokenPeriod() {
        return tokenPeriod;
    }

    public String getTokenType() {
        return tokenType;
    }

    public VaultKubernetesAuthRole setBoundServiceAccountNames(List<String> boundServiceAccountNames) {
        this.boundServiceAccountNames = boundServiceAccountNames;
        return this;
    }

    public VaultKubernetesAuthRole setBoundServiceAccountNamespaces(List<String> boundServiceAccountNamespaces) {
        this.boundServiceAccountNamespaces = boundServiceAccountNamespaces;
        return this;
    }

    public VaultKubernetesAuthRole setAudience(String audience) {
        this.audience = audience;
        return this;
    }

    public VaultKubernetesAuthRole setTokenTtl(Integer tokenTtl) {
        this.tokenTtl = tokenTtl;
        return this;
    }

    public VaultKubernetesAuthRole setTokenPolicies(List<String> tokenPolicies) {
        this.tokenPolicies = tokenPolicies;
        return this;
    }

    public VaultKubernetesAuthRole setTokenBoundCidrs(List<String> tokenBoundCidrs) {
        this.tokenBoundCidrs = tokenBoundCidrs;
        return this;
    }

    public VaultKubernetesAuthRole setTokenMaxTtl(Integer tokenMaxTtl) {
        this.tokenMaxTtl = tokenMaxTtl;
        return this;
    }

    public VaultKubernetesAuthRole setTokenExplicitMaxTtl(Integer tokenExplicitMaxTtl) {
        this.tokenExplicitMaxTtl = tokenExplicitMaxTtl;
        return this;
    }

    public VaultKubernetesAuthRole setTokenNoDefaultPolicy(Boolean tokenNoDefaultPolicy) {
        this.tokenNoDefaultPolicy = tokenNoDefaultPolicy;
        return this;
    }

    public VaultKubernetesAuthRole setTokenNumUses(Integer tokenNumUses) {
        this.tokenNumUses = tokenNumUses;
        return this;
    }

    public VaultKubernetesAuthRole setTokenPeriod(Integer tokenPeriod) {
        this.tokenPeriod = tokenPeriod;
        return this;
    }

    public VaultKubernetesAuthRole setTokenType(String tokenType) {
        this.tokenType = tokenType;
        return this;
    }
}
