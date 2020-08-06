package io.quarkus.arc.deployment.configproperties;

import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.join;
import static io.quarkus.runtime.util.StringUtil.lowerCase;
import static io.quarkus.runtime.util.StringUtil.withoutSuffix;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.builder.item.MultiBuildItem;

public final class ConfigPropertiesMetadataBuildItem extends MultiBuildItem {

    private final ClassInfo classInfo;
    private final String prefix;
    private final ConfigProperties.NamingStrategy namingStrategy;
    private final boolean failOnMismatchingMember;
    private final boolean needsQualifier;

    public ConfigPropertiesMetadataBuildItem(ClassInfo classInfo, String prefix,
            ConfigProperties.NamingStrategy namingStrategy, boolean failOnMismatchingMember, boolean needsQualifier) {
        this.classInfo = classInfo;
        this.prefix = sanitisePrefix(prefix);
        this.namingStrategy = namingStrategy;
        this.failOnMismatchingMember = failOnMismatchingMember;
        this.needsQualifier = needsQualifier;
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
}
