package io.quarkus.annotation.processor.documentation.config.scanner;

import java.util.Map;
import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigGroup;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigProperty;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigRoot;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryRootElement;
import io.quarkus.annotation.processor.documentation.config.discovery.ResolvedType;
import io.quarkus.annotation.processor.documentation.config.model.ConfigPhase;
import io.quarkus.annotation.processor.documentation.config.model.SourceElementType;
import io.quarkus.annotation.processor.documentation.config.util.ConfigNamingUtil;
import io.quarkus.annotation.processor.documentation.config.util.Markers;
import io.quarkus.annotation.processor.documentation.config.util.Types;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Strings;
import io.quarkus.annotation.processor.util.Utils;

public class LegacyConfigRootListener extends AbstractConfigListener {

    LegacyConfigRootListener(Config config, Utils utils, ConfigCollector configCollector) {
        super(config, utils, configCollector);
    }

    @Override
    public Optional<DiscoveryConfigRoot> onConfigRoot(TypeElement configRoot) {
        if (config.getExtension().isMixedModule() && configRoot.getKind() == ElementKind.INTERFACE) {
            return Optional.empty();
        }

        String prefix = Markers.DEFAULT_PREFIX;
        ConfigPhase configPhase = ConfigPhase.BUILD_TIME;

        AnnotationMirror configRootAnnotation = null;
        AnnotationMirror configDocPrefixAnnotation = null;
        AnnotationMirror configDocFileNameAnnotation = null;

        for (AnnotationMirror annotationMirror : configRoot.getAnnotationMirrors()) {
            String annotationName = utils.element().getQualifiedName(annotationMirror.getAnnotationType());

            if (annotationName.equals(Types.ANNOTATION_CONFIG_ROOT)) {
                configRootAnnotation = annotationMirror;
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

        if (configRootAnnotation == null) {
            throw new IllegalStateException("@ConfigRoot is missing on " + configRoot);
        }

        final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = configRootAnnotation
                .getElementValues();
        String name = Markers.HYPHENATED_ELEMENT_NAME;
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
            final String key = entry.getKey().toString();
            final String value = entry.getValue().getValue().toString();
            if ("name()".equals(key)) {
                name = value;
            } else if ("phase()".equals(key)) {
                configPhase = ConfigPhase.valueOf(value);
            } else if ("prefix()".equals(key)) {
                prefix = value;
            }
        }

        validateRuntimeConfigOnDeploymentModules(configPhase, configRoot);

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

        String rootPrefix = ConfigNamingUtil.getRootPrefix(prefix, name, configRoot.getSimpleName().toString(), configPhase);
        String binaryName = utils.element().getBinaryName(configRoot);

        DiscoveryConfigRoot discoveryConfigRoot = new DiscoveryConfigRoot(config.getExtension(),
                rootPrefix, overriddenDocPrefix,
                binaryName, configRoot.getQualifiedName().toString(),
                configPhase, overriddenDocFileName, false);
        configCollector.addConfigRoot(discoveryConfigRoot);
        return Optional.of(discoveryConfigRoot);
    }

    @Override
    public void onEnclosedField(DiscoveryRootElement discoveryRootElement, TypeElement clazz, VariableElement field,
            ResolvedType resolvedType) {
        if (config.getExtension().isMixedModule() && discoveryRootElement.isConfigMapping()) {
            return;
        }

        Map<String, AnnotationMirror> fieldAnnotations = utils.element().getAnnotations(field);

        String sourceElementName = field.getSimpleName().toString();
        String name = ConfigNamingUtil.hyphenate(sourceElementName);

        DiscoveryConfigProperty.Builder builder = DiscoveryConfigProperty.builder(clazz.getQualifiedName().toString(),
                sourceElementName, SourceElementType.FIELD, resolvedType);

        AnnotationMirror configItemAnnotation = fieldAnnotations.get(Types.ANNOTATION_CONFIG_ITEM);
        if (configItemAnnotation != null) {
            Map<String, Object> configItemValues = utils.element().getAnnotationValues(configItemAnnotation);

            String configItemName = (String) configItemValues.get("name");
            if (configItemName != null && !Markers.HYPHENATED_ELEMENT_NAME.equals(configItemName)) {
                name = configItemName;
            }

            String configItemDefaultValue = (String) configItemValues.get("defaultValue");
            if (configItemDefaultValue != null && !Markers.NO_DEFAULT.equals(configItemDefaultValue)) {
                builder.defaultValue(configItemDefaultValue);
            }

            String configItemDefaultValueForDoc = (String) configItemValues.get("defaultValueDocumentation");
            if (!Strings.isEmpty(configItemDefaultValueForDoc)) {
                builder.defaultValueForDoc(configItemDefaultValueForDoc);
            } else {
                // while ConfigDocDefault was added for ConfigMappings, it's allowed on fields so let's be safe
                AnnotationMirror configDocDefaultAnnotation = fieldAnnotations.get(Types.ANNOTATION_CONFIG_DOC_DEFAULT);
                if (configDocDefaultAnnotation != null) {
                    builder.defaultValueForDoc(
                            configDocDefaultAnnotation.getElementValues().values().iterator().next().getValue().toString());
                }
            }
        }
        builder.name(name);

        if (resolvedType.isMap()) {
            String mapKey = ConfigNamingUtil.hyphenate(sourceElementName);
            AnnotationMirror configDocMapKeyAnnotation = fieldAnnotations.get(Types.ANNOTATION_CONFIG_DOC_MAP_KEY);
            if (configDocMapKeyAnnotation != null) {
                mapKey = configDocMapKeyAnnotation.getElementValues().values().iterator().next().getValue().toString();
            }
            builder.mapKey(mapKey);
        }

        if (fieldAnnotations.containsKey(Types.ANNOTATION_DEFAULT_CONVERTER) ||
                fieldAnnotations.containsKey(Types.ANNOTATION_CONVERT_WITH)) {
            builder.converted();
        }

        handleCommonPropertyAnnotations(builder, fieldAnnotations, resolvedType, sourceElementName);

        discoveryRootElement.addProperty(builder.build());
    }

    @Deprecated(forRemoval = true)
    @Override
    public Optional<DiscoveryConfigGroup> onConfigGroup(TypeElement configGroup) {
        if (config.getExtension().isMixedModule() && configGroup.getKind() == ElementKind.INTERFACE) {
            return Optional.empty();
        }

        return super.onConfigGroup(configGroup);
    }
}
