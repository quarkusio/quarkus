package io.quarkus.deployment.steps;

import static io.quarkus.deployment.steps.KotlinUtil.isKotlinClass;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.LambdaCapturingTypeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem.Builder;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class RegisterForReflectionBuildStep {

    private static final Logger log = Logger.getLogger(RegisterForReflectionBuildStep.class);

    @BuildStep
    public void build(CombinedIndexBuildItem combinedIndexBuildItem, Capabilities capabilities,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchy,
            BuildProducer<LambdaCapturingTypeBuildItem> lambdaCapturingTypeProducer) {

        ReflectiveHierarchyBuildItem.Builder builder = new ReflectiveHierarchyBuildItem.Builder();
        Set<DotName> processedReflectiveHierarchies = new HashSet<DotName>();

        IndexView index = combinedIndexBuildItem.getComputingIndex();
        for (AnnotationInstance i : combinedIndexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(RegisterForReflection.class.getName()))) {

            boolean methods = i.valueWithDefault(index, "methods").asBoolean();
            boolean fields = i.valueWithDefault(index, "fields").asBoolean();
            boolean ignoreNested = i.valueWithDefault(index, "ignoreNested").asBoolean();
            boolean serialization = i.valueWithDefault(index, "serialization").asBoolean();
            boolean unsafeAllocated = i.valueWithDefault(index, "unsafeAllocated").asBoolean();
            boolean registerFullHierarchyValue = i.valueWithDefault(index, "registerFullHierarchy").asBoolean();

            AnnotationValue targetsValue = i.value("targets");
            AnnotationValue classNamesValue = i.value("classNames");
            AnnotationValue lambdaCapturingTypesValue = i.value("lambdaCapturingTypes");

            if (lambdaCapturingTypesValue != null) {
                for (String lambdaCapturingType : lambdaCapturingTypesValue.asStringArray()) {
                    lambdaCapturingTypeProducer.produce(new LambdaCapturingTypeBuildItem(lambdaCapturingType));
                }
            }

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (targetsValue == null && classNamesValue == null) {
                ClassInfo classInfo = i.target().asClass();
                if (capabilities.isPresent(Capability.KOTLIN) && ignoreNested) {
                    // for Kotlin classes, we need to register the nested classes as well because companion classes are very often necessary at runtime
                    if (isKotlinClass(classInfo)) {
                        ignoreNested = false;
                    }
                }
                registerClass(classLoader, classInfo.name().toString(), methods, fields, ignoreNested, serialization,
                        unsafeAllocated, reflectiveClass, reflectiveClassHierarchy, processedReflectiveHierarchies,
                        registerFullHierarchyValue,
                        builder);
                continue;
            }

            if (targetsValue != null) {
                Type[] targets = targetsValue.asClassArray();
                for (Type type : targets) {
                    registerClass(classLoader, type.name().toString(), methods, fields, ignoreNested, serialization,
                            unsafeAllocated, reflectiveClass, reflectiveClassHierarchy, processedReflectiveHierarchies,
                            registerFullHierarchyValue, builder);
                }
            }

            if (classNamesValue != null) {
                String[] classNames = classNamesValue.asStringArray();
                for (String className : classNames) {
                    registerClass(classLoader, className, methods, fields, ignoreNested, serialization, unsafeAllocated,
                            reflectiveClass,
                            reflectiveClassHierarchy, processedReflectiveHierarchies, registerFullHierarchyValue, builder);
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
    private void registerClass(ClassLoader classLoader, String className, boolean methods, boolean fields,
            boolean ignoreNested, boolean serialization, boolean unsafeAllocated,
            final BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchy, Set<DotName> processedReflectiveHierarchies,
            boolean registerFullHierarchyValue, Builder builder) {
        reflectiveClass.produce(serialization
                ? ReflectiveClassBuildItem.builder(className).serialization().unsafeAllocated(unsafeAllocated).build()
                : ReflectiveClassBuildItem.builder(className).constructors().methods(methods).fields(fields)
                        .unsafeAllocated(unsafeAllocated).build());

        //Search all class hierarchy, fields and methods in order to register its classes for reflection
        if (registerFullHierarchyValue) {
            registerClassDependencies(reflectiveClassHierarchy, classLoader, processedReflectiveHierarchies, methods, builder,
                    className);
        }

        if (ignoreNested) {
            return;
        }

        try {
            Class<?>[] declaredClasses = classLoader.loadClass(className).getDeclaredClasses();
            for (Class<?> clazz : declaredClasses) {
                registerClass(classLoader, clazz.getName(), methods, fields, false, serialization, unsafeAllocated,
                        reflectiveClass,
                        reflectiveClassHierarchy, processedReflectiveHierarchies, registerFullHierarchyValue, builder);
            }
        } catch (ClassNotFoundException e) {
            log.warnf(e, "Failed to load Class %s", className);
        }
    }

    private void registerClassDependencies(BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchy,
            ClassLoader classLoader, Set<DotName> processedReflectiveHierarchies, boolean methods,
            ReflectiveHierarchyBuildItem.Builder builder,
            String className) {
        try {
            DotName dotName = DotName.createSimple(className);
            if (!processedReflectiveHierarchies.contains(dotName)
                    && !ReflectiveHierarchyBuildItem.DefaultIgnoreTypePredicate.INSTANCE.test(dotName)) {

                processedReflectiveHierarchies.add(dotName);
                Index indexView = Index.of(classLoader.loadClass(className));
                reflectiveClassHierarchy.produce(builder
                        .type(org.jboss.jandex.Type
                                .create(dotName,
                                        org.jboss.jandex.Type.Kind.CLASS))
                        .index(indexView)
                        .build());

                ClassInfo classInfo = indexView.getClassByName(dotName);
                if (methods) {
                    addMethodsForReflection(reflectiveClassHierarchy, classLoader, processedReflectiveHierarchies,
                            indexView, dotName,
                            builder, classInfo, methods);
                }
                registerClassFields(reflectiveClassHierarchy, classLoader, processedReflectiveHierarchies,
                        indexView, dotName, builder, classInfo, methods);
            }
        } catch (ClassNotFoundException | IOException ignored) {
            log.error("Cannot load class for reflection: " + className);
        }
    }

    private void addMethodsForReflection(BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchy,
            ClassLoader classLoader, Set<DotName> processedReflectiveHierarchies, IndexView indexView, DotName initialName,
            ReflectiveHierarchyBuildItem.Builder builder, ClassInfo classInfo, boolean methods) {
        List<MethodInfo> methodList = classInfo.methods();
        for (MethodInfo methodInfo : methodList) {
            // we will only consider potential getters
            if (methodInfo.parameters().size() > 0 ||
                    Modifier.isStatic(methodInfo.flags()) ||
                    methodInfo.returnType().kind() == Kind.VOID || methodInfo.returnType().kind() == Kind.PRIMITIVE) {
                continue;
            }
            registerType(reflectiveClassHierarchy, classLoader, processedReflectiveHierarchies,
                    methods, builder,
                    getMethodReturnType(indexView, initialName, classInfo, methodInfo));
        }
    }

    private void registerClassFields(BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchy,
            ClassLoader classLoader, Set<DotName> processedReflectiveHierarchies, IndexView indexView, DotName initialName,
            ReflectiveHierarchyBuildItem.Builder builder, ClassInfo classInfo, boolean methods) {
        List<FieldInfo> fieldList = classInfo.fields();
        for (FieldInfo fieldInfo : fieldList) {
            if (Modifier.isStatic(fieldInfo.flags()) ||
                    fieldInfo.name().startsWith("this$") || fieldInfo.name().startsWith("val$")) {
                continue;
            }
            registerType(reflectiveClassHierarchy, classLoader, processedReflectiveHierarchies,
                    methods, builder,
                    fieldInfo.type());
        }
    }

    private void registerType(BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchy,
            ClassLoader classLoader, Set<DotName> processedReflectiveHierarchies, boolean methods,
            ReflectiveHierarchyBuildItem.Builder builder, Type type) {
        if (type.kind().equals(Kind.ARRAY)) {
            type = type.asArrayType().constituent();
        }
        if (type.kind() != Kind.PRIMITIVE && !processedReflectiveHierarchies.contains(type.name())) {
            registerClassDependencies(reflectiveClassHierarchy, classLoader, processedReflectiveHierarchies,
                    methods, builder, type.name().toString());
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
