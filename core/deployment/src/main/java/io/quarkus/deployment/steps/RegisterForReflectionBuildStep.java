package io.quarkus.deployment.steps;

import static io.quarkus.deployment.steps.KotlinUtil.isKotlinClass;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.LambdaCapturingTypeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.util.JandexUtil;
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
            boolean registerFullHierarchyValue = i.valueWithDefault(computingIndex, "registerFullHierarchy").asBoolean();

            AnnotationValue targetsValue = i.value("targets");
            AnnotationValue classNamesValue = i.value("classNames");
            AnnotationValue lambdaCapturingTypesValue = i.value("lambdaCapturingTypes");

            if (lambdaCapturingTypesValue != null) {
                for (String lambdaCapturingType : lambdaCapturingTypesValue.asStringArray()) {
                    lambdaCapturingTypeProducer.produce(new LambdaCapturingTypeBuildItem(lambdaCapturingType));
                }
            }

            if (targetsValue == null && classNamesValue == null) {
                if (capabilities.isPresent(Capability.KOTLIN) && ignoreNested) {
                    // for Kotlin classes, we need to register the nested classes as well because companion classes are very often necessary at runtime
                    if (isKotlinClass(classInfo)) {
                        ignoreNested = false;
                    }
                }
                registerClass(computingIndex, reason, classInfo.name().toString(), methods, fields, ignoreNested,
                        serialization, unsafeAllocated, reflectiveClass, reflectiveClassHierarchy,
                        processedReflectiveHierarchies, registerFullHierarchyValue);
                continue;
            }

            if (targetsValue != null) {
                Type[] targets = targetsValue.asClassArray();
                for (Type type : targets) {
                    registerClass(computingIndex, reason, type.name().toString(), methods, fields, ignoreNested,
                            serialization, unsafeAllocated, reflectiveClass, reflectiveClassHierarchy,
                            processedReflectiveHierarchies, registerFullHierarchyValue);
                }
            }

            if (classNamesValue != null) {
                String[] classNames = classNamesValue.asStringArray();
                for (String className : classNames) {
                    registerClass(computingIndex, reason, className, methods, fields, ignoreNested, serialization,
                            unsafeAllocated, reflectiveClass, reflectiveClassHierarchy, processedReflectiveHierarchies,
                            registerFullHierarchyValue);
                }
            }
        }
    }

    /**
     * BFS Recursive Method to register a class and its inner classes for Reflection.
     *
     * @param unsafeAllocated
     * @param reflectiveClassHierarchy
     * @param processedReflectiveHierarchies
     * @param registerFullHierarchyValue
     * @param builder
     */
    private void registerClass(IndexView computingIndex, String reason, String className,
            boolean methods, boolean fields, boolean ignoreNested, boolean serialization, boolean unsafeAllocated,
            final BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchy, Set<DotName> processedReflectiveHierarchies,
            boolean registerFullHierarchyValue) {
        reflectiveClass.produce(serialization
                ? ReflectiveClassBuildItem.builder(className).reason(reason).serialization().unsafeAllocated(unsafeAllocated)
                        .build()
                : ReflectiveClassBuildItem.builder(className).reason(reason).constructors().methods(methods).fields(fields)
                        .unsafeAllocated(unsafeAllocated).build());

        //Search all class hierarchy, fields and methods in order to register its classes for reflection
        if (registerFullHierarchyValue) {
            registerClassDependencies(reflectiveClassHierarchy, computingIndex, processedReflectiveHierarchies,
                    methods, className);
        }

        if (ignoreNested) {
            return;
        }

        ClassInfo classInfo = computingIndex.getClassByName(className);
        if (classInfo != null) {
            for (DotName memberClass : classInfo.memberClasses()) {
                registerClass(computingIndex, reason, memberClass.toString(), methods, fields, false, serialization,
                        unsafeAllocated, reflectiveClass, reflectiveClassHierarchy, processedReflectiveHierarchies,
                        registerFullHierarchyValue);
            }
        }
    }

    private void registerClassDependencies(BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchy,
            IndexView computingIndex, Set<DotName> processedReflectiveHierarchies, boolean methods,
            String className) {
        DotName dotName = DotName.createSimple(className);
        if (!processedReflectiveHierarchies.contains(dotName)
                && !ReflectiveHierarchyBuildItem.DefaultIgnoreTypePredicate.INSTANCE.test(dotName)) {

            processedReflectiveHierarchies.add(dotName);
            reflectiveClassHierarchy.produce(ReflectiveHierarchyBuildItem.builder(dotName)
                    .index(computingIndex)
                    .source(RegisterForReflectionBuildStep.class.getSimpleName())
                    .build());

            ClassInfo classInfo = computingIndex.getClassByName(dotName);
            if (methods) {
                addMethodsForReflection(reflectiveClassHierarchy, computingIndex, processedReflectiveHierarchies,
                        computingIndex, dotName, classInfo, methods);
            }
            registerClassFields(reflectiveClassHierarchy, computingIndex, processedReflectiveHierarchies,
                    computingIndex, dotName, classInfo, methods);
        }
    }

    private void addMethodsForReflection(BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchy,
            IndexView computingIndex, Set<DotName> processedReflectiveHierarchies, IndexView indexView,
            DotName initialName,
            ClassInfo classInfo, boolean methods) {
        List<MethodInfo> methodList = classInfo.methods();
        for (MethodInfo methodInfo : methodList) {
            // we will only consider potential getters
            if (methodInfo.parameters().size() > 0 ||
                    Modifier.isStatic(methodInfo.flags()) ||
                    methodInfo.returnType().kind() == Kind.VOID || methodInfo.returnType().kind() == Kind.PRIMITIVE) {
                continue;
            }
            registerType(reflectiveClassHierarchy, computingIndex, processedReflectiveHierarchies,
                    methods,
                    getMethodReturnType(indexView, initialName, classInfo, methodInfo));
        }
    }

    private void registerClassFields(BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchy,
            IndexView computingIndex, Set<DotName> processedReflectiveHierarchies, IndexView indexView,
            DotName initialName, ClassInfo classInfo, boolean methods) {
        List<FieldInfo> fieldList = classInfo.fields();
        for (FieldInfo fieldInfo : fieldList) {
            if (Modifier.isStatic(fieldInfo.flags()) ||
                    fieldInfo.name().startsWith("this$") || fieldInfo.name().startsWith("val$")) {
                continue;
            }
            registerType(reflectiveClassHierarchy, computingIndex, processedReflectiveHierarchies,
                    methods, fieldInfo.type());
        }
    }

    private void registerType(BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchy,
            IndexView computingIndex, Set<DotName> processedReflectiveHierarchies, boolean methods,
            Type type) {
        if (type.kind().equals(Kind.ARRAY)) {
            type = type.asArrayType().constituent();
        }
        if (type.kind() != Kind.PRIMITIVE && !processedReflectiveHierarchies.contains(type.name())) {
            registerClassDependencies(reflectiveClassHierarchy, computingIndex, processedReflectiveHierarchies,
                    methods, type.name().toString());
        }
    }

    private static Type getMethodReturnType(IndexView indexView, DotName initialName, ClassInfo info, MethodInfo method) {
        Type methodReturnType = method.returnType();
        if ((methodReturnType.kind() == Kind.TYPE_VARIABLE) && (info.typeParameters().size() == 1) &&
                methodReturnType.asTypeVariable().identifier().equals(info.typeParameters().get(0).identifier())) {
            List<Type> types = JandexUtil.resolveTypeParameters(initialName, info.name(), indexView);
            methodReturnType = types.get(0);
        }
        return methodReturnType;
    }

}
