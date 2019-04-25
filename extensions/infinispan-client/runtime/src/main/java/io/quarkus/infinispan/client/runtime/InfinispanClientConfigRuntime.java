package io.quarkus.infinispan.client.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * @author Katia Aresti
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class InfinispanClientConfigRuntime {

    /**
     * Sets the host name/port to connect to. Each one is separated by a semicolon (eg. host1:11222;host2:11222).
     */
    @ConfigItem
    public Optional<String> serverList;

    @Override
    public String toString() {
        return "InfinispanClientConfigRuntime{" +
                "serverList=" + serverList +
                '}';
    }
}
