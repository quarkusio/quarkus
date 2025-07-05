package io.quarkus.annotation.processor.documentation.config.scanner;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigGroup;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigProperty;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigRoot;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryRootElement;
import io.quarkus.annotation.processor.documentation.config.discovery.EnumDefinition;
import io.quarkus.annotation.processor.documentation.config.discovery.EnumDefinition.EnumConstant;
import io.quarkus.annotation.processor.documentation.config.discovery.ResolvedType;
import io.quarkus.annotation.processor.documentation.config.model.ConfigPhase;
import io.quarkus.annotation.processor.documentation.config.model.ExtensionModule;
import io.quarkus.annotation.processor.documentation.config.model.SourceElementType;
import io.quarkus.annotation.processor.documentation.config.util.ConfigNamingUtil;
import io.quarkus.annotation.processor.documentation.config.util.Markers;
import io.quarkus.annotation.processor.documentation.config.util.Types;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Utils;

public class ConfigMappingListener implements ConfigAnnotationListener {

    private final Config config;
    private final Utils utils;
    private final ConfigCollector configCollector;

    ConfigMappingListener(Config config, Utils utils, ConfigCollector configCollector) {
        this.config = config;
        this.utils = utils;
        this.configCollector = configCollector;
    }

    @Override
    public Optional<DiscoveryConfigRoot> onConfigRoot(TypeElement configRoot) {
        String prefix = Markers.DEFAULT_PREFIX;
        ConfigPhase configPhase = ConfigPhase.BUILD_TIME;

        AnnotationMirror configRootAnnotation = null;
        AnnotationMirror configMappingAnnotion = null;
        AnnotationMirror configDocPrefixAnnotation = null;
        AnnotationMirror configDocFileNameAnnotation = null;

        for (AnnotationMirror annotationMirror : configRoot.getAnnotationMirrors()) {
            String annotationName = utils.element().getQualifiedName(annotationMirror.getAnnotationType());

            if (annotationName.equals(Types.ANNOTATION_CONFIG_ROOT)) {
                configRootAnnotation = annotationMirror;
                continue;
            }
            if (annotationName.equals(Types.ANNOTATION_CONFIG_MAPPING)) {
                configMappingAnnotion = annotationMirror;
                continue;
            }
            if (annotationName.equals(Types.ANNOTATION_CONFIG_DOC_PREFIX)) {
                configDocPrefixAnnotation = annotationMirror;
                continue;
            }
            if (annotationName.equals(Types.ANNOTATION_CONFIG_DOC_FILE_NAME)) {
                configDocFileNameAnnotation = annotationMirror;
                continue;
            }
        }

        if (configRootAnnotation == null || configMappingAnnotion == null) {
            throw new IllegalStateException("Either @ConfigRoot or @ConfigMapping is missing on " + configRoot);
        }

        final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = configRootAnnotation
                .getElementValues();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
            if ("phase()".equals(entry.getKey().toString())) {
                configPhase = ConfigPhase.valueOf(entry.getValue().getValue().toString());
            }
        }

