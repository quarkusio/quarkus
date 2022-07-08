package io.quarkus.arc.deployment.configproperties;

import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.join;
import static io.quarkus.runtime.util.StringUtil.lowerCase;
import static io.quarkus.runtime.util.StringUtil.withoutSuffix;

import java.util.function.BiFunction;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;

public final class ConfigPropertiesMetadataBuildItem extends MultiBuildItem {

    private final ClassInfo classInfo;
    private final String prefix;
    private final ConfigProperties.NamingStrategy namingStrategy;
    private final boolean failOnMismatchingMember;
    private final boolean needsQualifier;

    // used when the instance of the object needs to be created with special logic
    private final InstanceFactory instanceFactory;

    public ConfigPropertiesMetadataBuildItem(ClassInfo classInfo, String prefix,
            ConfigProperties.NamingStrategy namingStrategy, boolean failOnMismatchingMember, boolean needsQualifier) {
        this.classInfo = classInfo;
        this.prefix = sanitisePrefix(prefix);
        this.namingStrategy = namingStrategy;
        this.failOnMismatchingMember = failOnMismatchingMember;
        this.needsQualifier = needsQualifier;
        this.instanceFactory = null;
    }

    public ConfigPropertiesMetadataBuildItem(ClassInfo classInfo, String prefix,
            ConfigProperties.NamingStrategy namingStrategy,
            boolean failOnMismatchingMember, boolean needsQualifier,
            InstanceFactory instanceFactory) {
        this.classInfo = classInfo;
        this.prefix = sanitisePrefix(prefix);
        this.namingStrategy = namingStrategy;
        this.failOnMismatchingMember = failOnMismatchingMember;
        this.needsQualifier = needsQualifier;
        this.instanceFactory = instanceFactory;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public String getPrefix() {
        return prefix;
    }

    public ConfigProperties.NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public boolean isFailOnMismatchingMember() {
        return failOnMismatchingMember;
    }

    public boolean isNeedsQualifier() {
        return needsQualifier;
    }

    public InstanceFactory getInstanceFactory() {
        return instanceFactory;
    }

    private String sanitisePrefix(String prefix) {
        if (isPrefixUnset(prefix)) {
            return getPrefixFromClassName(classInfo.name());
        }
        return prefix;
    }

    private boolean isPrefixUnset(String prefix) {
        return prefix == null || prefix.trim().isEmpty() || ConfigProperties.UNSET_PREFIX.equals(prefix.trim());
    }

    private String getPrefixFromClassName(DotName className) {
        String simpleName = className.isInner() ? className.local() : className.withoutPackagePrefix();
        return join("-",
                withoutSuffix(lowerCase(camelHumpsIterator(simpleName)), "config", "configuration",
                        "properties", "props"));
    }

    /**
     * Class that takes a {@link MethodCreator} and the config object class name
     * and produces an instance of that class
     */
    public interface InstanceFactory extends BiFunction<MethodCreator, String, ResultHandle> {

    }
}
