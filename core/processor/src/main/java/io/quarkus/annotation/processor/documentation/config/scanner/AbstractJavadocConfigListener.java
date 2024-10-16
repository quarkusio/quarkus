package io.quarkus.annotation.processor.documentation.config.scanner;

import java.util.Optional;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigRoot;
import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadoc;
import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadocSection;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
import io.quarkus.annotation.processor.documentation.config.util.JavadocUtil;
import io.quarkus.annotation.processor.documentation.config.util.Markers;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Utils;

public class AbstractJavadocConfigListener implements ConfigAnnotationListener {

    protected final Config config;
    protected final Utils utils;
    protected final ConfigCollector configCollector;

    protected AbstractJavadocConfigListener(Config config, Utils utils, ConfigCollector configCollector) {
        this.config = config;
        this.utils = utils;
        this.configCollector = configCollector;
    }

    @Override
    public Optional<DiscoveryConfigRoot> onConfigRoot(TypeElement configRoot) {
        // we only get Javadoc for local classes
        // classes coming from other modules won't have Javadoc available
        if (!utils.element().isLocalClass(configRoot)) {
            return Optional.empty();
        }

        Optional<String> rawJavadoc = utils.element().getJavadoc(configRoot);

        if (rawJavadoc.isEmpty()) {
            return Optional.empty();
        }

        ParsedJavadocSection parsedJavadocSection = JavadocUtil.parseConfigSectionJavadoc(rawJavadoc.get());
        if (parsedJavadocSection.title() == null) {
            return Optional.empty();
        }

        configCollector.addJavadocElement(
                configRoot.getQualifiedName().toString(),
                new JavadocElement(parsedJavadocSection.title(), parsedJavadocSection.format(), null,
                        parsedJavadocSection.deprecated()));

        return Optional.empty();
    }

    @Override
    public void onResolvedEnum(TypeElement enumTypeElement) {
        if (!utils.element().isLocalClass(enumTypeElement)) {
            return;
        }

        for (Element enumElement : enumTypeElement.getEnclosedElements()) {
            if (enumElement.getKind() != ElementKind.ENUM_CONSTANT) {
                continue;
            }

            Optional<String> rawJavadoc = utils.element().getJavadoc(enumElement);

            if (rawJavadoc.isEmpty()) {
                continue;
            }

            ParsedJavadoc parsedJavadoc = JavadocUtil.parseConfigItemJavadoc(rawJavadoc.get());

            if (parsedJavadoc.description() == null) {
                continue;
            }

            configCollector.addJavadocElement(
                    enumTypeElement.getQualifiedName().toString() + Markers.DOT + enumElement.getSimpleName()
                            .toString(),
                    new JavadocElement(parsedJavadoc.description(), parsedJavadoc.format(), parsedJavadoc.since(),
                            parsedJavadoc.deprecated()));
        }
    }
}
