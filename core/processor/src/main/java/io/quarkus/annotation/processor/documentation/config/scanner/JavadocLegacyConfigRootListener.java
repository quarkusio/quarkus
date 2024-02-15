package io.quarkus.annotation.processor.documentation.config.scanner;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

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
public class JavadocLegacyConfigRootListener extends AbstractJavadocConfigListener {

    JavadocLegacyConfigRootListener(Config config, Utils utils, ConfigCollector configCollector) {
        super(config, utils, configCollector);
    }

    @Override
    public void onEnclosedField(DiscoveryRootElement discoveryRootElement, TypeElement clazz, VariableElement field,
            ResolvedType resolvedType) {
        configCollector.addJavadocProperty(clazz.getQualifiedName().toString() + Markers.DOT + field.getSimpleName()
                .toString(), utils.element().getRequiredJavadoc(field));
    }
}
