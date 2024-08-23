package io.quarkus.annotation.processor.documentation.config.resolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigGroup;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigProperty;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigRoot;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryRootElement;
import io.quarkus.annotation.processor.documentation.config.discovery.EnumDefinition;
import io.quarkus.annotation.processor.documentation.config.discovery.ResolvedType;
import io.quarkus.annotation.processor.documentation.config.model.ConfigItemCollection;
import io.quarkus.annotation.processor.documentation.config.model.ConfigPhase;
import io.quarkus.annotation.processor.documentation.config.model.ConfigProperty;
import io.quarkus.annotation.processor.documentation.config.model.ConfigRoot;
import io.quarkus.annotation.processor.documentation.config.model.ConfigSection;
import io.quarkus.annotation.processor.documentation.config.model.EnumAcceptedValues;
import io.quarkus.annotation.processor.documentation.config.model.EnumAcceptedValues.EnumAcceptedValue;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements;
import io.quarkus.annotation.processor.documentation.config.model.ResolvedModel;
import io.quarkus.annotation.processor.documentation.config.scanner.ConfigCollector;
import io.quarkus.annotation.processor.documentation.config.util.ConfigNamingUtil;
import io.quarkus.annotation.processor.documentation.config.util.JavadocUtil;
import io.quarkus.annotation.processor.documentation.config.util.Markers;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Strings;
import io.quarkus.annotation.processor.util.Utils;

/**
 * The goal of this class is to resolve the elements obtained on scanning/discovery
 * and assemble them into the final model.
 * <p>
 * Note that the model is not exactly final as some elements might not be resolvable
 * because they are inside another module: this annotation processor doesn't cross
 * the module boundaries as it causes a lot of headaches (for instance for the Develocity
 * caching but not only).
 * <p>
 * NEVER CROSS THE STREAMS!
 */
public class ConfigResolver {

    private final Config config;
    private final Utils utils;
    private final ConfigCollector configCollector;

    public ConfigResolver(Config config, Utils utils, ConfigCollector configCollector) {
        this.config = config;
        this.utils = utils;
        this.configCollector = configCollector;
    }

    public JavadocElements resolveJavadoc() {
        return new JavadocElements(config.getExtension(), configCollector.getJavadocElements());
    }

    public ResolvedModel resolveModel() {
        List<ConfigRoot> configRoots = new ArrayList<>();

        for (DiscoveryConfigRoot discoveryConfigRoot : configCollector.getConfigRoots()) {
            ConfigRoot configRoot = new ConfigRoot(discoveryConfigRoot.getExtension(), discoveryConfigRoot.getPrefix(),
                    discoveryConfigRoot.getOverriddenDocPrefix(), discoveryConfigRoot.getOverriddenDocFileName());
            Map<String, ConfigSection> existingRootConfigSections = new HashMap<>();

            configRoot.addQualifiedName(discoveryConfigRoot.getQualifiedName());

            ResolutionContext context = new ResolutionContext(configRoot.getPrefix(), new ArrayList<>(), discoveryConfigRoot,
                    configRoot, 0, false, false, false);
            for (DiscoveryConfigProperty discoveryConfigProperty : discoveryConfigRoot.getProperties().values()) {
                resolveProperty(configRoot, existingRootConfigSections, discoveryConfigRoot.getPhase(), context,
                        discoveryConfigProperty);
            }

            configRoots.add(configRoot);
        }

        return new ResolvedModel(configRoots);
    }

