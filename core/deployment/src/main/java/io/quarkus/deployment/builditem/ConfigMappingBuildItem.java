package io.quarkus.deployment.builditem;

import static io.quarkus.deployment.builditem.ConfigClassInfo.collectTypes;

import java.util.Objects;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.configuration.DotNames;

/**
 * Discovered application classes annotated with {@link io.smallrye.config.ConfigMapping}.
 * <p>
 * This does not include extension {@link io.smallrye.config.ConfigMapping} classes annotated with
 * {@link io.quarkus.runtime.annotations.ConfigRoot}, which are handled separately by the core configuration
 * processor.
 */
public final class ConfigMappingBuildItem extends MultiBuildItem implements ConfigClassInfo {
    private final ClassInfo configClass;
    private final String prefix;
    private final Set<Type> types;

    public ConfigMappingBuildItem(ClassInfo configClass, String prefix, IndexView index) {
        this.configClass = configClass;
        this.prefix = prefix;
        this.types = collectTypes(configClass, index);
    }

    @Override
    public ClassInfo getConfigClass() {
        return configClass;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    public boolean isStaticInitSafe() {
        return configClass.hasDeclaredAnnotation(DotNames.STATIC_INIT_SAFE);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ConfigMappingBuildItem that = (ConfigMappingBuildItem) o;
        return configClass.equals(that.configClass) &&
                prefix.equals(that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configClass, prefix);
    }
}
