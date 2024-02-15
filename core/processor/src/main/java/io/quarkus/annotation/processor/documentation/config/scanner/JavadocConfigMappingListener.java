package io.quarkus.annotation.processor.documentation.config.scanner;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryRootElement;
import io.quarkus.annotation.processor.documentation.config.discovery.ResolvedType;
import io.quarkus.annotation.processor.documentation.config.util.Markers;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Utils;

/**
 * This class is responsible for collecting and writing the Javadoc in quarkus-javadoc.properties.
 * We want this class to be replaced by the new descriptors that will be generated.
 */
@Deprecated(since = "3.14", forRemoval = true)
public class JavadocConfigMappingListener extends AbstractJavadocConfigListener {

    JavadocConfigMappingListener(Config config, Utils utils, ConfigCollector configCollector) {
        super(config, utils, configCollector);
    }

    @Override
    public void onEnclosedMethod(DiscoveryRootElement discoveryRootElement, TypeElement clazz, ExecutableElement method,
            ResolvedType resolvedType) {
        String docComment = utils.element().getRequiredJavadoc(method);
        configCollector.addJavadocProperty(
                clazz.getQualifiedName().toString() + Markers.DOT + method.getSimpleName().toString(),
                docComment);
    }
}
