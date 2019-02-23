package io.quarkus.infinispan.client.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * @author William Burns
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME_STATIC)
public class InfinispanClientConfiguration {

    /**
     * Sets the host name/port to connect to. Each one is separated by a semicolon (eg. host1:11222;host2:11222).
     */
    @ConfigItem
    public Optional<String> serverList;

    /**
     * Sets the bounded entry count for near cache. If this value is 0 or less near cache is disabled.
     */
    @ConfigItem(defaultValue = "0")
    public int nearCacheMaxEntries;

    @Override
    public String toString() {
        return "InfinispanClientConfiguration{" +
                "serverList=" + serverList +
                ", nearCacheMaxEntries=" + nearCacheMaxEntries +
                '}';
    }
}
