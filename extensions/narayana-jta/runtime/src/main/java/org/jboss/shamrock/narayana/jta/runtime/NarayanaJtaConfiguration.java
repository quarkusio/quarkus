package org.jboss.shamrock.narayana.jta.runtime;

import java.util.Optional;

import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public final class NarayanaJtaConfiguration {
    /**
     * The node name used by the transaction manager
     */
    @ConfigItem(defaultValue = "shamrock")
    public String nodeName;

    /**
     * The XA node name used by the transaction manager
     */
    @ConfigItem()
    public Optional<String> xaNodeName;
}
