package io.quarkus.observability.common.config;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractContainerConfig implements ContainerConfig {

    private final String imageName;
    private final boolean shared;

    public AbstractContainerConfig(String imageName) {
        this(imageName, true);
    }

    public AbstractContainerConfig(String imageName, boolean shared) {
        this.imageName = imageName;
        this.shared = shared;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public String imageName() {
        return imageName;
    }

    @Override
    public boolean shared() {
        return shared;
    }

    @Override
    public Optional<Set<String>> networkAliases() {
        return Optional.empty();
    }

    @Override
    public String label() {
        String sn = getClass().getSimpleName().toLowerCase(Locale.ROOT);
        return "quarkus-dev-resource-" + sn;
    }

    @Override
    public String serviceName() {
        return "quarkus";
    }
}
