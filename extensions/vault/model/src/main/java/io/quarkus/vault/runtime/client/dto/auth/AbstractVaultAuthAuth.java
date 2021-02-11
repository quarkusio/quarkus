package io.quarkus.vault.runtime.client.dto.auth;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class AbstractVaultAuthAuth<METADATA> implements VaultModel {

    @JsonProperty("client_token")
    public String clientToken;
    public String accessor;
    public List<String> policies;
    @JsonProperty("token_policies")
    public List<String> tokenPolicies;
    public METADATA metadata;
    @JsonProperty("lease_duration")
    public long leaseDurationSecs;
    public boolean renewable;
    @JsonProperty("entity_id")
    public String entityId;
    @JsonProperty("token_type")
    public String tokenType;
    public boolean orphan;

}
