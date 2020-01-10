package io.quarkus.arc.deployment.configproperties;

import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.join;
import static io.quarkus.runtime.util.StringUtil.lowerCase;
import static io.quarkus.runtime.util.StringUtil.withoutSuffix;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.builder.item.MultiBuildItem;

public final class ConfigPropertiesMetadataBuildItem extends MultiBuildItem {

    private static final DotName CONFIG_PROPERTIES_ANNOTATION = DotName.createSimple(ConfigProperties.class.getName());

    private final ClassInfo classInfo;
    private final String prefix;
    private final ConfigProperties.NamingStrategy namingStrategy;

    public ConfigPropertiesMetadataBuildItem(AnnotationInstance annotation, ConfigProperties.NamingStrategy defaultStrategy) {
        if (!CONFIG_PROPERTIES_ANNOTATION.equals(annotation.name())) {
            throw new IllegalArgumentException(annotation + " is not an instance of " + ConfigProperties.class.getSimpleName());
        }

        this.classInfo = annotation.target().asClass();
        this.prefix = extractPrefix(annotation);
        AnnotationValue namingStrategyValue = annotation.value("namingStrategy");
        this.namingStrategy = namingStrategyValue == null ? defaultStrategy
                : ConfigProperties.NamingStrategy.valueOf(namingStrategyValue.asEnum());
    }

    public ConfigPropertiesMetadataBuildItem(ClassInfo classInfo, String prefix,
            ConfigProperties.NamingStrategy namingStrategy) {
        this.classInfo = classInfo;
        this.prefix = sanitisePrefix(prefix);
        this.namingStrategy = namingStrategy;
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

    private String extractPrefix(AnnotationInstance annotationInstance) {
        AnnotationValue value = annotationInstance.value("prefix");
        return sanitisePrefix(value == null ? null : value.asString());
    }

    private String sanitisePrefix(String prefix) {
        if (isPrefixUnset(prefix)) {
            return getPrefixFromClassName(classInfo.name());
        }
        return prefix;
    }

    private boolean isPrefixUnset(String prefix) {
        return prefix == null || "".equals(prefix.trim()) || ConfigProperties.UNSET_PREFIX.equals(prefix.trim());
    }

    private String getPrefixFromClassName(DotName className) {
        String simpleName = className.isInner() ? className.local() : className.withoutPackagePrefix();
        return join("-",
                withoutSuffix(lowerCase(camelHumpsIterator(simpleName)), "config", "configuration",
                        "properties", "props"));
    }
}
