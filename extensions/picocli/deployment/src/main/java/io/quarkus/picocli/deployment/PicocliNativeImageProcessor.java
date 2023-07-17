package io.quarkus.picocli.deployment;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.AnnotationValue.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import picocli.CommandLine;

public class PicocliNativeImageProcessor {

    private static final Logger LOGGER = Logger.getLogger(PicocliNativeImageProcessor.class);

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
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
                DotName.createSimple(CommandLine.Parameters.class.getName()),
                DotName.createSimple(CommandLine.ParentCommand.class.getName()),
                DotName.createSimple(CommandLine.Spec.class.getName()),
                DotName.createSimple(CommandLine.Unmatched.class.getName()));

        Set<ClassInfo> foundClasses = new HashSet<>();
        Set<FieldInfo> foundFields = new HashSet<>();
        Set<Type> typeAnnotationValues = new HashSet<>();

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

                // register classes references in Picocli annotations for reflection
                List<AnnotationValue> values = ann.valuesWithDefaults(index);
                for (AnnotationValue value : values) {
                    if (value.kind() == Kind.CLASS) {
                        typeAnnotationValues.add(value.asClass());
                    } else if (value.kind() == Kind.ARRAY && value.componentKind() == Kind.CLASS) {
                        Collections.addAll(typeAnnotationValues, value.asClassArray());
                    }
                }
            }
        }

        foundClasses.forEach(classInfo -> {
            if (Modifier.isInterface(classInfo.flags())) {
                nativeImageProxies
                        .produce(new NativeImageProxyDefinitionBuildItem(classInfo.name().toString()));
                reflectiveClasses
                        .produce(ReflectiveClassBuildItem.builder(classInfo.name().toString()).constructors(false).methods()
                                .build());
            } else {
                reflectiveClasses
                        .produce(ReflectiveClassBuildItem.builder(classInfo.name().toString()).methods()
                                .build());
            }
        });
        foundFields.forEach(fieldInfo -> reflectiveFields.produce(new ReflectiveFieldBuildItem(fieldInfo)));
        typeAnnotationValues.forEach(type -> reflectiveHierarchies.produce(new ReflectiveHierarchyBuildItem.Builder()
                .type(type)
                .source(PicocliNativeImageProcessor.class.getSimpleName())
                .build()));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void resourceBundlesConfiguration(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundles) {
        combinedIndexBuildItem.getIndex().getAnnotations(DotName.createSimple(CommandLine.Command.class.getName()))
                .stream().map(ann -> ann.value("resourceBundle")).filter(Objects::nonNull)
                .map(AnnotationValue::asString).filter(stringVal -> stringVal != null && !stringVal.isEmpty())
                .collect(Collectors.toSet())
                .forEach(bundleName -> resourceBundles.produce(new NativeImageResourceBundleBuildItem(bundleName)));
    }

}
