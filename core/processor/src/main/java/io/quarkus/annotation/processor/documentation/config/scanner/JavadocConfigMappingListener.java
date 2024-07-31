package io.quarkus.annotation.processor.documentation.config.scanner;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryRootElement;
import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadoc;
import io.quarkus.annotation.processor.documentation.config.discovery.ResolvedType;
import io.quarkus.annotation.processor.documentation.config.formatter.JavadocToAsciidocTransformer;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
import io.quarkus.annotation.processor.documentation.config.util.Markers;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Utils;

/**
 * This class is responsible for collecting and writing the Javadoc.
 */
public class JavadocConfigMappingListener extends AbstractJavadocConfigListener {

    JavadocConfigMappingListener(Config config, Utils utils, ConfigCollector configCollector) {
        super(config, utils, configCollector);
    }

    @Override
    public void onEnclosedMethod(DiscoveryRootElement discoveryRootElement, TypeElement clazz, ExecutableElement method,
            ResolvedType resolvedType) {
        if (config.getExtension().isMixedModule() && !discoveryRootElement.isConfigMapping()) {
            return;
        }

        // we only get Javbadoc for local classes
        // classes coming from other modules won't have Javadoc available
        if (!utils.element().isLocalClass(clazz)) {
            return;
        }

        String rawJavadoc = utils.element().getRequiredJavadoc(method);
        if (rawJavadoc != null && !rawJavadoc.isBlank()) {
            ParsedJavadoc parsedJavadoc = JavadocToAsciidocTransformer.INSTANCE.parseConfigItemJavadoc(rawJavadoc);

            configCollector.addJavadocElement(
                    clazz.getQualifiedName().toString() + Markers.DOT + method.getSimpleName().toString(),
                    new JavadocElement(parsedJavadoc.description(), parsedJavadoc.since(), rawJavadoc));
        }
    }
}
