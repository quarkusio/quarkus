package io.quarkus.vault.auth;

import java.util.List;

public class VaultKubernetesAuthConfig {

    public String kubernetesHost;
    public String kubernetesCaCert;
    public String tokenReviewerJwt;
    public List<String> pemKeys;
    public String issuer;

    public String getKubernetesHost() {
        return kubernetesHost;
    }

    public VaultKubernetesAuthConfig setKubernetesHost(String kubernetesHost) {
        this.kubernetesHost = kubernetesHost;
        return this;
    }

    public String getKubernetesCaCert() {
        return kubernetesCaCert;
    }

    public VaultKubernetesAuthConfig setKubernetesCaCert(String kubernetesCaCert) {
        this.kubernetesCaCert = kubernetesCaCert;
        return this;
    }

    public String getTokenReviewerJwt() {
        return tokenReviewerJwt;
    }

    public VaultKubernetesAuthConfig setTokenReviewerJwt(String tokenReviewerJwt) {
        this.tokenReviewerJwt = tokenReviewerJwt;
        return this;
    }

    public List<String> getPemKeys() {
        return pemKeys;
    }

    public VaultKubernetesAuthConfig setPemKeys(List<String> pemKeys) {
        this.pemKeys = pemKeys;
        return this;
    }

    public String getIssuer() {
        return issuer;
    }

    public VaultKubernetesAuthConfig setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }
}
