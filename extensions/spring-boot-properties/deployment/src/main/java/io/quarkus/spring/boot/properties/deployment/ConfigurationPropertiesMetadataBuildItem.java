package io.quarkus.spring.boot.properties.deployment;

import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.lowerCase;
import static io.quarkus.runtime.util.StringUtil.withoutSuffix;

import java.util.function.BiFunction;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.smallrye.config.ConfigMapping;

final class ConfigurationPropertiesMetadataBuildItem extends MultiBuildItem {

    private final ClassInfo classInfo;
    private final String prefix;
    private final ConfigMapping.NamingStrategy namingStrategy;
    private final boolean failOnMismatchingMember;

    // used when the instance of the object needs to be created with special logic
    private final InstanceFactory instanceFactory;

    public ConfigurationPropertiesMetadataBuildItem(ClassInfo classInfo, String prefix,
            ConfigMapping.NamingStrategy namingStrategy, boolean failOnMismatchingMember) {
        this(classInfo, prefix, namingStrategy, failOnMismatchingMember, null);
    }

    public ConfigurationPropertiesMetadataBuildItem(ClassInfo classInfo, String prefix,
            ConfigMapping.NamingStrategy namingStrategy,
            boolean failOnMismatchingMember, InstanceFactory instanceFactory) {
        this.classInfo = classInfo;
        this.prefix = sanitisePrefix(prefix);
        this.namingStrategy = namingStrategy;
        this.failOnMismatchingMember = failOnMismatchingMember;
        this.instanceFactory = instanceFactory;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public String getPrefix() {
        return prefix;
    }

    public ConfigMapping.NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public boolean isFailOnMismatchingMember() {
        return failOnMismatchingMember;
    }

    public InstanceFactory getInstanceFactory() {
        return instanceFactory;
    }

    private String sanitisePrefix(String prefix) {
        if (prefix == null) {
            return getPrefixFromClassName(classInfo.name());
        }
        return prefix;
    }

    private String getPrefixFromClassName(DotName className) {
        String simpleName = className.isInner() ? className.local() : className.withoutPackagePrefix();
        return String.join("-",
                (Iterable<String>) () -> withoutSuffix(lowerCase(camelHumpsIterator(simpleName)), "config", "configuration",
                        "properties", "props"));
    }

    /**
     * Class that takes a {@link MethodCreator} and the config object class name
     * and produces an instance of that class
     */
    public interface InstanceFactory extends BiFunction<MethodCreator, String, ResultHandle> {

    }
}