        validateRuntimeConfigOnDeploymentModules(configPhase, configRoot);

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : configMappingAnnotion.getElementValues()
                .entrySet()) {
            if ("prefix()".equals(entry.getKey().toString())) {
                prefix = entry.getValue().getValue().toString();
            }
        }

        String overriddenDocPrefix = null;
        if (configDocPrefixAnnotation != null) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : configDocPrefixAnnotation
                    .getElementValues()
                    .entrySet()) {
                if ("value()".equals(entry.getKey().toString())) {
                    overriddenDocPrefix = entry.getValue().getValue().toString();
                    break;
                }
            }
        }

        String overriddenDocFileName = null;
        if (configDocFileNameAnnotation != null) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : configDocFileNameAnnotation
                    .getElementValues()
                    .entrySet()) {
                if ("value()".equals(entry.getKey().toString())) {
                    overriddenDocFileName = entry.getValue().getValue().toString();
                    break;
                }
            }
        }

        String rootPrefix = ConfigNamingUtil.getRootPrefix(prefix, "", configRoot.getSimpleName().toString(), configPhase);
        String binaryName = utils.element().getBinaryName(configRoot);

        DiscoveryConfigRoot discoveryConfigRoot = new DiscoveryConfigRoot(config.getExtension(),
                rootPrefix, overriddenDocPrefix,
                binaryName, configRoot.getQualifiedName().toString(), configPhase, overriddenDocFileName, true);
        configCollector.addConfigRoot(discoveryConfigRoot);
        return Optional.of(discoveryConfigRoot);
    }

    @Override
    public void onEnclosedMethod(DiscoveryRootElement discoveryRootElement, TypeElement clazz, ExecutableElement method,
            ResolvedType resolvedType) {
        Map<String, AnnotationMirror> methodAnnotations = utils.element().getAnnotations(method);

        String sourceElementName = method.getSimpleName().toString();
        DiscoveryConfigProperty.Builder builder = DiscoveryConfigProperty.builder(clazz.getQualifiedName().toString(),
                sourceElementName, SourceElementType.METHOD, resolvedType);

        String name = ConfigNamingUtil.hyphenate(sourceElementName);
        AnnotationMirror withNameAnnotation = methodAnnotations.get(Types.ANNOTATION_CONFIG_WITH_NAME);
        if (withNameAnnotation != null) {
            name = withNameAnnotation.getElementValues().values().iterator().next().getValue().toString();
        }
        if (methodAnnotations.containsKey(Types.ANNOTATION_CONFIG_WITH_PARENT_NAME)) {
            name = Markers.PARENT;
        }
        builder.name(name);

        AnnotationMirror withDefaultAnnotation = methodAnnotations.get(Types.ANNOTATION_CONFIG_WITH_DEFAULT);
        if (withDefaultAnnotation != null) {
            builder.defaultValue(withDefaultAnnotation.getElementValues().values().isEmpty() ? null
                    : withDefaultAnnotation.getElementValues().values().iterator().next().getValue().toString());
        }

        AnnotationMirror configDocDefaultAnnotation = methodAnnotations.get(Types.ANNOTATION_CONFIG_DOC_DEFAULT);
        if (configDocDefaultAnnotation != null) {
            builder.defaultValueForDoc(
                    configDocDefaultAnnotation.getElementValues().values().iterator().next().getValue().toString());
        }

        if (resolvedType.isMap()) {
            String mapKey = ConfigNamingUtil.hyphenate(sourceElementName);
            AnnotationMirror configDocMapKeyAnnotation = methodAnnotations.get(Types.ANNOTATION_CONFIG_DOC_MAP_KEY);
            if (configDocMapKeyAnnotation != null) {
                mapKey = configDocMapKeyAnnotation.getElementValues().values().iterator().next().getValue().toString();
            }
            builder.mapKey(mapKey);

            AnnotationMirror unnamedMapKeyAnnotation = methodAnnotations.get(Types.ANNOTATION_CONFIG_WITH_UNNAMED_KEY);
            if (unnamedMapKeyAnnotation != null) {
                builder.unnamedMapKey();
            }
        }

        if (methodAnnotations.containsKey(Types.ANNOTATION_CONFIG_WITH_CONVERTER)) {
            builder.converted();
        }

        handleCommonPropertyAnnotations(builder, methodAnnotations, resolvedType, sourceElementName);

        discoveryRootElement.addProperty(builder.build());
    }

    @Override
    public Optional<DiscoveryConfigGroup> onConfigGroup(TypeElement configGroup) {
        DiscoveryConfigGroup discoveryConfigGroup = new DiscoveryConfigGroup(config.getExtension(),
                utils.element().getBinaryName(configGroup),
                configGroup.getQualifiedName().toString(),
                // interface config groups are considered config mappings, let's hope it's enough
                configGroup.getKind() == ElementKind.INTERFACE);
        configCollector.addResolvedConfigGroup(discoveryConfigGroup);
        return Optional.of(discoveryConfigGroup);
    }

    @Override
    public void onResolvedEnum(TypeElement enumTypeElement) {
        Map<String, EnumConstant> enumConstants = new LinkedHashMap<>();

        for (Element enumElement : enumTypeElement.getEnclosedElements()) {
            if (enumElement.getKind() != ElementKind.ENUM_CONSTANT) {
                continue;
            }

            String explicitValue = null;
            Map<String, AnnotationMirror> annotations = utils.element().getAnnotations(enumElement);
            AnnotationMirror configDocEnumValue = annotations.get(Types.ANNOTATION_CONFIG_DOC_ENUM_VALUE);
            if (configDocEnumValue != null) {
                Map<String, Object> enumValueValues = utils.element().getAnnotationValues(configDocEnumValue);
                explicitValue = (String) enumValueValues.get("value");
            }

            enumConstants.put(enumElement.getSimpleName().toString(), new EnumConstant(explicitValue));
        }

        EnumDefinition enumDefinition = new EnumDefinition(enumTypeElement.getQualifiedName().toString(),
                enumConstants);
        configCollector.addResolvedEnum(enumDefinition);
    }

    private void validateRuntimeConfigOnDeploymentModules(ConfigPhase configPhase, TypeElement configRoot) {
        if (configPhase.equals(ConfigPhase.RUN_TIME) || configPhase.equals(ConfigPhase.BUILD_AND_RUN_TIME_FIXED)) {
            ExtensionModule.ExtensionModuleType type = config.getExtensionModule().type();
            if (type.equals(ExtensionModule.ExtensionModuleType.DEPLOYMENT)) {
                throw new IllegalStateException(String.format(
                        "Error on %s: Configuration classes with ConfigPhase.RUN_TIME or " +
                                "ConfigPhase.BUILD_AND_RUNTIME_FIXED phases, must reside in the respective module.",
                        configRoot.getSimpleName().toString()));
            }
        }
    }

    private void handleCommonPropertyAnnotations(DiscoveryConfigProperty.Builder builder,
            Map<String, AnnotationMirror> propertyAnnotations, ResolvedType resolvedType, String sourceElementName) {

        AnnotationMirror deprecatedAnnotation = propertyAnnotations.get(Deprecated.class.getName());
        if (deprecatedAnnotation != null) {
            String since = (String) utils.element().getAnnotationValues(deprecatedAnnotation).get("since");
            // TODO add more information about the deprecation, typically the reason and a replacement
            builder.deprecated(since, null, null);
        }

        AnnotationMirror configDocSectionAnnotation = propertyAnnotations.get(Types.ANNOTATION_CONFIG_DOC_SECTION);
        if (configDocSectionAnnotation != null) {
            Boolean sectionGenerated = (Boolean) utils.element().getAnnotationValues(configDocSectionAnnotation)
                    .get("generated");
            if (sectionGenerated != null && sectionGenerated) {
                builder.section(true);
            } else {
                builder.section(false);
            }
        }

        AnnotationMirror configDocEnum = propertyAnnotations.get(Types.ANNOTATION_CONFIG_DOC_ENUM);
        if (configDocEnum != null) {
            Boolean enforceHyphenateValues = (Boolean) utils.element().getAnnotationValues(configDocEnum)
                    .get("enforceHyphenateValues");
            if (enforceHyphenateValues != null && enforceHyphenateValues) {
                builder.enforceHyphenateEnumValues();
            }
        }
    }
}
