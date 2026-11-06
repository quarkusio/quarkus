package io.quarkus.rest.client.reactive.deployment;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.restclient.config.RegisteredRestClient;

/**
 * Contains information about the REST Clients that have been discovered via
 * {@link org.eclipse.microprofile.rest.client.inject.RegisterRestClient}
 *
 * @deprecated use {@link io.quarkus.rest.client.reactive.spi.RegisteredRestClientBuildItem} from the
 *             {@code io.quarkus:quarkus-rest-client-spi-deployment} artifact instead. This will be
 *             removed in a future version.
 */
@Deprecated(forRemoval = true, since = "4.0")
public final class RegisteredRestClientBuildItem extends MultiBuildItem {

    private final ClassInfo classInfo;
    private final Optional<String> configKey;
    private final Optional<String> defaultBaseUri;

    public RegisteredRestClientBuildItem(ClassInfo classInfo, Optional<String> configKey, Optional<String> defaultBaseUri) {
        this.classInfo = Objects.requireNonNull(classInfo);
        this.configKey = Objects.requireNonNull(configKey);
        this.defaultBaseUri = Objects.requireNonNull(defaultBaseUri);
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public Optional<String> getConfigKey() {
        return configKey;
    }

    public Optional<String> getDefaultBaseUri() {
        return defaultBaseUri;
    }

    /**
     * @deprecated there is no replacement, {@link RegisteredRestClient} is an internal type
     */
    @Deprecated(forRemoval = true, since = "4.0")
    public static List<RegisteredRestClient> toRegisteredRestClients(List<RegisteredRestClientBuildItem> restClients) {
        return restClients.stream()
                .map(rc -> new RegisteredRestClient(
                        rc.getClassInfo().name().toString(),
                        rc.getClassInfo().simpleName(),
                        rc.getConfigKey().orElse(null)))
                .toList();
    }
}
