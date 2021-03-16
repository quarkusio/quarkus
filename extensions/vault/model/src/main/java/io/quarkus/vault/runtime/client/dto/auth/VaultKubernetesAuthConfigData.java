package io.quarkus.vault.runtime.client.dto.auth;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultKubernetesAuthConfigData implements VaultModel {

    @JsonProperty("kubernetes_host")
    public String kubernetesHost;
    @JsonProperty("kubernetes_ca_cert")
    public String kubernetesCaCert;
    @JsonProperty("token_reviewer_jwt")
    public String tokenReviewerJwt;
    @JsonProperty("pem_keys")
    public List<String> pemKeys;
    public String issuer;

    public String getKubernetesHost() {
        return kubernetesHost;
    }

    public VaultKubernetesAuthConfigData setKubernetesHost(String kubernetesHost) {
        this.kubernetesHost = kubernetesHost;
        return this;
    }

    public String getKubernetesCaCert() {
        return kubernetesCaCert;
    }

    public VaultKubernetesAuthConfigData setKubernetesCaCert(String kubernetesCaCert) {
        this.kubernetesCaCert = kubernetesCaCert;
        return this;
    }

    public String getTokenReviewerJwt() {
        return tokenReviewerJwt;
    }

    public VaultKubernetesAuthConfigData setTokenReviewerJwt(String tokenReviewerJwt) {
        this.tokenReviewerJwt = tokenReviewerJwt;
        return this;
    }

    public List<String> getPemKeys() {
        return pemKeys;
    }

    public VaultKubernetesAuthConfigData setPemKeys(List<String> pemKeys) {
        this.pemKeys = pemKeys;
        return this;
    }

    public String getIssuer() {
        return issuer;
    }

    public VaultKubernetesAuthConfigData setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }
}
