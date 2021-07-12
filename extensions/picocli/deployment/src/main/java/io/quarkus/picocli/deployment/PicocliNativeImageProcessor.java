package io.quarkus.picocli.deployment;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import picocli.CommandLine;

public class PicocliNativeImageProcessor {

    private static final Logger LOGGER = Logger.getLogger(PicocliNativeImageProcessor.class);

    private static final DotName PARAMETERS = DotName.createSimple(CommandLine.Parameters.class.getName());
    private static final String PARAMETERS_COMPLETION_CANDIDATES = "completionCandidates";

    @BuildStep(onlyIf = NativeBuild.class)
    void reflectionConfiguration(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveFieldBuildItem> reflectiveFields,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies,
            BuildProducer<NativeImageProxyDefinitionBuildItem> nativeImageProxies) {
        IndexView index = combinedIndexBuildItem.getIndex();

        Collection<DotName> annotationsToAnalyze = Arrays.asList(
                DotName.createSimple(CommandLine.ArgGroup.class.getName()),
                DotName.createSimple(CommandLine.Command.class.getName()),
                DotName.createSimple(CommandLine.Mixin.class.getName()),
                DotName.createSimple(CommandLine.Option.class.getName()),
                PARAMETERS,
                DotName.createSimple(CommandLine.ParentCommand.class.getName()),
                DotName.createSimple(CommandLine.Spec.class.getName()),
                DotName.createSimple(CommandLine.Unmatched.class.getName()));

        Set<ClassInfo> foundClasses = new HashSet<>();
        Set<FieldInfo> foundFields = new HashSet<>();
        for (DotName analyzedAnnotation : annotationsToAnalyze) {
            for (AnnotationInstance ann : index.getAnnotations(analyzedAnnotation)) {
                AnnotationTarget target = ann.target();
                switch (target.kind()) {
                    case CLASS:
                        foundClasses.add(target.asClass());
                        break;
                    case FIELD:
                        foundFields.add(target.asField());
                        // This may be class which will be used as Mixin. We need to be sure that Picocli will be able
                        // to initialize those even if they are not beans.
                        foundClasses.add(target.asField().declaringClass());
                        break;
                    case METHOD:
                        foundClasses.add(target.asMethod().declaringClass());
                        break;
                    case METHOD_PARAMETER:
                        foundClasses.add(target.asMethodParameter().method().declaringClass());
                        break;
                    default:
                        LOGGER.warnf("Unsupported type %s annotated with %s", target.kind().name(), analyzedAnnotation);
                        break;
                }
            }
        }

        Arrays.asList(DotName.createSimple(CommandLine.IVersionProvider.class.getName()),
                DotName.createSimple(CommandLine.class.getName()))
                .forEach(interfaceName -> foundClasses.addAll(index.getAllKnownImplementors(interfaceName)));

        foundClasses.forEach(classInfo -> {
            if (Modifier.isInterface(classInfo.flags())) {
                nativeImageProxies
                        .produce(new NativeImageProxyDefinitionBuildItem(classInfo.name().toString()));
                reflectiveClasses
                        .produce(new ReflectiveClassBuildItem(false, true, false, classInfo.name().toString()));
            } else {
                reflectiveClasses
                        .produce(new ReflectiveClassBuildItem(true, true, false, classInfo.name().toString()));
            }
        });
        foundFields.forEach(fieldInfo -> reflectiveFields.produce(new ReflectiveFieldBuildItem(fieldInfo)));

        // register @Parameters(completionCandidates = ...) for reflection
        Collection<AnnotationInstance> parametersAnnotationInstances = index
                .getAnnotations(PARAMETERS);
        for (AnnotationInstance parametersAnnotationInstance : parametersAnnotationInstances) {
            AnnotationValue completionCandidates = parametersAnnotationInstance.value(PARAMETERS_COMPLETION_CANDIDATES);

            if (completionCandidates == null) {
                continue;
            }

            reflectiveHierarchies.produce(new ReflectiveHierarchyBuildItem.Builder()
                    .type(completionCandidates.asClass())
                    .source(PicocliNativeImageProcessor.class.getSimpleName() + " > " + parametersAnnotationInstance.target())
                    .build());
        }
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void resourceBundlesConfiguration(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundles) {
        combinedIndexBuildItem.getIndex().getAnnotations(DotName.createSimple(CommandLine.Command.class.getName()))
                .stream().map(ann -> ann.value("resourceBundle")).filter(Objects::nonNull)
                .map(AnnotationValue::asString).filter(stringVal -> stringVal != null && !stringVal.isEmpty())
                .collect(Collectors.toSet())
                .forEach(bundleName -> resourceBundles.produce(new NativeImageResourceBundleBuildItem(bundleName)));
    }

}
