package io.quarkus.deployment.builditem;

import static io.quarkus.deployment.builditem.ConfigClassInfo.collectTypes;

import java.util.Objects;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Discovered classes annotated with {@link org.eclipse.microprofile.config.inject.ConfigProperties}.
 *
 * @see org.eclipse.microprofile.config.inject.ConfigProperties
 */
public final class ConfigPropertiesBuildItem extends MultiBuildItem
        implements ConfigClassInfo, Comparable<ConfigPropertiesBuildItem> {
    private final ClassInfo configClass;
    private final String prefix;
    private final Set<Type> types;

    public ConfigPropertiesBuildItem(ClassInfo configClass, String prefix, IndexView index) {
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ConfigPropertiesBuildItem that = (ConfigPropertiesBuildItem) o;
        return configClass.equals(that.configClass) &&
                prefix.equals(that.prefix);
    }

    @Override
    public int compareTo(ConfigPropertiesBuildItem o) {
        int result = getConfigClassName().compareTo(o.getConfigClassName());
        if (result != 0) {
            return result;
        }
        return this.prefix.compareTo(o.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configClass, prefix);
    }
}
