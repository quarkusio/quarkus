package io.quarkus.annotation.processor.documentation.config.scanner;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigGroup;
import io.quarkus.annotation.processor.documentation.config.discovery.EnumDefinition;
import io.quarkus.annotation.processor.documentation.config.discovery.EnumDefinition.EnumConstant;
import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadoc;
import io.quarkus.annotation.processor.documentation.config.discovery.UnresolvedEnumDefinition;
import io.quarkus.annotation.processor.documentation.config.discovery.UnresolvedEnumDefinition.UnresolvedEnumConstant;
import io.quarkus.annotation.processor.documentation.config.formatter.JavadocToAsciidocTransformer;
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
                configGroup.getQualifiedName().toString());
        configCollector.addResolvedConfigGroup(discoveryConfigGroup);
        return Optional.of(discoveryConfigGroup);
    }

    @Override
    public void onUnresolvedConfigGroup(TypeElement configGroup) {
        configCollector.addUnresolvedConfigGroup(configGroup.getQualifiedName().toString());
    }

    @Override
    public void onResolvedEnum(TypeElement enumTypeElement) {
        Map<String, EnumConstant> enumConstants = new LinkedHashMap<>();

        for (Element enumElement : enumTypeElement.getEnclosedElements()) {
            if (enumElement.getKind() != ElementKind.ENUM_CONSTANT) {
                continue;
            }

            String rawJavadoc = utils.element().getJavadoc(enumElement);
            ParsedJavadoc parsedJavadoc = JavadocToAsciidocTransformer.INSTANCE.parseConfigItemJavadoc(rawJavadoc);

            String explicitValue = null;
            Map<String, AnnotationMirror> annotations = utils.element().getAnnotations(enumElement);
            AnnotationMirror configDocEnumValue = annotations.get(Types.ANNOTATION_CONFIG_DOC_ENUM_VALUE);
            if (configDocEnumValue != null) {
                Map<String, Object> enumValueValues = utils.element().getAnnotationValues(configDocEnumValue);
                explicitValue = (String) enumValueValues.get("value");
            }

            enumConstants.put(enumElement.getSimpleName().toString(),
                    new EnumConstant(explicitValue, parsedJavadoc.description(), parsedJavadoc.since(), rawJavadoc));
        }

        EnumDefinition enumDefinition = new EnumDefinition(enumTypeElement.getQualifiedName().toString(),
                enumConstants);
        configCollector.addResolvedEnum(enumDefinition);
    }

    @Override
    public void onUnresolvedEnum(TypeElement enumTypeElement) {
        Map<String, UnresolvedEnumConstant> enumConstants = new LinkedHashMap<>();

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

            enumConstants.put(enumElement.getSimpleName().toString(), new UnresolvedEnumConstant(explicitValue));
        }

        UnresolvedEnumDefinition unresolvedEnumDefinition = new UnresolvedEnumDefinition(
                enumTypeElement.getQualifiedName().toString(),
                enumConstants);

        configCollector.addUnresolvedEnum(unresolvedEnumDefinition);
    }
}