    private void resolveProperty(ConfigRoot configRoot, Map<String, ConfigSection> existingRootConfigSections,
            ConfigPhase phase, ResolutionContext context, DiscoveryConfigProperty discoveryConfigProperty) {
        String propertyPath = appendPath(context.getPath(), discoveryConfigProperty.getPath());

        List<String> additionalPaths = context.getAdditionalPaths().stream()
                .map(p -> appendPath(p, discoveryConfigProperty.getPath()))
                .collect(Collectors.toCollection(ArrayList::new));
        boolean deprecated = context.isDeprecated() || discoveryConfigProperty.isDeprecated();

        String typeQualifiedName = discoveryConfigProperty.getType().qualifiedName();

        if (configCollector.isResolvedConfigGroup(typeQualifiedName)) {
            DiscoveryConfigGroup discoveryConfigGroup = configCollector.getResolvedConfigGroup(typeQualifiedName);

            String potentiallyMappedPath = propertyPath;
            if (discoveryConfigProperty.getType().isMap()) {
                if (discoveryConfigProperty.isUnnamedMapKey()) {
                    ListIterator<String> additionalPathsIterator = additionalPaths.listIterator();

                    additionalPathsIterator
                            .add(propertyPath + ConfigNamingUtil.getMapKey(discoveryConfigProperty.getMapKey()));
                    while (additionalPathsIterator.hasNext()) {
                        additionalPathsIterator.add(additionalPathsIterator.next()
                                + ConfigNamingUtil.getMapKey(discoveryConfigProperty.getMapKey()));
                    }
                } else {
                    potentiallyMappedPath += ConfigNamingUtil.getMapKey(discoveryConfigProperty.getMapKey());
                    additionalPaths = additionalPaths.stream()
                            .map(p -> p + ConfigNamingUtil.getMapKey(discoveryConfigProperty.getMapKey()))
                            .collect(Collectors.toCollection(ArrayList::new));
                }
            }

            ResolutionContext configGroupContext;

            boolean isWithinMap = context.isWithinMap() || discoveryConfigProperty.getType().isMap();
            boolean isWithMapWithUnnamedKey = context.isWithinMapWithUnnamedKey() || discoveryConfigProperty.isUnnamedMapKey();

            if (discoveryConfigProperty.isSection()) {
                ConfigSection configSection = existingRootConfigSections.get(propertyPath);

                if (configSection != null) {
                    configSection.appendState(discoveryConfigProperty.isSectionGenerated(), deprecated);
                } else {
                    configSection = new ConfigSection(discoveryConfigProperty.getSourceClass(),
                            discoveryConfigProperty.getSourceName(), propertyPath, typeQualifiedName,
                            context.getSectionLevel(), discoveryConfigProperty.isSectionGenerated(), deprecated);
                    context.getItemCollection().addItem(configSection);
                    existingRootConfigSections.put(propertyPath, configSection);
                }

                configGroupContext = new ResolutionContext(potentiallyMappedPath, additionalPaths, discoveryConfigGroup,
                        configSection, context.getSectionLevel() + 1, isWithinMap, isWithMapWithUnnamedKey, deprecated);
            } else {
                configGroupContext = new ResolutionContext(potentiallyMappedPath, additionalPaths, discoveryConfigGroup,
                        context.getItemCollection(), context.getSectionLevel(), isWithinMap, isWithMapWithUnnamedKey,
                        deprecated);
            }

            for (DiscoveryConfigProperty configGroupProperty : discoveryConfigGroup.getProperties().values()) {
                resolveProperty(configRoot, existingRootConfigSections, phase, configGroupContext, configGroupProperty);
            }
        } else {
            String typeBinaryName = discoveryConfigProperty.getType().binaryName();
            String typeSimplifiedName = discoveryConfigProperty.getType().simplifiedName();

            // if the property has a converter, we don't hyphenate the values (per historical rules, not exactly sure of the reason)
            boolean hyphenateEnumValues = discoveryConfigProperty.isEnforceHyphenateEnumValue() ||
                    !discoveryConfigProperty.isConverted();

            String defaultValue = getDefaultValue(discoveryConfigProperty.getDefaultValue(),
                    discoveryConfigProperty.getDefaultValueForDoc(), discoveryConfigProperty.getType(), hyphenateEnumValues);

            EnumAcceptedValues enumAcceptedValues = null;
            if (discoveryConfigProperty.getType().isEnum()) {
                EnumDefinition enumDefinition = configCollector.getResolvedEnum(typeQualifiedName);
                Map<String, EnumAcceptedValue> localAcceptedValues = enumDefinition.constants().entrySet().stream()
                        .collect(Collectors.toMap(
                                e -> e.getKey(),
                                e -> new EnumAcceptedValue(e.getValue().hasExplicitValue() ? e.getValue().explicitValue()
                                        : (hyphenateEnumValues ? ConfigNamingUtil.hyphenateEnumValue(e.getKey())
                                                : e.getKey())),
                                (x, y) -> y, LinkedHashMap::new));
                enumAcceptedValues = new EnumAcceptedValues(enumDefinition.qualifiedName(), localAcceptedValues);
            }

            String potentiallyMappedPath = propertyPath;
            boolean optional = discoveryConfigProperty.getType().isOptional();

            if (discoveryConfigProperty.getType().isMap()) {
                // it is a leaf pass through map, it is always optional
                optional = true;
                typeQualifiedName = discoveryConfigProperty.getType().wrapperType().toString();
                typeSimplifiedName = utils.element().simplifyGenericType(discoveryConfigProperty.getType().wrapperType());

                potentiallyMappedPath += ConfigNamingUtil.getMapKey(discoveryConfigProperty.getMapKey());
                additionalPaths = additionalPaths.stream()
                        .map(p -> p + ConfigNamingUtil.getMapKey(discoveryConfigProperty.getMapKey()))
                        .collect(Collectors.toCollection(ArrayList::new));
            } else if (discoveryConfigProperty.getType().isList()) {
                typeQualifiedName = discoveryConfigProperty.getType().wrapperType().toString();
            }

            // this is a standard property
            ConfigProperty configProperty = new ConfigProperty(phase,
                    discoveryConfigProperty.getSourceClass(),
                    discoveryConfigProperty.getSourceName(), potentiallyMappedPath, additionalPaths,
                    ConfigNamingUtil.toEnvVarName(potentiallyMappedPath), typeQualifiedName, typeSimplifiedName,
                    discoveryConfigProperty.getType().isMap(), discoveryConfigProperty.getType().isList(),
                    optional, discoveryConfigProperty.getMapKey(),
                    discoveryConfigProperty.isUnnamedMapKey(), context.isWithinMap(),
                    discoveryConfigProperty.isConverted(),
                    discoveryConfigProperty.getType().isEnum(),
                    enumAcceptedValues, defaultValue,
                    JavadocUtil.getJavadocSiteLink(typeBinaryName),
                    deprecated);
            context.getItemCollection().addItem(configProperty);
        }
    }

