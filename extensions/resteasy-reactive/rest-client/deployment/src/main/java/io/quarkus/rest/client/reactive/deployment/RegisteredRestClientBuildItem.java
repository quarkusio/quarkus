package io.quarkus.rest.client.reactive.deployment;

import java.util.Objects;
import java.util.Optional;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Contains information about the REST Clients that have been discovered via
 * {@link org.eclipse.microprofile.rest.client.inject.RegisterRestClient}
 */
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
}
