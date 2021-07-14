package io.quarkus.vault.runtime.client.dto.pki;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultPKIEnableBody {

    public static class Config {

        @JsonProperty("default_lease_ttl")
        public String defaultLeaseTimeToLive;

        @JsonProperty("max_lease_ttl")
        public String maxLeaseTimeToLive;

    }

    public String type = "pki";

    public String description = "";

    public Config config;

}
