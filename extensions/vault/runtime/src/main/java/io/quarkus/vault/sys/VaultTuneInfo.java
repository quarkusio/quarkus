package io.quarkus.vault.sys;

public class VaultTuneInfo {

    private String description;
    private Long defaultLeaseTimeToLive;
    private Long maxLeaseTimeToLive;
    private Boolean forceNoCache;

    public String getDescription() {
        return description;
    }

    public VaultTuneInfo setDescription(String description) {
        this.description = description;
        return this;
    }

    public Long getDefaultLeaseTimeToLive() {
        return defaultLeaseTimeToLive;
    }

    public VaultTuneInfo setDefaultLeaseTimeToLive(Long defaultLeaseTimeToLive) {
        this.defaultLeaseTimeToLive = defaultLeaseTimeToLive;
        return this;
    }

    public Long getMaxLeaseTimeToLive() {
        return maxLeaseTimeToLive;
    }

    public VaultTuneInfo setMaxLeaseTimeToLive(Long maxLeaseTimeToLive) {
        this.maxLeaseTimeToLive = maxLeaseTimeToLive;
        return this;
    }

    public Boolean getForceNoCache() {
        return forceNoCache;
    }

    public VaultTuneInfo setForceNoCache(Boolean forceNoCache) {
        this.forceNoCache = forceNoCache;
        return this;
    }
}
