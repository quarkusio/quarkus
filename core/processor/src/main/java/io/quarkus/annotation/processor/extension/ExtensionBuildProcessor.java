package io.quarkus.annotation.processor.extension;

import static javax.lang.model.util.ElementFilter.methodsIn;
import static javax.lang.model.util.ElementFilter.typesIn;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

import io.quarkus.annotation.processor.ExtensionProcessor;
import io.quarkus.annotation.processor.Outputs;
import io.quarkus.annotation.processor.documentation.config.util.Types;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Utils;

public class ExtensionBuildProcessor implements ExtensionProcessor {

    private Utils utils;

    private final Set<String> processorClassNames = new TreeSet<>();
    private final Set<String> recorderClassNames = new TreeSet<>();
    private final Set<String> configRootClassNames = new TreeSet<>();
    private final Map<String, Boolean> annotationUsageTracker = new ConcurrentHashMap<>();

    @Override
    public void init(Config config, Utils utils) {
        this.utils = utils;
    }

    @Override
    public void process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            switch (annotation.getQualifiedName().toString()) {
                case Types.ANNOTATION_BUILD_STEP:
                    trackAnnotationUsed(Types.ANNOTATION_BUILD_STEP);
                    processBuildStep(roundEnv, annotation);
                    break;
                case Types.ANNOTATION_RECORDER:
                    trackAnnotationUsed(Types.ANNOTATION_RECORDER);
                    processRecorder(roundEnv, annotation);
                    break;
                case Types.ANNOTATION_CONFIG_ROOT:
                    trackAnnotationUsed(Types.ANNOTATION_CONFIG_ROOT);
                    processConfigRoot(roundEnv, annotation);
                    break;
                case Types.ANNOTATION_CONFIG_GROUP:
                    trackAnnotationUsed(Types.ANNOTATION_CONFIG_GROUP);
                    processConfigGroup(roundEnv, annotation);
                    break;
            }
        }
    }

    @Override
    public void finalizeProcessing() {
        validateAnnotationUsage();

        /*
         * During an incremental compilation (i.e. while developing extensions in Intellij IDEA)
         * the Annotation Processor API will include only changed classes
         * creating a subset of processors that are not enough to run a quarkus app
         * By assuming a full compilation was made initially, all the processors are included inside the
         * META-INF/quarkus-build-steps.list file
         * So by reading it we can ensure that all the processors are included.
         * See
         * https://youtrack.jetbrains.com/issue/IJPL-196660/During-an-incremental-build-getElementsAnnotatedWith-doesnt-include-
         * all-the-elements-but-only-the-one-recompiled
         */
        Set<String> allProcessorClassNames = new TreeSet<>(processorClassNames);
        allProcessorClassNames.addAll(utils.filer().readSet(Outputs.META_INF_QUARKUS_BUILD_STEPS));
        utils.filer().writeSet(Outputs.META_INF_QUARKUS_BUILD_STEPS, allProcessorClassNames);

        utils.filer().writeSet(Outputs.META_INF_QUARKUS_CONFIG_ROOTS, configRootClassNames);
    }

    private void processBuildStep(RoundEnvironment roundEnv, TypeElement annotation) {
        for (ExecutableElement buildStep : methodsIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            final TypeElement clazz = utils.element().getClassOf(buildStep);
            if (clazz == null) {
                continue;
            }

            final PackageElement pkg = utils.element().getPackageOf(clazz);
            if (pkg == null) {
                utils.processingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Element " + clazz + " has no enclosing package");
                continue;
            }

            final String binaryName = utils.element().getBinaryName(clazz);
            if (processorClassNames.add(binaryName)) {
                validateRecordBuildSteps(clazz);
                utils.accessorGenerator().generateAccessor(clazz);
            }
        }
    }

    private void validateRecordBuildSteps(TypeElement clazz) {
        for (Element e : clazz.getEnclosedElements()) {
            if (e.getKind() != ElementKind.METHOD) {
                continue;
            }
            ExecutableElement ex = (ExecutableElement) e;
            if (!utils.element().isAnnotationPresent(ex, Types.ANNOTATION_BUILD_STEP)) {
                continue;
            }
            if (!utils.element().isAnnotationPresent(ex, Types.ANNOTATION_RECORD)) {
                continue;
            }

            boolean hasRecorder = false;
            boolean allTypesResolvable = true;
            for (VariableElement parameter : ex.getParameters()) {
                String parameterClassName = parameter.asType().toString();
                TypeElement parameterTypeElement = utils.processingEnv().getElementUtils().getTypeElement(parameterClassName);
                if (parameterTypeElement == null) {
                    allTypesResolvable = false;
                } else {
                    if (utils.element().isAnnotationPresent(parameterTypeElement, Types.ANNOTATION_RECORDER)) {
                        if (parameterTypeElement.getModifiers().contains(Modifier.FINAL)) {
                            utils.processingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR,
                                    "Class '" + parameterTypeElement.getQualifiedName()
                                            + "' is annotated with @Recorder and therefore cannot be made as a final class.");
                        } else if (utils.element().getPackageName(clazz)
                                .equals(utils.element().getPackageName(parameterTypeElement))) {
                            utils.processingEnv().getMessager().printMessage(Diagnostic.Kind.WARNING,
                                    "Build step class '" + clazz.getQualifiedName()
                                            + "' and recorder '" + parameterTypeElement
                                            + "' share the same package. This is highly discouraged as it can lead to unexpected results.");
                        }
                        hasRecorder = true;
                        break;
                    }
                }
            }

            if (!hasRecorder && allTypesResolvable) {
                utils.processingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, "Build Step '"
                        + clazz.getQualifiedName() + "#"
                        + ex.getSimpleName()
                        + "' which is annotated with '@Record' does not contain a method parameter whose type is annotated with '@Recorder'.");
            }
        }
    }

    private void processRecorder(RoundEnvironment roundEnv, TypeElement annotation) {
        for (TypeElement recorder : typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            if (recorderClassNames.add(recorder.getQualifiedName().toString())) {
                utils.accessorGenerator().generateAccessor(recorder);
            }
        }
    }

    private void processConfigRoot(RoundEnvironment roundEnv, TypeElement annotation) {
        for (TypeElement configRoot : typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            configRootClassNames.add(utils.element().getBinaryName(configRoot));
        }
    }

    private void processConfigGroup(RoundEnvironment roundEnv, TypeElement annotation) {
        for (TypeElement configGroup : typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            // do nothing for now
        }
    }

    private void validateAnnotationUsage() {
        if (isAnnotationUsed(Types.ANNOTATION_BUILD_STEP) && isAnnotationUsed(Types.ANNOTATION_RECORDER)) {
            utils.processingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Detected use of @Recorder annotation in 'deployment' module. Classes annotated with @Recorder must be part of the extension's 'runtime' module");
        }
    }

    private boolean isAnnotationUsed(String annotation) {
        return annotationUsageTracker.getOrDefault(annotation, false);
    }

    private void trackAnnotationUsed(String annotation) {
        annotationUsageTracker.put(annotation, true);
    }
}
