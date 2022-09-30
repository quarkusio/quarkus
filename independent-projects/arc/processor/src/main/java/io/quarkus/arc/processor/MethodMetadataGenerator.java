package io.quarkus.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import io.quarkus.arc.MethodMetadata;
import io.quarkus.arc.ParameterMetadata;
import io.quarkus.arc.impl.ComputingCache;
import io.quarkus.arc.processor.MethodMetadataProcessor.MethodMetadataImplClass;
import io.quarkus.arc.processor.MethodMetadataProcessor.MethodMetadataShape;
import io.quarkus.arc.processor.MethodMetadataProcessor.ParameterMetadataImplClass;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 * This is an internal companion of {@link MethodMetadataProcessor} that handles generating
 * {@code MethodMetadata} and {@code ParameterMetadata} implementations.
 * <p>
 * See {@link #generate(ComputingCache, ComputingCache, Set) generate()} for more info.
 */
public class MethodMetadataGenerator extends AbstractGenerator {
    static final int UNFOLD_ARRAYS_THRESHOLD = 4; // value found empirically

    MethodMetadataGenerator(boolean generateSources) {
        super(generateSources);
    }

    /**
     * Creator of a {@link MethodMetadataProcessor} must call this method at an appropriate point
     * in time and write the result to an appropriate output. If not, the bytecode sequences generated
     * using the {@code MethodMetadataProcessor} will refer to non-existing classes.
     *
     * @param existingClasses names of classes that already exist and should not be generated again
     * @return the generated classes, never {@code null}
     */
    Collection<Resource> generate(ComputingCache<MethodMetadataShape, MethodMetadataImplClass> methodsToGenerate,
            ComputingCache<MethodMetadataProcessor.ParameterMetadataShape, ParameterMetadataImplClass> parametersToGenerate,
            Set<String> existingClasses) {
        List<Resource> resources = new ArrayList<>();
        methodsToGenerate.forEachExistingValue(clazz -> {
            ResourceClassOutput classOutput = new ResourceClassOutput(clazz.isApplicationClass, generateSources);
            createMethodMetadataClass(classOutput, clazz, existingClasses);
            resources.addAll(classOutput.getResources());
        });
        parametersToGenerate.forEachExistingValue(clazz -> {
            ResourceClassOutput classOutput = new ResourceClassOutput(clazz.isApplicationClass, generateSources);
            createParameterMetadataClass(classOutput, clazz, existingClasses);
            resources.addAll(classOutput.getResources());
        });
        return resources;
    }

    /**
     * Creator of a {@link MethodMetadataProcessor} must call this method at an appropriate point
     * in time and write the result to an appropriate output. If not, the bytecode sequences generated
     * using the {@code MethodMetadataProcessor} will refer to non-existing classes.
     *
     * @param existingClasses names of classes that already exist and should not be generated again
     * @return the generated classes, never {@code null}
     */
    Collection<Future<Collection<Resource>>> generate(
            ComputingCache<MethodMetadataShape, MethodMetadataImplClass> methodsToGenerate,
            ComputingCache<MethodMetadataProcessor.ParameterMetadataShape, ParameterMetadataImplClass> parametersToGenerate,
            Set<String> existingClasses, ExecutorService executor) {
        List<Future<Collection<Resource>>> futures = new ArrayList<>();
        methodsToGenerate.forEachExistingValue(clazz -> {
            futures.add(executor.submit(() -> {
                ResourceClassOutput classOutput = new ResourceClassOutput(clazz.isApplicationClass, generateSources);
                createMethodMetadataClass(classOutput, clazz, existingClasses);
                return classOutput.getResources();
            }));
        });
        parametersToGenerate.forEachExistingValue(clazz -> {
            futures.add(executor.submit(() -> {
                ResourceClassOutput classOutput = new ResourceClassOutput(clazz.isApplicationClass, generateSources);
                createParameterMetadataClass(classOutput, clazz, existingClasses);
                return classOutput.getResources();
            }));
        });
        return futures;
    }

    /**
     * Based on given {@code methodMetadataClass} data, generates a {@code MethodMetadata} implementation
     * into the given {@code classOutput}. Does nothing if {@code existingClasses} indicates that the class
     * to be generated already exists.
     *
     * @param classOutput the output to which the class is written
     * @param methodMetadataClass data about the {@code MethodMetadata} class to be generated
     * @param existingClasses set of existing classes that shouldn't be generated again
     */
    private void createMethodMetadataClass(ClassOutput classOutput, MethodMetadataImplClass methodMetadataClass,
            Set<String> existingClasses) {

        String generatedClassName = methodMetadataClass.generatedClassName.replace('.', '/');
        if (existingClasses.contains(generatedClassName)) {
            return;
        }

        try (ClassCreator clazz = ClassCreator.builder()
                .classOutput(classOutput)
                .className(generatedClassName)
                .interfaces(MethodMetadata.class)
                .setFinal(true)
                .build()) {

            // boolean isConstructor;
            FieldCreator isConstructorField = clazz.getFieldCreator("isConstructor", boolean.class);
            isConstructorField.setModifiers(ACC_PRIVATE | ACC_FINAL);

            // boolean isStatic;
            FieldCreator isStaticField = clazz.getFieldCreator("isStatic", boolean.class);
            isStaticField.setModifiers(ACC_PRIVATE | ACC_FINAL);

            // String name;
            FieldCreator nameField = clazz.getFieldCreator("name", String.class);
            nameField.setModifiers(ACC_PRIVATE | ACC_FINAL);

            // int modifiers;
            FieldCreator modifiersField = clazz.getFieldCreator("modifiers", int.class);
            modifiersField.setModifiers(ACC_PRIVATE | ACC_FINAL);

            // Class declaringClass;
            FieldCreator declaringClassField = clazz.getFieldCreator("declaringClass", Class.class);
            declaringClassField.setModifiers(ACC_PRIVATE | ACC_FINAL);

            // Class returnType;
            FieldCreator returnTypeField = clazz.getFieldCreator("returnType", Class.class);
            returnTypeField.setModifiers(ACC_PRIVATE | ACC_FINAL);

            // ParameterMetadata parameterN;
            // or
            // ParameterMetadata[] parameters;
            int parametersCount = methodMetadataClass.shape.parametersCount;
            List<FieldCreator> unfoldedParameters = new ArrayList<>(parametersCount);
            FieldCreator parametersArray = null;
            if (methodMetadataClass.parametersUnfolded) {
                for (int i = 0; i < parametersCount; i++) {
                    FieldCreator parameter = clazz.getFieldCreator("parameter" + i, ParameterMetadata.class);
                    parameter.setModifiers(ACC_PRIVATE | ACC_FINAL);
                    unfoldedParameters.add(parameter);
                }
            } else {
                parametersArray = clazz.getFieldCreator("parameters", ParameterMetadata[].class);
                parametersArray.setModifiers(ACC_PRIVATE | ACC_FINAL);
            }

            int annotationsCount = methodMetadataClass.shape.annotationsCount;
            AnnotationFields annotationFields = generateAnnotationFields(clazz, annotationsCount);

            // constructor
            MethodCreator constructor = clazz.getMethodCreator(Methods.INIT, void.class,
                    (Object[]) methodMetadataClass.constructorParameterTypes());
            constructor.invokeSpecialMethod(MethodDescriptors.OBJECT_CONSTRUCTOR, constructor.getThis());
            constructor.writeInstanceField(isConstructorField.getFieldDescriptor(), constructor.getThis(),
                    constructor.getMethodParam(0));
            constructor.writeInstanceField(isStaticField.getFieldDescriptor(), constructor.getThis(),
                    constructor.getMethodParam(1));
            constructor.writeInstanceField(nameField.getFieldDescriptor(), constructor.getThis(),
                    constructor.getMethodParam(2));
            constructor.writeInstanceField(modifiersField.getFieldDescriptor(), constructor.getThis(),
                    constructor.getMethodParam(3));
            constructor.writeInstanceField(declaringClassField.getFieldDescriptor(), constructor.getThis(),
                    constructor.getMethodParam(4));
            constructor.writeInstanceField(returnTypeField.getFieldDescriptor(), constructor.getThis(),
                    constructor.getMethodParam(5));
            if (methodMetadataClass.parametersUnfolded) {
                for (int i = 0; i < parametersCount; i++) {
                    constructor.writeInstanceField(unfoldedParameters.get(i).getFieldDescriptor(), constructor.getThis(),
                            constructor.getMethodParam(6 + i));
                }
            } else {
                constructor.writeInstanceField(parametersArray.getFieldDescriptor(), constructor.getThis(),
                        constructor.getMethodParam(6));
            }
            if (methodMetadataClass.annotationsUnfolded) {
                for (int i = 0; i < annotationsCount; i++) {
                    constructor.writeInstanceField(annotationFields.unfolded.get(i).getFieldDescriptor(), constructor.getThis(),
                            constructor.getMethodParam(6 + methodMetadataClass.parametersSlots + i));
                }
            } else {
                constructor.writeInstanceField(annotationFields.map.getFieldDescriptor(), constructor.getThis(),
                        constructor.getMethodParam(6 + methodMetadataClass.parametersSlots));
            }
            constructor.returnValue(null);

            // boolean isConstructor();
            MethodCreator isConstructor = clazz.getMethodCreator("isConstructor", boolean.class);
            isConstructor.returnValue(
                    isConstructor.readInstanceField(isConstructorField.getFieldDescriptor(), isConstructor.getThis()));

            // boolean isStatic();
            MethodCreator isStatic = clazz.getMethodCreator("isStatic", boolean.class);
            isStatic.returnValue(isStatic.readInstanceField(isStaticField.getFieldDescriptor(), isStatic.getThis()));

            // String getName();
            MethodCreator getName = clazz.getMethodCreator("getName", String.class);
            getName.returnValue(getName.readInstanceField(nameField.getFieldDescriptor(), getName.getThis()));

            // int getModifiers();
            MethodCreator getModifiers = clazz.getMethodCreator("getModifiers", int.class);
            getModifiers
                    .returnValue(getModifiers.readInstanceField(modifiersField.getFieldDescriptor(), getModifiers.getThis()));

            // Class<?> getDeclaringClass();
            MethodCreator getDeclaringClass = clazz.getMethodCreator("getDeclaringClass", Class.class);
            getDeclaringClass.returnValue(
                    getDeclaringClass.readInstanceField(declaringClassField.getFieldDescriptor(), getDeclaringClass.getThis()));

            // Class<?> getReturnType();
            MethodCreator getReturnType = clazz.getMethodCreator("getReturnType", Class.class);
            getReturnType.returnValue(
                    getReturnType.readInstanceField(returnTypeField.getFieldDescriptor(), getReturnType.getThis()));

            // int getParameterCount();
            MethodCreator getParameterCount = clazz.getMethodCreator("getParameterCount", int.class);
            getParameterCount.returnValue(getParameterCount.load(parametersCount));

            // Class<?>[] getParameterTypes();
            MethodCreator getParameterTypes = clazz.getMethodCreator("getParameterTypes", Class[].class);
            if (parametersCount == 0) {
                ResultHandle emptyArray = getParameterTypes.readStaticField(
                        FieldDescriptors.ANNOTATION_LITERALS_EMPTY_CLASS_ARRAY);
                getParameterTypes.returnValue(emptyArray);
            } else if (methodMetadataClass.parametersUnfolded) {
                ResultHandle array = getParameterTypes.newArray(Class.class, parametersCount);
                for (int i = 0; i < unfoldedParameters.size(); i++) {
                    ResultHandle parameter = getParameterTypes.readInstanceField(
                            unfoldedParameters.get(i).getFieldDescriptor(), getParameterTypes.getThis());
                    ResultHandle parameterType = getParameterTypes.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(ParameterMetadata.class, "getType", Class.class),
                            parameter);
                    getParameterTypes.writeArrayValue(array, i, parameterType);
                }
                getParameterTypes.returnValue(array);
            } else {
                ResultHandle localArray = getParameterTypes.readInstanceField(parametersArray.getFieldDescriptor(),
                        getParameterTypes.getThis());
                ResultHandle array = getParameterTypes.newArray(Class.class, parametersCount);
                for (int i = 0; i < parametersCount; i++) {
                    ResultHandle parameter = getParameterTypes.readArrayValue(localArray, i);
                    ResultHandle parameterType = getParameterTypes.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(ParameterMetadata.class, "getType", Class.class),
                            parameter);
                    getParameterTypes.writeArrayValue(array, i, parameterType);
                }
                getParameterTypes.returnValue(array);
            }

            // ParameterMetadata[] getParameters();
            MethodCreator getParameters = clazz.getMethodCreator("getParameters", ParameterMetadata[].class);
            if (parametersCount == 0) {
                ResultHandle emptyArray = getParameters.readStaticField(
                        FieldDescriptor.of(ParameterMetadata.class, "EMPTY_ARRAY", ParameterMetadata[].class));
                getParameters.returnValue(emptyArray);
            } else if (methodMetadataClass.parametersUnfolded) {
                ResultHandle array = getParameters.newArray(ParameterMetadata.class, parametersCount);
                for (int i = 0; i < unfoldedParameters.size(); i++) {
                    ResultHandle parameter = getParameters.readInstanceField(
                            unfoldedParameters.get(i).getFieldDescriptor(), getParameters.getThis());
                    getParameters.writeArrayValue(array, i, parameter);
                }
                getParameters.returnValue(array);
            } else {
                ResultHandle localArray = getParameters.readInstanceField(parametersArray.getFieldDescriptor(),
                        getParameters.getThis());
                ResultHandle clone = getParameters.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Annotation[].class, "clone", Annotation[].class), localArray);
                getParameters.returnValue(clone);
            }

            generateAnnotationMethods(clazz, annotationFields);

            // String toString();
            MethodCreator toString = clazz.getMethodCreator("toString", String.class);
            ResultHandle declaringClass = toString.readInstanceField(declaringClassField.getFieldDescriptor(),
                    toString.getThis());
            ResultHandle declaringClassName = toString.invokeVirtualMethod(MethodDescriptors.CLASS_GET_NAME, declaringClass);
            ResultHandle returnType = toString.readInstanceField(returnTypeField.getFieldDescriptor(), toString.getThis());
            ResultHandle returnTypeName = toString.invokeVirtualMethod(MethodDescriptors.CLASS_GET_NAME, returnType);
            ResultHandle name = toString.readInstanceField(nameField.getFieldDescriptor(), toString.getThis());
            Gizmo.StringBuilderGenerator str = Gizmo.newStringBuilder(toString);
            str.append(returnTypeName)
                    .append(' ')
                    .append(declaringClassName)
                    .append('.')
                    .append(name)
                    .append('(');
            if (methodMetadataClass.parametersUnfolded) {
                for (int i = 0; i < unfoldedParameters.size(); i++) {
                    if (i > 0) {
                        str.append(", ");
                    }

                    ResultHandle parameter = toString.readInstanceField(
                            unfoldedParameters.get(i).getFieldDescriptor(), toString.getThis());
                    ResultHandle parameterType = toString.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(ParameterMetadata.class, "getType", Class.class),
                            parameter);
                    ResultHandle parameterTypeName = toString.invokeVirtualMethod(MethodDescriptors.CLASS_GET_NAME,
                            parameterType);
                    str.append(parameterTypeName);
                }
            } else {
                ResultHandle localArray = toString.readInstanceField(parametersArray.getFieldDescriptor(),
                        toString.getThis());
                for (int i = 0; i < parametersCount; i++) {
                    if (i > 0) {
                        str.append(", ");
                    }

                    ResultHandle parameter = toString.readArrayValue(localArray, i);
                    ResultHandle parameterType = toString.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(ParameterMetadata.class, "getType", Class.class),
                            parameter);
                    ResultHandle parameterTypeName = toString.invokeVirtualMethod(MethodDescriptors.CLASS_GET_NAME,
                            parameterType);
                    str.append(parameterTypeName);
                }
            }
            str.append(')');
            toString.returnValue(str.callToString());

            Gizmo.generateEqualsAndHashCode(clazz, clazz.getExistingFields());
        }
    }

    /**
     * Based on given {@code parameterMetadataClass} data, generates a {@code ParameterMetadata} implementation
     * into the given {@code classOutput}. Does nothing if {@code existingClasses} indicates that the class
     * to be generated already exists.
     *
     * @param classOutput the output to which the class is written
     * @param parameterMetadataClass data about the {@code ParameterMetadata} class to be generated
     * @param existingClasses set of existing classes that shouldn't be generated again
     */
    private void createParameterMetadataClass(ClassOutput classOutput, ParameterMetadataImplClass parameterMetadataClass,
            Set<String> existingClasses) {

        String generatedClassName = parameterMetadataClass.generatedClassName.replace('.', '/');
        if (existingClasses.contains(generatedClassName)) {
            return;
        }

        try (ClassCreator clazz = ClassCreator.builder()
                .classOutput(classOutput)
                .className(generatedClassName)
                .interfaces(ParameterMetadata.class)
                .setFinal(true)
                .build()) {

            // String name;
            FieldCreator nameField = clazz.getFieldCreator("name", String.class);
            nameField.setModifiers(ACC_PRIVATE | ACC_FINAL);

            // Class type;
            FieldCreator typeField = clazz.getFieldCreator("type", Class.class);
            typeField.setModifiers(ACC_PRIVATE | ACC_FINAL);

            int annotationsCount = parameterMetadataClass.shape.annotationsCount;
            AnnotationFields annotationFields = generateAnnotationFields(clazz, annotationsCount);

            // constructor
            MethodCreator constructor = clazz.getMethodCreator(Methods.INIT, void.class,
                    (Object[]) parameterMetadataClass.constructorParameterTypes());
            constructor.invokeSpecialMethod(MethodDescriptors.OBJECT_CONSTRUCTOR, constructor.getThis());
            constructor.writeInstanceField(nameField.getFieldDescriptor(), constructor.getThis(),
                    constructor.getMethodParam(0));
            constructor.writeInstanceField(typeField.getFieldDescriptor(), constructor.getThis(),
                    constructor.getMethodParam(1));
            if (parameterMetadataClass.annotationsUnfolded) {
                for (int i = 0; i < annotationsCount; i++) {
                    constructor.writeInstanceField(annotationFields.unfolded.get(i).getFieldDescriptor(),
                            constructor.getThis(), constructor.getMethodParam(2 + i));
                }
            } else {
                constructor.writeInstanceField(annotationFields.map.getFieldDescriptor(),
                        constructor.getThis(), constructor.getMethodParam(2));
            }
            constructor.returnValue(null);

            // String getName();
            MethodCreator getName = clazz.getMethodCreator("getName", String.class);
            getName.returnValue(getName.readInstanceField(nameField.getFieldDescriptor(), getName.getThis()));

            // Class<?> getType();
            MethodCreator getType = clazz.getMethodCreator("getType", Class.class);
            getType.returnValue(getType.readInstanceField(typeField.getFieldDescriptor(), getType.getThis()));

            generateAnnotationMethods(clazz, annotationFields);

            // String toString();
            MethodCreator toString = clazz.getMethodCreator("toString", String.class);
            ResultHandle type = toString.readInstanceField(typeField.getFieldDescriptor(), toString.getThis());
            ResultHandle typeName = toString.invokeVirtualMethod(MethodDescriptors.CLASS_GET_NAME, type);
            ResultHandle name = toString.readInstanceField(nameField.getFieldDescriptor(), toString.getThis());
            Gizmo.StringBuilderGenerator str = Gizmo.newStringBuilder(toString);
            str.append(typeName).append(' ').append(name);
            toString.returnValue(str.callToString());

            Gizmo.generateEqualsAndHashCode(clazz, clazz.getExistingFields());
        }
    }

    private static class AnnotationFields {
        final List<FieldCreator> unfolded;
        final FieldCreator map;

        AnnotationFields(List<FieldCreator> unfolded, FieldCreator map) {
            this.unfolded = unfolded;
            this.map = map;
        }

        boolean isEmpty() {
            return unfolded == null && map == null;
        }
    }

    private static AnnotationFields generateAnnotationFields(ClassCreator clazz, int annotationsCount) {
        // Annotation annotationN;
        // or
        // Map<Class<? extend Annotation>, Annotation> annotations;
        if (annotationsCount == 0) {
            return new AnnotationFields(null, null);
        } else if (annotationsCount <= UNFOLD_ARRAYS_THRESHOLD) {
            List<FieldCreator> unfoldedAnnotations = new ArrayList<>(annotationsCount);
            for (int i = 0; i < annotationsCount; i++) {
                FieldCreator annotation = clazz.getFieldCreator("annotation" + i, Annotation.class);
                annotation.setModifiers(ACC_PRIVATE | ACC_FINAL);
                unfoldedAnnotations.add(annotation);
            }

            return new AnnotationFields(unfoldedAnnotations, null);
        } else {
            FieldCreator annotationsArray = clazz.getFieldCreator("annotations", Map.class);
            annotationsArray.setModifiers(ACC_PRIVATE | ACC_FINAL);

            return new AnnotationFields(null, annotationsArray);
        }
    }

    private static void generateAnnotationMethods(ClassCreator clazz, AnnotationFields annotationFields) {
        // boolean isAnnotationPresent(Class<? extends Annotation> annotationClass)
        MethodCreator isAnnotationPresent = clazz.getMethodCreator("isAnnotationPresent", boolean.class, Class.class);
        if (annotationFields.isEmpty()) {
            isAnnotationPresent.returnValue(isAnnotationPresent.load(false));
        } else if (annotationFields.unfolded != null) {
            ResultHandle param = isAnnotationPresent.getMethodParam(0);
            for (FieldCreator annotationField : annotationFields.unfolded) {
                ResultHandle annotation = isAnnotationPresent.readInstanceField(annotationField.getFieldDescriptor(),
                        isAnnotationPresent.getThis());
                ResultHandle annotationType = isAnnotationPresent.invokeInterfaceMethod(
                        MethodDescriptors.ANNOTATION_TYPE, annotation);
                ResultHandle equals = isAnnotationPresent.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS,
                        annotationType, param);
                BytecodeCreator ifTrue = isAnnotationPresent.ifTrue(equals).trueBranch();
                ifTrue.returnValue(ifTrue.load(true));
            }
            isAnnotationPresent.returnValue(isAnnotationPresent.load(false));
        } else {
            ResultHandle param = isAnnotationPresent.getMethodParam(0);
            ResultHandle localMap = isAnnotationPresent.readInstanceField(annotationFields.map.getFieldDescriptor(),
                    isAnnotationPresent.getThis());
            ResultHandle result = isAnnotationPresent.invokeInterfaceMethod(MethodDescriptors.MAP_CONTAINS_KEY, localMap,
                    param);
            isAnnotationPresent.returnValue(result);
        }

        // <T extends Annotation> T getAnnotation(Class<T> annotationClass);
        MethodCreator getAnnotation = clazz.getMethodCreator("getAnnotation", Annotation.class, Class.class);
        if (annotationFields.isEmpty()) {
            getAnnotation.returnValue(getAnnotation.loadNull());
        } else if (annotationFields.unfolded != null) {
            ResultHandle param = getAnnotation.getMethodParam(0);
            for (FieldCreator annotationField : annotationFields.unfolded) {
                ResultHandle annotation = getAnnotation.readInstanceField(annotationField.getFieldDescriptor(),
                        getAnnotation.getThis());
                ResultHandle annotationType = getAnnotation.invokeInterfaceMethod(MethodDescriptors.ANNOTATION_TYPE,
                        annotation);
                ResultHandle equals = getAnnotation.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS, annotationType, param);
                getAnnotation.ifTrue(equals).trueBranch().returnValue(annotation);
            }
            getAnnotation.returnValue(getAnnotation.loadNull());
        } else {
            ResultHandle param = getAnnotation.getMethodParam(0);
            ResultHandle localMap = getAnnotation.readInstanceField(annotationFields.map.getFieldDescriptor(),
                    getAnnotation.getThis());
            ResultHandle result = getAnnotation.invokeInterfaceMethod(MethodDescriptors.MAP_GET, localMap, param);
            getAnnotation.returnValue(result);
        }

        // Annotation[] getAnnotations();
        MethodCreator getAnnotations = clazz.getMethodCreator("getAnnotations", Annotation[].class);
        if (annotationFields.isEmpty()) {
            ResultHandle emptyArray = getAnnotations.readStaticField(
                    FieldDescriptors.ANNOTATION_LITERALS_EMPTY_ANNOTATION_ARRAY);
            getAnnotations.returnValue(emptyArray);
        } else if (annotationFields.unfolded != null) {
            ResultHandle array = getAnnotations.newArray(Annotation.class, annotationFields.unfolded.size());
            for (int i = 0; i < annotationFields.unfolded.size(); i++) {
                ResultHandle annotationHandle = getAnnotations.readInstanceField(
                        annotationFields.unfolded.get(i).getFieldDescriptor(), getAnnotations.getThis());
                getAnnotations.writeArrayValue(array, i, annotationHandle);
            }
            getAnnotations.returnValue(array);
        } else {
            ResultHandle localMap = getAnnotations.readInstanceField(annotationFields.map.getFieldDescriptor(),
                    getAnnotations.getThis());
            ResultHandle values = getAnnotations.invokeInterfaceMethod(MethodDescriptors.MAP_VALUES, localMap);
            ResultHandle emptyArray = getAnnotations
                    .readStaticField(FieldDescriptors.ANNOTATION_LITERALS_EMPTY_ANNOTATION_ARRAY);
            ResultHandle result = getAnnotations.invokeInterfaceMethod(MethodDescriptors.COLLECTION_TO_ARRAY, values,
                    emptyArray);
            getAnnotations.returnValue(result);
        }
    }
}
