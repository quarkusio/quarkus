package io.quarkus.annotation.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

import org.jboss.jdeparser.JDeparser;

import io.quarkus.annotation.processor.documentation.config.ConfigDocExtensionProcessor;
import io.quarkus.annotation.processor.documentation.config.model.ExtensionModule;
import io.quarkus.annotation.processor.documentation.config.util.Types;
import io.quarkus.annotation.processor.extension.ExtensionBuildProcessor;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Utils;

@SupportedOptions({ Options.LEGACY_CONFIG_ROOT, Options.GENERATE_DOC, Options.SPLIT_ON_CONFIG_ROOT_DESCRIPTION })
public class ExtensionAnnotationProcessor extends AbstractProcessor {

    private static final String DEBUG = "debug-extension-annotation-processor";

    private Utils utils;
    private List<ExtensionProcessor> extensionProcessors;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        utils = new Utils(processingEnv);

        boolean useConfigMapping = !Boolean
                .parseBoolean(utils.processingEnv().getOptions().getOrDefault(Options.LEGACY_CONFIG_ROOT, "false"));

        if (!useConfigMapping) {
            throw new IllegalArgumentException(
                    "Starting with Quarkus 3.25, legacy config classes (deprecated since Quarkus 3.19) are not supported anymore. "
                            + "Please migrate the configuration of your extension to interfaces annotated with @ConfigMapping. See https://quarkus.io/guides/config-mappings#config-mappings for more information.");
        }

        boolean debug = Boolean.getBoolean(DEBUG);

        ExtensionModule extensionModule = utils.extension().getExtensionModule();

        Config config = new Config(extensionModule, useConfigMapping, debug);

        if (!useConfigMapping) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Extension module " + extensionModule.artifactId()
                    + " config implementation is deprecated. Please migrate to use @ConfigMapping: https://quarkus.io/guides/writing-extensions#configuration");
        }

        List<ExtensionProcessor> extensionProcessors = new ArrayList<>();
        extensionProcessors.add(new ExtensionBuildProcessor());

        boolean skipDocs = Boolean.getBoolean("skipDocs") || Boolean.getBoolean("quickly");
        boolean generateDoc = !skipDocs && !"false".equals(processingEnv.getOptions().get(Options.GENERATE_DOC));

        // for now, we generate the old config doc by default but we will change this behavior soon
        if (generateDoc) {
            if (extensionModule.detected()) {
                extensionProcessors.add(new ConfigDocExtensionProcessor());
            } else {
                processingEnv.getMessager().printMessage(Kind.WARNING,
                        "We could not detect the groupId and artifactId of this module (maybe you are using Gradle to build your extension?). The generation of the configuration documentation has been disabled.");
            }
        }

        this.extensionProcessors = Collections.unmodifiableList(extensionProcessors);

        for (ExtensionProcessor extensionProcessor : this.extensionProcessors) {
            extensionProcessor.init(config, utils);
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Types.SUPPORTED_ANNOTATIONS_TYPES;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member,
            String userText) {
        return Collections.emptySet();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (ExtensionProcessor extensionProcessor : extensionProcessors) {
                extensionProcessor.process(annotations, roundEnv);
            }

            if (roundEnv.processingOver()) {
                for (ExtensionProcessor extensionProcessor : extensionProcessors) {
                    extensionProcessor.finalizeProcessing();
                }
            }
            return true;
        } finally {
            JDeparser.dropCaches();
        }
    }
}
