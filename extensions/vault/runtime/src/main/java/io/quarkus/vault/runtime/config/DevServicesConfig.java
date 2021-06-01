package io.quarkus.vault.runtime.config;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DevServicesConfig {

    /**
     * If DevServices has been explicitly enabled or disabled. DevServices is generally enabled
     * by default, unless there is an existing configuration present.
     * <p>
     * When DevServices is enabled Quarkus will attempt to automatically configure and start
     * a vault instance when running in Dev or Test mode and when Docker is running.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The container image name to use, for container based DevServices providers.
     */
    @ConfigItem
    public Optional<String> imageName;

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    @ConfigItem
    public OptionalInt port;

    /**
     * Should the Transit secret engine be enabled
     */
    @ConfigItem(defaultValue = "false")
    public boolean transitEnabled;

    /**
     * Should the PKI secret engine be enabled
     */
    @ConfigItem(defaultValue = "false")
    public boolean pkiEnabled;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DevServicesConfig that = (DevServicesConfig) o;
        return enabled == that.enabled && Objects.equals(imageName,
                that.imageName) && Objects.equals(port,
                        that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, imageName, port);
    }

    @Override
    public String toString() {
        return "DevServicesConfig{" +
                "enabled=" + enabled +
                ", imageName=" + imageName +
                ", port=" + port +
                '}';
    }
}
