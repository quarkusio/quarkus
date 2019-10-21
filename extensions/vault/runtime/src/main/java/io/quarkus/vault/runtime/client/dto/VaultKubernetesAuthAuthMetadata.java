package io.quarkus.vault.runtime.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultKubernetesAuthAuthMetadata implements VaultModel {

    public String role;
    @JsonProperty("service_account_name")
    public String serviceAccountName;
    @JsonProperty("service_account_namespace")
    public String serviceAccountNamespace;
    @JsonProperty("service_account_secret_name")
    public String serviceAccountSecretName;
    @JsonProperty("service_account_uid")
    public String serviceAccountUid;

}
