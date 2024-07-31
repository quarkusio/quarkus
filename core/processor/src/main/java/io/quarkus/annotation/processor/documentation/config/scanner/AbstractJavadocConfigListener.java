package io.quarkus.annotation.processor.documentation.config.scanner;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadoc;
import io.quarkus.annotation.processor.documentation.config.formatter.JavadocToAsciidocTransformer;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
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
    public void onResolvedEnum(TypeElement enumTypeElement) {
        if (!utils.element().isLocalClass(enumTypeElement)) {
            return;
        }

        for (Element enumElement : enumTypeElement.getEnclosedElements()) {
            if (enumElement.getKind() != ElementKind.ENUM_CONSTANT) {
                continue;
            }

            String rawJavadoc = utils.element().getJavadoc(enumElement);
            if (rawJavadoc != null && !rawJavadoc.isBlank()) {
                ParsedJavadoc parsedJavadoc = JavadocToAsciidocTransformer.INSTANCE.parseConfigItemJavadoc(rawJavadoc);

                configCollector.addJavadocElement(
                        enumTypeElement.getQualifiedName().toString() + Markers.DOT + enumElement.getSimpleName()
                                .toString(),
                        new JavadocElement(parsedJavadoc.description(), parsedJavadoc.since(), rawJavadoc));
            }
        }
    }
}
