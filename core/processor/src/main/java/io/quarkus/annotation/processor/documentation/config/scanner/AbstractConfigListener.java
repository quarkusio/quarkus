package io.quarkus.annotation.processor.documentation.config.scanner;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigGroup;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigProperty;
import io.quarkus.annotation.processor.documentation.config.discovery.EnumDefinition;
import io.quarkus.annotation.processor.documentation.config.discovery.EnumDefinition.EnumConstant;
import io.quarkus.annotation.processor.documentation.config.discovery.ResolvedType;
import io.quarkus.annotation.processor.documentation.config.util.Types;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Utils;

public class AbstractConfigListener implements ConfigAnnotationListener {

    protected final Config config;
    protected final Utils utils;
    protected final ConfigCollector configCollector;

    protected AbstractConfigListener(Config config, Utils utils, ConfigCollector configCollector) {
        this.config = config;
        this.utils = utils;
        this.configCollector = configCollector;
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

    protected void handleCommonPropertyAnnotations(DiscoveryConfigProperty.Builder builder,
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