    public static String getDefaultValue(String defaultValue, String defaultValueForDoc, ResolvedType type,
            boolean hyphenateEnumValues) {
        if (!Strings.isBlank(defaultValueForDoc)) {
            return defaultValueForDoc;
        }

        if (defaultValue == null) {
            return null;
        }

        if (type.isEnum() && hyphenateEnumValues) {
            if (type.isList()) {
                return Arrays.stream(defaultValue.split(Markers.COMMA))
                        .map(v -> ConfigNamingUtil.hyphenateEnumValue(v.trim()))
                        .collect(Collectors.joining(Markers.COMMA));
            } else {
                return ConfigNamingUtil.hyphenateEnumValue(defaultValue.trim());
            }
        }

        return defaultValue;
    }

    public static String getType(TypeMirror typeMirror) {
        if (typeMirror instanceof DeclaredType) {
            DeclaredType declaredType = (DeclaredType) typeMirror;
            TypeElement typeElement = (TypeElement) declaredType.asElement();
            return typeElement.getQualifiedName().toString();
        }
        return typeMirror.toString();
    }

    public static String appendPath(String parentPath, String path) {
        return Markers.PARENT.equals(path) ? parentPath : parentPath + Markers.DOT + path;
    }

    private static class ResolutionContext {

        private final String path;
        private final List<String> additionalPaths;
        private final DiscoveryRootElement discoveryRootElement;
        private final ConfigItemCollection itemCollection;
        private final int sectionLevel;
        private final boolean withinMap;
        private final boolean withinMapWithUnnamedKey;
        private final boolean deprecated;

        private ResolutionContext(String path, List<String> additionalPaths, DiscoveryRootElement discoveryRootElement,
                ConfigItemCollection itemCollection,
                int sectionLevel, boolean withinMap, boolean withinMapWithUnnamedKey, boolean deprecated) {
            this.path = path;
            this.additionalPaths = additionalPaths;
            this.discoveryRootElement = discoveryRootElement;
            this.itemCollection = itemCollection;
            this.withinMap = withinMap;
            this.withinMapWithUnnamedKey = withinMapWithUnnamedKey;
            this.deprecated = deprecated;
            this.sectionLevel = sectionLevel;
        }

        public String getPath() {
            return path;
        }

        public List<String> getAdditionalPaths() {
            return additionalPaths;
        }

        public DiscoveryRootElement getDiscoveryRootElement() {
            return discoveryRootElement;
        }

        public ConfigItemCollection getItemCollection() {
            return itemCollection;
        }

        public int getSectionLevel() {
            return sectionLevel;
        }

        public boolean isWithinMap() {
            return withinMap;
        }

        public boolean isWithinMapWithUnnamedKey() {
            return withinMapWithUnnamedKey;
        }

        public boolean isDeprecated() {
            return deprecated;
        }
    }
}
