package io.quarkus.resteasy.common.deployment;

import java.util.Objects;
import java.util.Optional;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used to mark a class as a potential REST client interface consumed by the MicroProfile REST client.
 * <p>
 * Useful when you want to apply different behaviors to REST resources and REST clients.
 */
public final class RestClientBuildItem extends MultiBuildItem {

    private String interfaceName;
    private final ClassInfo classInfo;
    private final Optional<String> configKey;
    private final Optional<String> defaultBaseUri;

    public RestClientBuildItem(ClassInfo classInfo, Optional<String> configKey, Optional<String> defaultBaseUri) {
        this.classInfo = Objects.requireNonNull(classInfo);
        this.configKey = Objects.requireNonNull(configKey);
        this.defaultBaseUri = Objects.requireNonNull(defaultBaseUri);
    }

    public String getInterfaceName() {
        return classInfo.name().toString();
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
