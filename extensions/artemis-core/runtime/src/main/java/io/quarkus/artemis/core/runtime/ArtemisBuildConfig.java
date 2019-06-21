package io.quarkus.artemis.core.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "artemis", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class ArtemisBuildConfig {

    public enum Protocol {
        CORE,
        JMS
    }

    /**
     * Artemis protocol, defaults to JMS if it is on classpath else to CORE
     */
    @ConfigItem
    public Optional<Protocol> protocol;

    public Protocol getProtocol() {
        return protocol.orElseGet(ArtemisBuildConfig::getDefaultProtocol);
    }

    public static Protocol getDefaultProtocol() {
        try {
            Class.forName("io.quarkus.artemis.jms.runtime.ArtemisJmsTemplate");
            return Protocol.JMS;
        } catch (ClassNotFoundException e) {
            return Protocol.CORE;
        }
    }
}
