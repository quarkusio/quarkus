package io.quarkus.narayana.jta.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public final class NarayanaJtaConfiguration {
    /**
     * The node name used by the transaction manager
     */
    @ConfigItem(defaultValue = "quarkus")
    public String nodeName;

    /**
     * The XA node name used by the transaction manager
     */
    @ConfigItem()
    public Optional<String> xaNodeName;
}
