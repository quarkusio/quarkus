package io.quarkus.deployment.configuration.definition;

import static io.quarkus.deployment.configuration.RunTimeConfigurationGenerator.CONFIG_CLASS_NAME;
import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.lowerCase;
import static io.quarkus.runtime.util.StringUtil.lowerCaseFirst;
import static io.quarkus.runtime.util.StringUtil.toList;
import static io.quarkus.runtime.util.StringUtil.withoutSuffix;

import java.util.List;

import org.wildfly.common.Assert;

import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;

/**
 *
 */
public final class RootDefinition extends ClassDefinition {
    private final String prefix;
    private final ConfigPhase configPhase;
    private final String rootName;
    private final FieldDescriptor descriptor;

    RootDefinition(final Builder builder) {
        super(builder);
        this.prefix = builder.prefix;
        this.configPhase = builder.configPhase;
        String rootName = builder.rootName;
        final Class<?> configClass = getConfigurationClass();
        final List<String> segments = toList(camelHumpsIterator(configClass.getSimpleName()));
        final List<String> trimmedSegments;
        if (configPhase == ConfigPhase.RUN_TIME) {
            trimmedSegments = withoutSuffix(
                    withoutSuffix(
                            withoutSuffix(
                                    withoutSuffix(
                                            withoutSuffix(
                                                    withoutSuffix(
                                                            segments,
                                                            "Runtime", "Configuration"),
                                                    "Runtime", "Config"),
                                            "Run", "Time", "Configuration"),
                                    "Run", "Time", "Config"),
                            "Configuration"),
                    "Config");
        } else if (configPhase == ConfigPhase.BOOTSTRAP) {
            trimmedSegments = withoutSuffix(
                    withoutSuffix(
                            withoutSuffix(
                                    withoutSuffix(
                                            segments,
                                            "Bootstrap", "Configuration"),
                                    "Bootstrap", "Config"),
                            "Configuration"),
                    "Config");
        } else {
            trimmedSegments = withoutSuffix(
                    withoutSuffix(
                            withoutSuffix(
                                    withoutSuffix(
                                            segments,
                                            "Build", "Time", "Configuration"),
                                    "Build", "Time", "Config"),
                            "Configuration"),
                    "Config");
        }
        if (rootName.equals(ConfigItem.PARENT)) {
            rootName = "";
        } else if (rootName.equals(ConfigItem.ELEMENT_NAME)) {
            rootName = String.join("", (Iterable<String>) () -> lowerCaseFirst(trimmedSegments.iterator()));
        } else if (rootName.equals(ConfigItem.HYPHENATED_ELEMENT_NAME)) {
            rootName = String.join("-", (Iterable<String>) () -> lowerCase(trimmedSegments.iterator()));
        }
        this.rootName = rootName;
        this.descriptor = FieldDescriptor.of(CONFIG_CLASS_NAME, String.join("", segments), configClass);
    }

    public String getPrefix() {
        return prefix;
    }

    public ConfigPhase getConfigPhase() {
        return configPhase;
    }

    public String getRootName() {
        return rootName;
    }

    public String getName() {
        if (prefix != null && !prefix.isEmpty()) {
            if (rootName != null && !rootName.isEmpty()) {
                return prefix + "." + rootName;
            } else {
                return prefix;
            }
        } else {
            return rootName;
        }
    }

    public FieldDescriptor getDescriptor() {
        return descriptor;
    }

    public static final class Builder extends ClassDefinition.Builder {
        private String prefix = "quarkus";
        private ConfigPhase configPhase = ConfigPhase.BUILD_TIME;
        private String rootName = ConfigItem.HYPHENATED_ELEMENT_NAME;

        public Builder() {
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(final String prefix) {
            this.prefix = prefix;
        }

        public ConfigPhase getConfigPhase() {
            return configPhase;
        }

        public Builder setConfigPhase(final ConfigPhase configPhase) {
            Assert.checkNotNullParam("configPhase", configPhase);
            this.configPhase = configPhase;
            return this;
        }

        public String getRootName() {
            return rootName;
        }

        public Builder setRootName(final String rootName) {
            Assert.checkNotNullParam("rootName", rootName);
            this.rootName = rootName;
            return this;
        }

        public RootDefinition build() {
            return new RootDefinition(this);
        }
    }
}
