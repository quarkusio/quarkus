package io.quarkus.vault.runtime.client.dto.sys;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultEnableEngineBody {

    public static class Config {

        @JsonProperty("default_lease_ttl")
        public String defaultLeaseTimeToLive;

        @JsonProperty("max_lease_ttl")
        public String maxLeaseTimeToLive;

    }

    public String type;

    public String description = "";

    public Config config;

    public Map<String, String> options;

}
