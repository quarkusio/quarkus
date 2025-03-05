package io.quarkus.deployment.steps;

import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.LambdaCapturingTypeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class RegisterForReflectionBuildStep {

    @BuildStep
    public void build(CombinedIndexBuildItem combinedIndexBuildItem, Capabilities capabilities,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchy,
            BuildProducer<LambdaCapturingTypeBuildItem> lambdaCapturingTypeProducer) {

        Set<DotName> processedReflectiveHierarchies = new HashSet<DotName>();

        IndexView computingIndex = combinedIndexBuildItem.getComputingIndex();
        IndexView index = combinedIndexBuildItem.getIndex();

        for (AnnotationInstance i : index.getAnnotations(DotName.createSimple(RegisterForReflection.class.getName()))) {

            ClassInfo classInfo = i.target().asClass();
            String reason = "@" + RegisterForReflection.class.getSimpleName() + " on " + classInfo.name();

            // we use the computingIndex here as @RegisterForReflection is not in the index
            boolean methods = i.valueWithDefault(computingIndex, "methods").asBoolean();
            boolean fields = i.valueWithDefault(computingIndex, "fields").asBoolean();
            boolean ignoreNested = i.valueWithDefault(computingIndex, "ignoreNested").asBoolean();
            boolean serialization = i.valueWithDefault(computingIndex, "serialization").asBoolean();
            boolean unsafeAllocated = i.valueWithDefault(computingIndex, "unsafeAllocated").asBoolean();
            boolean registerFullHierarchy = i.valueWithDefault(computingIndex, "registerFullHierarchy").asBoolean();

            AnnotationValue targetsValue = i.value("targets");
            AnnotationValue classNamesValue = i.value("classNames");
            AnnotationValue lambdaCapturingTypesValue = i.value("lambdaCapturingTypes");

            if (lambdaCapturingTypesValue != null) {
                for (String lambdaCapturingType : lambdaCapturingTypesValue.asStringArray()) {
                    lambdaCapturingTypeProducer.produce(new LambdaCapturingTypeBuildItem(lambdaCapturingType));
                }
            }

            if (targetsValue == null && classNamesValue == null) {
                registerClass(reflectiveClass, reflectiveClassHierarchy, processedReflectiveHierarchies, computingIndex,
                        capabilities, reason, classInfo.name().toString(), methods, fields, ignoreNested, serialization,
                        unsafeAllocated, registerFullHierarchy);
                continue;
            }

            if (targetsValue != null) {
                Type[] targets = targetsValue.asClassArray();
                for (Type type : targets) {
                    registerClass(reflectiveClass, reflectiveClassHierarchy, processedReflectiveHierarchies, computingIndex,
                            capabilities, reason, type.name().toString(), methods, fields, ignoreNested, serialization,
                            unsafeAllocated, registerFullHierarchy);
                }
            }

            if (classNamesValue != null) {
                String[] classNames = classNamesValue.asStringArray();
                for (String className : classNames) {
                    registerClass(reflectiveClass, reflectiveClassHierarchy, processedReflectiveHierarchies, computingIndex,
                            capabilities, reason, className, methods, fields, ignoreNested, serialization, unsafeAllocated,
                            registerFullHierarchy);
                }
            }
        }
    }

    private void registerClass(final BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchy, Set<DotName> processedReflectiveHierarchies,
            IndexView computingIndex,
            Capabilities capabilities, String reason, String className, boolean methods, boolean fields,
            boolean ignoreNested,
            boolean serialization, boolean unsafeAllocated,
            boolean registerFullHierarchyValue) {
        if (registerFullHierarchyValue) {
            registerFullHierarchy(reflectiveClassHierarchy, reason, computingIndex, processedReflectiveHierarchies,
                    methods, fields, ignoreNested, serialization, unsafeAllocated, className);
            return;
        }

        reflectiveClass.produce(serialization
                ? ReflectiveClassBuildItem.builder(className).reason(reason).serialization().unsafeAllocated(unsafeAllocated)
                        .build()
                : ReflectiveClassBuildItem.builder(className).reason(reason).constructors().methods(methods).fields(fields)
                        .unsafeAllocated(unsafeAllocated).build());

        ClassInfo classInfo = computingIndex.getClassByName(className);

        if (ignoreNested && !isKotlinClass(capabilities, classInfo)) {
            return;
        }

        if (classInfo != null) {
            for (DotName memberClass : classInfo.memberClasses()) {
                registerClass(reflectiveClass, reflectiveClassHierarchy, processedReflectiveHierarchies, computingIndex,
                        capabilities, reason, memberClass.toString(), methods, fields, false, serialization, unsafeAllocated,
                        registerFullHierarchyValue);
            }
        }
    }

    private void registerFullHierarchy(BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchy,
            String reason, IndexView computingIndex, Set<DotName> processedReflectiveHierarchies, boolean methods,
            boolean fields, boolean ignoreNested, boolean serialization, boolean unsafeAllocated, String className) {
        DotName dotName = DotName.createSimple(className);

        if (processedReflectiveHierarchies.contains(dotName)
                || ReflectiveHierarchyBuildItem.DefaultIgnoreTypePredicate.INSTANCE.test(dotName)) {
            return;
        }

        processedReflectiveHierarchies.add(dotName);
        reflectiveClassHierarchy.produce(ReflectiveHierarchyBuildItem.builder(dotName)
                .index(computingIndex)
                .source(RegisterForReflectionBuildStep.class.getSimpleName())
                .ignoreMethodPredicate(methods ? ReflectiveHierarchyBuildItem.DefaultIgnoreMethodPredicate.INSTANCE : m -> true)
                .ignoreFieldPredicate(fields ? ReflectiveHierarchyBuildItem.DefaultIgnoreFieldPredicate.INSTANCE : m -> true)
                .methods(methods)
                .fields(fields)
                .ignoreNested(ignoreNested)
                .serialization(serialization)
                .unsafeAllocated(unsafeAllocated)
                .build());
    }

    private static boolean isKotlinClass(Capabilities capabilities, ClassInfo classInfo) {
        if (capabilities.isPresent(Capability.KOTLIN) && KotlinUtil.isKotlinClass(classInfo)) {
            return true;
        }

        return false;
    }
}
