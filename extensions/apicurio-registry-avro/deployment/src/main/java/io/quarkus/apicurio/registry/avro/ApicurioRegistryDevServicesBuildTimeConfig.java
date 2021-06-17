package io.quarkus.apicurio.registry.avro;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "apicurio-registry.devservices", phase = ConfigPhase.BUILD_TIME)
public class ApicurioRegistryDevServicesBuildTimeConfig {

    /**
     * If Dev Services for Apicurio Registry has been explicitly enabled or disabled. Dev Services are generally enabled
     * by default, unless there is an existing configuration present. For Apicurio Registry, Dev Services starts a registry
     * unless {@code mp.messaging.connector.smallrye-kafka.apicurio.registry.url} is set.
     */
    @ConfigItem
    public Optional<Boolean> enabled = Optional.empty();

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    @ConfigItem
    public Optional<Integer> port;

    /**
     * The Apicurio Registry image to use.
     * Note that only Apicurio Registry 2.x images are supported.
     */
    @ConfigItem(defaultValue = "apicurio/apicurio-registry-mem:2.0.1.Final")
    public String imageName;

}
