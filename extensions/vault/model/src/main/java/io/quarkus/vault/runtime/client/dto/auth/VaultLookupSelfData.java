package io.quarkus.vault.runtime.client.dto.auth;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.AbstractVaultLookupData;

public class VaultLookupSelfData extends AbstractVaultLookupData {

    public String accessor;
    @JsonProperty("creation_time")
    public long creationTime;
    @JsonProperty("creation_ttl")
    public long creationTtl;
    @JsonProperty("display_name")
    public String displayName;
    @JsonProperty("entity_id")
    public String entityId;
    @JsonProperty("explicit_max_ttl")
    public long explicitMaxTtl;
    @JsonProperty("last_renewal_time")
    public long lastRenewalTime;
    public Map<String, String> meta;
    @JsonProperty("num_uses")
    public int numUses;
    public boolean orphan;
    public String path;
    public List<String> policies;
    public String type;

}
