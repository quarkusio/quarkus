package io.quarkus.vault.runtime.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class VaultTransitConfig {

    /**
     * keys
     */
    @ConfigItem
    public Map<String, TransitKeyConfig> key;

}
