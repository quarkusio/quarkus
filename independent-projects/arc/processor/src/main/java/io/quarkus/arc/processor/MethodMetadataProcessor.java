package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;

import io.quarkus.arc.MethodMetadata;
import io.quarkus.arc.ParameterMetadata;
import io.quarkus.arc.impl.ComputingCache;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 * Handles generating bytecode for {@link MethodMetadata} and {@link ParameterMetadata} implementations.
 * The {@link #createMethodMetadata(BytecodeCreator, MethodInfo) createMethodMetadata()} method can be used
 * to generate a bytecode sequence for instantiating a {@code MethodMetadata} for given method. That
 * bytecode sequence necessarily also instantiates {@code ParameterMetadata} for all parameters.
 * <p>
 * Behind the scenes, for each {@code MethodMetadata} and {@code ParameterMetadata} instance, their classes
 * are also generated. The generated classes are shared. That is, one class is generated for each shape
 * of method (that is, number of parameters and number of annotations) and for each shape of parameter
 * (that is, number of annotations).
 * <p>
 * This construct is thread-safe.
 */
public class MethodMetadataProcessor {
    private final ComputingCache<MethodMetadataShape, MethodMetadataImplClass> methodsToGenerate;
    private final ComputingCache<ParameterMetadataShape, ParameterMetadataImplClass> parametersToGenerate;

    private final IndexView beanArchiveIndex;
    private final AnnotationLiteralProcessor annotationLiterals;
    private final AnnotationStore annotationStore;

    MethodMetadataProcessor(IndexView beanArchiveIndex, AnnotationLiteralProcessor annotationLiterals,
            AnnotationStore annotationStore, Predicate<DotName> applicationClassPredicate) {
        this.methodsToGenerate = new ComputingCache<>(key -> {
            String generatedClassName = generateMethodMetadataClassName(key);
            return new MethodMetadataImplClass(key, generatedClassName,
                    applicationClassPredicate.test(DotName.createSimple(generatedClassName)));
        });
        this.parametersToGenerate = new ComputingCache<>(key -> {
            String generatedClassName = generateParameterMetadataClassName(key);
            return new ParameterMetadataImplClass(key, generatedClassName,
                    applicationClassPredicate.test(DotName.createSimple(generatedClassName)));
        });

        this.beanArchiveIndex = beanArchiveIndex;
        this.annotationLiterals = annotationLiterals;
        this.annotationStore = annotationStore;
    }

    boolean hasClassesToGenerate() {
        return !methodsToGenerate.isEmpty() || !parametersToGenerate.isEmpty();
    }

    ComputingCache<MethodMetadataShape, MethodMetadataImplClass> getMethodsToGenerate() {
        return methodsToGenerate;
    }

    ComputingCache<ParameterMetadataShape, ParameterMetadataImplClass> getParametersToGenerate() {
        return parametersToGenerate;
    }

    /**
     * Generates a bytecode sequence to obtain an instance of {@link MethodMetadata} that represents
     * given {@code method}. For that purpose, a class implementing {@code MethodMetadata} is generated.
     *
     * @param bytecode will receive the bytecode sequence for obtaining the {@code MethodMetadata} object
     *        as a sequence of {@link BytecodeCreator} method calls
     * @param method method for which a {@code MethodMetadata} class should be generated
     * @return a handle to the {@code MethodMetadata} object
     */
    public ResultHandle createMethodMetadata(BytecodeCreator bytecode, MethodInfo method) {
        Objects.requireNonNull(method);

        List<AnnotationInstance> annotations = annotationStore.getAnnotations(method)
                .stream()
                .filter(it -> it.target().kind() == AnnotationTarget.Kind.METHOD)
                .collect(Collectors.toList());
        int annotationsCount = annotations.size();
        int parametersCount = method.parametersCount();
        MethodMetadataShape shape = new MethodMetadataShape(parametersCount, annotationsCount);
        MethodMetadataImplClass methodMetadataClass = methodsToGenerate.getValue(shape);

        Class<?>[] constructorParameterTypes = methodMetadataClass.constructorParameterTypes();

        ResultHandle[] constructorArguments = new ResultHandle[6 + methodMetadataClass.parametersSlots
                + methodMetadataClass.annotationsSlots];
        constructorArguments[0] = bytecode.load(Methods.INIT.equals(method.name()));
        constructorArguments[1] = bytecode.load(Modifier.isStatic(method.flags()));
        constructorArguments[2] = bytecode.load(Methods.INIT.equals(method.name())
                ? method.declaringClass().name().toString()
                : method.name());
        constructorArguments[3] = bytecode.load((int) method.flags());
        constructorArguments[4] = bytecode.loadClass(method.declaringClass());
        constructorArguments[5] = bytecode.loadClass(Methods.INIT.equals(method.name())
                ? method.declaringClass().name().toString()
                : method.returnType().name().toString());
        if (methodMetadataClass.parametersUnfolded) {
            for (int i = 0; i < parametersCount; i++) {
                constructorArguments[6 + i] = createParameterMetadata(bytecode, method.parameters().get(i));
            }
        } else {
            ResultHandle array = bytecode.newArray(ParameterMetadata.class, parametersCount);
            for (int i = 0; i < parametersCount; i++) {
                ResultHandle parameterMetadata = createParameterMetadata(bytecode, method.parameters().get(i));
                bytecode.writeArrayValue(array, i, parameterMetadata);
            }
            constructorArguments[6] = array;
        }
        if (methodMetadataClass.annotationsUnfolded) {
            for (int i = 0; i < annotationsCount; i++) {
                AnnotationInstance annotation = annotations.get(i);
                ClassInfo annotationClass = beanArchiveIndex.getClassByName(annotation.name());
                constructorArguments[6 + methodMetadataClass.parametersSlots + i] = annotationLiterals.create(bytecode,
                        annotationClass, annotation);
            }
        } else {
            ResultHandle array = bytecode.newArray(Map.Entry.class, annotationsCount);
            for (int i = 0; i < annotationsCount; i++) {
                AnnotationInstance annotation = annotations.get(i);
                ClassInfo annotationClass = beanArchiveIndex.getClassByName(annotation.name());
                ResultHandle annotationClassHandle = bytecode.loadClass(annotationClass);
                ResultHandle annotationHandle = annotationLiterals.create(bytecode, annotationClass, annotation);
                ResultHandle entry = bytecode.invokeStaticInterfaceMethod(MethodDescriptors.MAP_ENTRY, annotationClassHandle,
                        annotationHandle);
                bytecode.writeArrayValue(array, i, entry);
            }
            constructorArguments[6 + methodMetadataClass.parametersSlots] = bytecode
                    .invokeStaticInterfaceMethod(MethodDescriptors.MAP_OF_ENTRIES, array);
        }

        return bytecode.newInstance(
                MethodDescriptor.ofConstructor(methodMetadataClass.generatedClassName, (Object[]) constructorParameterTypes),
                constructorArguments);
    }

    /**
     * Generates a bytecode sequence to obtain an instance of {@link ParameterMetadata} that represents
     * given {@code parameter}. For that purpose, a class implementing {@code ParameterMetadata} is generated.
     *
     * @param bytecode will receive the bytecode sequence for obtaining the {@code ParameterMetadata} object
     *        as a sequence of {@link BytecodeCreator} method calls
     * @param parameter method parameter for which a {@code ParameterMetadata} class should be generated
     * @return a handle to the {@code ParameterMetadata} object
     */
    private ResultHandle createParameterMetadata(BytecodeCreator bytecode, MethodParameterInfo parameter) {
        Objects.requireNonNull(parameter);

        List<AnnotationInstance> annotations = annotationStore.getAnnotations(parameter.method())
                .stream()
                .filter(it -> it.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER
                        && it.target().asMethodParameter().position() == parameter.position())
                .collect(Collectors.toList());
        int annotationsCount = annotations.size();
        ParameterMetadataShape shape = new ParameterMetadataShape(annotationsCount);
        ParameterMetadataImplClass parameterMetadataClass = parametersToGenerate.getValue(shape);

        Class<?>[] constructorParameterTypes = parameterMetadataClass.constructorParameterTypes();

        String parameterName = parameter.name();
        if (parameterName == null) {
            parameterName = "arg" + parameter.position();
        }

        ResultHandle[] constructorArguments = new ResultHandle[2 + parameterMetadataClass.annotationsSlots];
        constructorArguments[0] = bytecode.load(parameterName);
        constructorArguments[1] = bytecode.loadClass(parameter.type().name().toString());
        if (parameterMetadataClass.annotationsUnfolded) {
            for (int i = 0; i < annotationsCount; i++) {
                AnnotationInstance annotation = annotations.get(i);
                ClassInfo annotationClass = beanArchiveIndex.getClassByName(annotation.name());
                constructorArguments[2 + i] = annotationLiterals.create(bytecode, annotationClass, annotation);
            }
        } else {
            ResultHandle array = bytecode.newArray(Map.Entry.class, annotationsCount);
            for (int i = 0; i < annotationsCount; i++) {
                AnnotationInstance annotation = annotations.get(i);
                ClassInfo annotationClass = beanArchiveIndex.getClassByName(annotation.name());
                ResultHandle annotationClassHandle = bytecode.loadClass(annotationClass);
                ResultHandle annotationHandle = annotationLiterals.create(bytecode, annotationClass, annotation);
                ResultHandle entry = bytecode.invokeStaticInterfaceMethod(MethodDescriptors.MAP_ENTRY, annotationClassHandle,
                        annotationHandle);
                bytecode.writeArrayValue(array, i, entry);
            }
            constructorArguments[2] = bytecode.invokeStaticInterfaceMethod(MethodDescriptors.MAP_OF_ENTRIES, array);
        }

        return bytecode.newInstance(
                MethodDescriptor.ofConstructor(parameterMetadataClass.generatedClassName, (Object[]) constructorParameterTypes),
                constructorArguments);
    }

    private static String generateMethodMetadataClassName(MethodMetadataShape shape) {
        return AbstractGenerator.DEFAULT_PACKAGE + ".MethodMetadataImpl"
                + "_p" + shape.parametersCount
                + "_a" + shape.annotationsCount;
    }

    private static String generateParameterMetadataClassName(ParameterMetadataShape shape) {
        return AbstractGenerator.DEFAULT_PACKAGE + ".ParameterMetadataImpl"
                + "_a" + shape.annotationsCount;
    }

    static class MethodMetadataShape {
        final int parametersCount;

        final int annotationsCount;

        MethodMetadataShape(int parametersCount, int annotationsCount) {
            this.annotationsCount = annotationsCount;
            this.parametersCount = parametersCount;
        }
    }

    static class MethodMetadataImplClass {
        final MethodMetadataShape shape;

        final String generatedClassName;

        final boolean isApplicationClass;

        final boolean parametersUnfolded;
        final int parametersSlots;

        final boolean annotationsUnfolded;
        final int annotationsSlots;

        MethodMetadataImplClass(MethodMetadataShape shape, String generatedClassName, boolean isApplicationClass) {
            this.shape = shape;
            this.generatedClassName = generatedClassName;
            this.isApplicationClass = isApplicationClass;

            this.parametersUnfolded = shape.parametersCount <= MethodMetadataGenerator.UNFOLD_ARRAYS_THRESHOLD;
            this.parametersSlots = parametersUnfolded ? shape.parametersCount : 1;

            this.annotationsUnfolded = shape.annotationsCount <= MethodMetadataGenerator.UNFOLD_ARRAYS_THRESHOLD;
            this.annotationsSlots = annotationsUnfolded ? shape.annotationsCount : 1;
        }

        Class<?>[] constructorParameterTypes() {
            Class<?>[] result = new Class[6 + parametersSlots + annotationsSlots];
            result[0] = boolean.class; // isConstructor
            result[1] = boolean.class; // isStatic
            result[2] = String.class; // name
            result[3] = int.class; // modifiers
            result[4] = Class.class; // declaringClass
            result[5] = Class.class; // returnType
            if (parametersUnfolded) {
                for (int i = 0; i < shape.parametersCount; i++) {
                    result[6 + i] = ParameterMetadata.class;
                }
            } else {
                result[6] = ParameterMetadata[].class;
            }
            if (annotationsUnfolded) {
                for (int i = 0; i < shape.annotationsCount; i++) {
                    result[6 + parametersSlots + i] = Annotation.class;
                }
            } else {
                result[6 + parametersSlots] = Map.class;
            }
            return result;
        }
    }

    static class ParameterMetadataShape {
        final int annotationsCount;

        ParameterMetadataShape(int annotationsCount) {
            this.annotationsCount = annotationsCount;
        }
    }

    static class ParameterMetadataImplClass {
        final ParameterMetadataShape shape;

        final String generatedClassName;

        final boolean isApplicationClass;

        final boolean annotationsUnfolded;
        final int annotationsSlots;

        ParameterMetadataImplClass(ParameterMetadataShape shape, String generatedClassName, boolean isApplicationClass) {
            this.shape = shape;
            this.generatedClassName = generatedClassName;
            this.isApplicationClass = isApplicationClass;

            this.annotationsUnfolded = shape.annotationsCount <= MethodMetadataGenerator.UNFOLD_ARRAYS_THRESHOLD;
            this.annotationsSlots = annotationsUnfolded ? shape.annotationsCount : 1;
        }

        Class<?>[] constructorParameterTypes() {
            Class<?>[] result = new Class[2 + annotationsSlots];
            result[0] = String.class; // name
            result[1] = Class.class; // type
            if (annotationsUnfolded) {
                for (int i = 0; i < shape.annotationsCount; i++) {
                    result[2 + i] = Annotation.class;
                }
            } else {
                result[2] = Map.class;
            }
            return result;
        }
    }
}
