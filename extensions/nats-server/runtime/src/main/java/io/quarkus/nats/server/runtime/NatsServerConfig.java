package io.quarkus.nats.server.runtime;

import berlin.yuna.natsserver.logic.Nats;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "natsserver", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class NatsServerConfig {

    /**
     * Sets nats port
     * -1 means random port
     */
    @ConfigItem(defaultValue = "4222")
    public int port;

    /**
     * Defines the startup and teardown timeout
     */
    @ConfigItem(defaultValue = "10000")
    public long timeoutMs;

    /**
     * Nats server name
     */
    @ConfigItem
    public String name;

    /**
     * Config file
     */
    @ConfigItem
    public String configFile;

    /**
     * Custom download URL
     */
    @ConfigItem
    public String downloadUrl;

    /**
     * File to nats server binary so no download will be needed
     */
    @ConfigItem
    public String binaryFile;

    /**
     * Passes the original parameters to {@link Nats#config()} for startup
     * {@link berlin.yuna.natsserver.config.NatsConfig}
     */
    @ConfigItem
    public String[] config;

    /**
     * Prevents the {@link NatsServerProducer} from recreating for each test class
     */
    @ConfigItem(defaultValue = "false")
    public boolean keepAlive;

    /**
     * Sets the version for the {@link NatsServerProducer}
     */
    @ConfigItem
    public String version;
}
