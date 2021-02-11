package io.quarkus.vault.runtime.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AbstractVaultLookupData implements VaultModel {

    @JsonProperty("expire_time")
    public String expireTime;
    public String id;
    @JsonProperty("issue_time")
    public String issueTime;
    @JsonProperty("last_renewal")
    public String lastRenewal;
    public boolean renewable;
    public long ttl;

}
