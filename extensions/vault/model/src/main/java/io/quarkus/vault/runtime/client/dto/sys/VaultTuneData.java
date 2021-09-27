package io.quarkus.vault.runtime.client.dto.sys;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultTuneData {

    public String description;

    @JsonProperty("default_lease_ttl")
    public Long defaultLeaseTimeToLive;

    @JsonProperty("max_lease_ttl")
    public Long maxLeaseTimeToLive;

    @JsonProperty("force_no_cache")
    public Boolean forceNoCache;

}
