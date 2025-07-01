package io.quarkus.annotation.processor.documentation.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import io.quarkus.annotation.processor.ExtensionProcessor;
import io.quarkus.annotation.processor.Outputs;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
import io.quarkus.annotation.processor.documentation.config.model.ResolvedModel;
import io.quarkus.annotation.processor.documentation.config.resolver.ConfigResolver;
import io.quarkus.annotation.processor.documentation.config.scanner.ConfigAnnotationScanner;
import io.quarkus.annotation.processor.documentation.config.scanner.ConfigCollector;
import io.quarkus.annotation.processor.documentation.config.util.Types;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Utils;

public class ConfigDocExtensionProcessor implements ExtensionProcessor {

    private Config config;
    private Utils utils;
    private ConfigAnnotationScanner configAnnotationScanner;

    @Override
    public void init(Config config, Utils utils) {
        this.config = config;
        this.utils = utils;
        this.configAnnotationScanner = new ConfigAnnotationScanner(config, utils);
    }

    @Override
    public void process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Optional<TypeElement> configGroup = findAnnotation(annotations, Types.ANNOTATION_CONFIG_GROUP);
        Optional<TypeElement> configRoot = findAnnotation(annotations, Types.ANNOTATION_CONFIG_ROOT);
        Optional<TypeElement> configMapping = findAnnotation(annotations, Types.ANNOTATION_CONFIG_MAPPING);

        // make sure we scan the groups before the root
        if (configGroup.isPresent()) {
            configAnnotationScanner.scanConfigGroups(roundEnv, configGroup.get());
        }
        if (configRoot.isPresent()) {
            configAnnotationScanner.scanConfigRoots(roundEnv, configRoot.get());
        }
        if (configMapping.isPresent()) {
            configAnnotationScanner.scanConfigMappingsWithoutConfigRoot(roundEnv, configMapping.get());
        }
    }

    private Optional<TypeElement> findAnnotation(Set<? extends TypeElement> annotations, String annotationName) {
        for (TypeElement annotation : annotations) {
            if (annotationName.equals(annotation.getQualifiedName().toString())) {
                return Optional.of(annotation);
            }
        }

        return Optional.empty();
    }

    @Override
    public void finalizeProcessing() {
        ConfigCollector configCollector = configAnnotationScanner.finalizeProcessing();

        // TODO radcortez drop this once we don't need them anymore
        // we will still need to read the quarkus-javadoc.properties in the Dev UI for now to allow for extensions built with older Quarkus versions
        Properties javadocProperties = new Properties();
        for (Entry<String, JavadocElement> javadocElementEntry : configCollector.getJavadocElements().entrySet()) {
            if (javadocElementEntry.getValue().description() == null
                    || javadocElementEntry.getValue().description().isBlank()) {
                continue;
            }

            javadocProperties.put(javadocElementEntry.getKey(), javadocElementEntry.getValue().description());
        }
        utils.filer().write(Outputs.META_INF_QUARKUS_JAVADOC, javadocProperties);

        ConfigResolver configResolver = new ConfigResolver(config, utils, configCollector);

        // the model is not written in the jar file
        JavadocElements javadocElements = configResolver.resolveJavadoc();
        if (!javadocElements.isEmpty()) {
            utils.filer().writeModel(Outputs.QUARKUS_CONFIG_DOC_JAVADOC, javadocElements);
        }

        ResolvedModel resolvedModel = configResolver.resolveModel();
        if (!resolvedModel.isEmpty()) {
            Path resolvedModelPath = utils.filer().writeModel(Outputs.QUARKUS_CONFIG_DOC_MODEL, resolvedModel);

            if (config.isDebug()) {
                try {
                    utils.processingEnv().getMessager().printMessage(Kind.NOTE,
                            "Resolved model:\n\n" + Files.readString(resolvedModelPath));
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to read the resolved model from: " + resolvedModelPath, e);
                }
            }
        }

        // Generate files that will be stored in the jar and can be consumed freely.
        // We generate JSON files to avoid requiring an additional dependency to the YAML mapper
        if (!javadocElements.isEmpty()) {
            utils.filer().writeJson(Outputs.META_INF_QUARKUS_CONFIG_JAVADOC_JSON, javadocElements);
        }
        if (!resolvedModel.isEmpty()) {
            utils.filer().writeJson(Outputs.META_INF_QUARKUS_CONFIG_MODEL_JSON, resolvedModel);
        }
        if (!javadocElements.isEmpty() || !resolvedModel.isEmpty()) {
            utils.filer().write(Outputs.META_INF_QUARKUS_CONFIG_MODEL_VERSION, "1");
        }
    }
}
