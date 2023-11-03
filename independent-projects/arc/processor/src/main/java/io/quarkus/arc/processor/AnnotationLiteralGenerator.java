package io.quarkus.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.AbstractAnnotationLiteral;
import io.quarkus.arc.impl.ComputingCache;
import io.quarkus.arc.processor.AnnotationLiteralProcessor.AnnotationLiteralClassInfo;
import io.quarkus.arc.processor.AnnotationLiteralProcessor.CacheKey;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 * This is an internal companion of {@link AnnotationLiteralProcessor} that handles generating
 * annotation literal classes. See {@link #generate(ComputingCache, Set) generate()} for more info.
 */
public class AnnotationLiteralGenerator extends AbstractGenerator {
    private static final Logger LOGGER = Logger.getLogger(AnnotationLiteralGenerator.class);

    AnnotationLiteralGenerator(boolean generateSources) {
        super(generateSources);
    }

    /**
     * Creator of an {@link AnnotationLiteralProcessor} must call this method at an appropriate point
     * in time and write the result to an appropriate output. If not, the bytecode sequences generated
     * using the {@code AnnotationLiteralProcessor} will refer to non-existing classes.
     *
     * @param existingClasses names of classes that already exist and should not be generated again
     * @return the generated classes, never {@code null}
     */
    Collection<Resource> generate(ComputingCache<CacheKey, AnnotationLiteralClassInfo> cache,
            Set<String> existingClasses) {
        List<ResourceOutput.Resource> resources = new ArrayList<>();
        cache.forEachExistingValue(literal -> {
            ResourceClassOutput classOutput = new ResourceClassOutput(literal.isApplicationClass, generateSources);
            createAnnotationLiteralClass(classOutput, literal, existingClasses);
            resources.addAll(classOutput.getResources());
        });
        return resources;
    }

    /**
     * Creator of an {@link AnnotationLiteralProcessor} must call this method at an appropriate point
     * in time and write the result to an appropriate output. If not, the bytecode sequences generated
     * using the {@code AnnotationLiteralProcessor} will refer to non-existing classes.
     *
     * @param existingClasses names of classes that already exist and should not be generated again
     * @return the generated classes, never {@code null}
     */
    Collection<Future<Collection<Resource>>> generate(ComputingCache<CacheKey, AnnotationLiteralClassInfo> cache,
            Set<String> existingClasses, ExecutorService executor) {
        List<Future<Collection<Resource>>> futures = new ArrayList<>();
        cache.forEachExistingValue(literal -> {
            futures.add(executor.submit(new Callable<Collection<Resource>>() {
                @Override
                public Collection<Resource> call() throws Exception {
                    ResourceClassOutput classOutput = new ResourceClassOutput(literal.isApplicationClass, generateSources);
                    createAnnotationLiteralClass(classOutput, literal, existingClasses);
                    return classOutput.getResources();
                }
            }));
        });
        return futures;
    }

    /**
     * Based on given {@code literal} data, generates an annotation literal class into the given {@code classOutput}.
     * Does nothing if {@code existingClasses} indicates that the class to be generated already exists.
     * <p>
     * The generated annotation literal class is supposed to have a constructor that accepts values
     * of all annotation members.
     *
     * @param classOutput the output to which the class is written
     * @param literal data about the annotation literal class to be generated
     * @param existingClasses set of existing classes that shouldn't be generated again
     */
    private void createAnnotationLiteralClass(ClassOutput classOutput, AnnotationLiteralClassInfo literal,
            Set<String> existingClasses) {

        String generatedName = literal.generatedClassName.replace('.', '/');
        if (existingClasses.contains(generatedName)) {
            return;
        }

        ClassCreator annotationLiteral = ClassCreator.builder()
                .classOutput(classOutput)
                .className(generatedName)
                .superClass(AbstractAnnotationLiteral.class)
                .interfaces(literal.annotationName().toString())
                .build();

        MethodCreator constructor = annotationLiteral.getMethodCreator(Methods.INIT, "V",
                literal.annotationMembers().stream().map(m -> m.returnType().name().toString()).toArray());

        constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(AbstractAnnotationLiteral.class), constructor.getThis());

        int constructorParameterIndex = 0;
        for (MethodInfo annotationMember : literal.annotationMembers()) {
            String type = annotationMember.returnType().name().toString();
            FieldDescriptor field = FieldDescriptor.of(annotationLiteral.getClassName(), annotationMember.name(), type);

            // field
            annotationLiteral.getFieldCreator(field).setModifiers(ACC_PRIVATE | ACC_FINAL);

            // constructor: param -> field
            constructor.writeInstanceField(field, constructor.getThis(),
                    constructor.getMethodParam(constructorParameterIndex));

            // annotation member method implementation
            MethodCreator value = annotationLiteral.getMethodCreator(annotationMember.name(), type).setModifiers(ACC_PUBLIC);
            value.returnValue(value.readInstanceField(field, value.getThis()));

            constructorParameterIndex++;
        }
        constructor.returnVoid();

        MethodCreator annotationType = annotationLiteral.getMethodCreator("annotationType", Class.class)
                .setModifiers(ACC_PUBLIC);
        annotationType.returnValue(annotationType.loadClass(literal.annotationClass));

        if (literal.annotationMembers().isEmpty()) {
            constructor.setModifiers(ACC_PRIVATE);

            FieldCreator singleton = annotationLiteral.getFieldCreator("INSTANCE", generatedName);
            singleton.setModifiers(ACC_PUBLIC | ACC_STATIC | ACC_FINAL);

            MethodCreator staticInit = annotationLiteral.getMethodCreator(Methods.CLINIT, void.class);
            staticInit.setModifiers(ACC_STATIC);
            ResultHandle singletonInstance = staticInit.newInstance(constructor.getMethodDescriptor());
            staticInit.writeStaticField(singleton.getFieldDescriptor(), singletonInstance);
            staticInit.returnVoid();
        } else {
            generateStaticFieldsWithDefaultValues(annotationLiteral, literal.annotationMembers());
        }

        generateEquals(annotationLiteral, literal);
        generateHashCode(annotationLiteral, literal);
        generateToString(annotationLiteral, literal);

        annotationLiteral.close();
        LOGGER.debugf("Annotation literal class generated: %s", literal.generatedClassName);
    }

    static String defaultValueStaticFieldName(MethodInfo annotationMember) {
        return annotationMember.name() + "_default_value";
    }

    private static boolean returnsClassOrClassArray(MethodInfo annotationMember) {
        boolean returnsClass = DotNames.CLASS.equals(annotationMember.returnType().name());
        boolean returnsClassArray = annotationMember.returnType().kind() == Type.Kind.ARRAY
                && DotNames.CLASS.equals(annotationMember.returnType().asArrayType().constituent().name());
        return returnsClass || returnsClassArray;
    }

    /**
     * Generates {@code public static final} fields for all the annotation members
     * that provide a default value and are of a class or class array type.
     * Also generates a static initializer that assigns the default value of those
     * annotation members to the generated fields.
     *
     * @param classCreator the class to which the fields and the static initializer should be added
     * @param annotationMembers the full set of annotation members of an annotation type
     */
    private static void generateStaticFieldsWithDefaultValues(ClassCreator classCreator, List<MethodInfo> annotationMembers) {
        List<MethodInfo> defaultOfClassType = new ArrayList<>();
        for (MethodInfo annotationMember : annotationMembers) {
            if (annotationMember.defaultValue() != null && returnsClassOrClassArray(annotationMember)) {
                defaultOfClassType.add(annotationMember);
            }
        }

        if (defaultOfClassType.isEmpty()) {
            return;
        }

        MethodCreator staticConstructor = classCreator.getMethodCreator(Methods.CLINIT, void.class);
        staticConstructor.setModifiers(ACC_STATIC);

        for (MethodInfo annotationMember : defaultOfClassType) {
            String type = annotationMember.returnType().name().toString();
            AnnotationValue defaultValue = annotationMember.defaultValue();

            FieldCreator fieldCreator = classCreator.getFieldCreator(defaultValueStaticFieldName(annotationMember), type);
            fieldCreator.setModifiers(ACC_PUBLIC | ACC_STATIC | ACC_FINAL);

            if (defaultValue.kind() == AnnotationValue.Kind.ARRAY) {
                Type[] clazzArray = defaultValue.asClassArray();
                ResultHandle array = staticConstructor.newArray(type, clazzArray.length);
                for (int i = 0; i < clazzArray.length; ++i) {
                    staticConstructor.writeArrayValue(array, staticConstructor.load(i),
                            staticConstructor.loadClass(clazzArray[i].name().toString()));
                }
                staticConstructor.writeStaticField(fieldCreator.getFieldDescriptor(), array);
            } else {
                staticConstructor.writeStaticField(fieldCreator.getFieldDescriptor(),
                        staticConstructor.loadClass(defaultValue.asClass().name().toString()));

            }
        }

        staticConstructor.returnVoid();
    }

    // ---
    // note that `java.lang.annotation.Annotation` specifies exactly how `equals` and `hashCode` should work
    // (and there's also a recommendation for `toString`)

    private static final MethodDescriptor FLOAT_TO_INT_BITS = MethodDescriptor.ofMethod(Float.class, "floatToIntBits",
            int.class, float.class);
    private static final MethodDescriptor DOUBLE_TO_LONG_BITS = MethodDescriptor.ofMethod(Double.class, "doubleToLongBits",
            long.class, double.class);

    private static final MethodDescriptor BOOLEAN_ARRAY_EQUALS = MethodDescriptor.ofMethod(Arrays.class, "equals",
            boolean.class, boolean[].class, boolean[].class);
    private static final MethodDescriptor BYTE_ARRAY_EQUALS = MethodDescriptor.ofMethod(Arrays.class, "equals", boolean.class,
            byte[].class, byte[].class);
    private static final MethodDescriptor SHORT_ARRAY_EQUALS = MethodDescriptor.ofMethod(Arrays.class, "equals", boolean.class,
            short[].class, short[].class);
    private static final MethodDescriptor INT_ARRAY_EQUALS = MethodDescriptor.ofMethod(Arrays.class, "equals", boolean.class,
            int[].class, int[].class);
    private static final MethodDescriptor LONG_ARRAY_EQUALS = MethodDescriptor.ofMethod(Arrays.class, "equals", boolean.class,
            long[].class, long[].class);
    private static final MethodDescriptor FLOAT_ARRAY_EQUALS = MethodDescriptor.ofMethod(Arrays.class, "equals", boolean.class,
            float[].class, float[].class);
    private static final MethodDescriptor DOUBLE_ARRAY_EQUALS = MethodDescriptor.ofMethod(Arrays.class, "equals", boolean.class,
            double[].class, double[].class);
    private static final MethodDescriptor CHAR_ARRAY_EQUALS = MethodDescriptor.ofMethod(Arrays.class, "equals", boolean.class,
            char[].class, char[].class);
    private static final MethodDescriptor OBJECT_ARRAY_EQUALS = MethodDescriptor.ofMethod(Arrays.class, "equals", boolean.class,
            Object[].class, Object[].class);

    private static final MethodDescriptor BOOLEAN_HASH_CODE = MethodDescriptor.ofMethod(Boolean.class, "hashCode", int.class,
            boolean.class);
    private static final MethodDescriptor BOOLEAN_ARRAY_HASH_CODE = MethodDescriptor.ofMethod(Arrays.class, "hashCode",
            int.class, boolean[].class);
    private static final MethodDescriptor BYTE_HASH_CODE = MethodDescriptor.ofMethod(Byte.class, "hashCode", int.class,
            byte.class);
    private static final MethodDescriptor BYTE_ARRAY_HASH_CODE = MethodDescriptor.ofMethod(Arrays.class, "hashCode", int.class,
            byte[].class);
    private static final MethodDescriptor SHORT_HASH_CODE = MethodDescriptor.ofMethod(Short.class, "hashCode", int.class,
            short.class);
    private static final MethodDescriptor SHORT_ARRAY_HASH_CODE = MethodDescriptor.ofMethod(Arrays.class, "hashCode", int.class,
            short[].class);
    private static final MethodDescriptor INT_HASH_CODE = MethodDescriptor.ofMethod(Integer.class, "hashCode", int.class,
            int.class);
    private static final MethodDescriptor INT_ARRAY_HASH_CODE = MethodDescriptor.ofMethod(Arrays.class, "hashCode", int.class,
            int[].class);
    private static final MethodDescriptor LONG_HASH_CODE = MethodDescriptor.ofMethod(Long.class, "hashCode", int.class,
            long.class);
    private static final MethodDescriptor LONG_ARRAY_HASH_CODE = MethodDescriptor.ofMethod(Arrays.class, "hashCode", int.class,
            long[].class);
    private static final MethodDescriptor FLOAT_ARRAY_HASH_CODE = MethodDescriptor.ofMethod(Arrays.class, "hashCode", int.class,
            float[].class);
    private static final MethodDescriptor FLOAT_HASH_CODE = MethodDescriptor.ofMethod(Float.class, "hashCode", int.class,
            float.class);
    private static final MethodDescriptor DOUBLE_HASH_CODE = MethodDescriptor.ofMethod(Double.class, "hashCode", int.class,
            double.class);
    private static final MethodDescriptor DOUBLE_ARRAY_HASH_CODE = MethodDescriptor.ofMethod(Arrays.class, "hashCode",
            int.class, double[].class);
    private static final MethodDescriptor CHAR_HASH_CODE = MethodDescriptor.ofMethod(Character.class, "hashCode", int.class,
            char.class);
    private static final MethodDescriptor CHAR_ARRAY_HASH_CODE = MethodDescriptor.ofMethod(Arrays.class, "hashCode", int.class,
            char[].class);
    private static final MethodDescriptor OBJECT_ARRAY_HASH_CODE = MethodDescriptor.ofMethod(Arrays.class, "hashCode",
            int.class, Object[].class);

    private static final MethodDescriptor BOOLEAN_ARRAY_TO_STRING = MethodDescriptor.ofMethod(Arrays.class, "toString",
            String.class, boolean[].class);
    private static final MethodDescriptor BYTE_ARRAY_TO_STRING = MethodDescriptor.ofMethod(Arrays.class, "toString",
            String.class, byte[].class);
    private static final MethodDescriptor SHORT_ARRAY_TO_STRING = MethodDescriptor.ofMethod(Arrays.class, "toString",
            String.class, short[].class);
    private static final MethodDescriptor INT_ARRAY_TO_STRING = MethodDescriptor.ofMethod(Arrays.class, "toString",
            String.class, int[].class);
    private static final MethodDescriptor LONG_ARRAY_TO_STRING = MethodDescriptor.ofMethod(Arrays.class, "toString",
            String.class, long[].class);
    private static final MethodDescriptor FLOAT_ARRAY_TO_STRING = MethodDescriptor.ofMethod(Arrays.class, "toString",
            String.class, float[].class);
    private static final MethodDescriptor DOUBLE_ARRAY_TO_STRING = MethodDescriptor.ofMethod(Arrays.class, "toString",
            String.class, double[].class);
    private static final MethodDescriptor CHAR_ARRAY_TO_STRING = MethodDescriptor.ofMethod(Arrays.class, "toString",
            String.class, char[].class);
    private static final MethodDescriptor OBJECT_ARRAY_TO_STRING = MethodDescriptor.ofMethod(Arrays.class, "toString",
            String.class, Object[].class);

    private static void generateEquals(ClassCreator clazz, AnnotationLiteralClassInfo literal) {
        MethodCreator equals = clazz.getMethodCreator("equals", boolean.class, Object.class);

        // this looks weird, but makes decompiled code look nicer
        equals.ifReferencesNotEqual(equals.getThis(), equals.getMethodParam(0))
                .falseBranch().returnBoolean(true);

        if (literal.annotationMembers().isEmpty()) {
            // special case for memberless annotations
            //
            // a lot of people apparently use the construct `new AnnotationLiteral<MyAnnotation>() {}`
            // to create an annotation literal for a memberless annotation, which is wrong, because
            // the result doesn't implement the annotation interface
            //
            // yet, we handle that case here by doing what `AnnotationLiteral` does: instead of
            // checking that the other object is an instance of the same annotation interface,
            // as specified by the `Annotation.equals()` contract, we check that it implements
            // the `Annotation` interface and have the same `annotationType()`
            equals.ifTrue(equals.instanceOf(equals.getMethodParam(0), Annotation.class))
                    .falseBranch().returnBoolean(false);
            ResultHandle thisAnnType = equals.loadClass(literal.annotationClass);
            ResultHandle otherAnnType = equals.invokeInterfaceMethod(MethodDescriptor.ofMethod(Annotation.class,
                    "annotationType", Class.class), equals.getMethodParam(0));
            equals.returnValue(Gizmo.equals(equals, thisAnnType, otherAnnType));
        }

        equals.ifTrue(equals.instanceOf(equals.getMethodParam(0), literal.annotationClass.name().toString()))
                .falseBranch().returnBoolean(false);

        ResultHandle other = equals.checkCast(equals.getMethodParam(0), literal.annotationClass.name().toString());

        for (MethodInfo annotationMember : literal.annotationMembers()) {
            String type = annotationMember.returnType().name().toString();

            // for `this` object, can read directly from the field, that's what the method also does
            FieldDescriptor field = FieldDescriptor.of(clazz.getClassName(), annotationMember.name(), type);
            ResultHandle thisValue = equals.readInstanceField(field, equals.getThis());

            // for the other object, must invoke the method
            ResultHandle thatValue = equals.invokeInterfaceMethod(annotationMember, other);

            // type of the field (in this class) is the same as return type of the method (in both classes)
            switch (field.getType()) {
                case "Z": // boolean
                case "B": // byte
                case "S": // short
                case "I": // int
                case "C": // char
                    equals.ifIntegerEqual(thisValue, thatValue)
                            .falseBranch().returnBoolean(false);
                    break;
                case "J": // long
                    equals.ifZero(equals.compareLong(thisValue, thatValue))
                            .falseBranch().returnBoolean(false);
                    break;
                case "F": // float
                    equals.ifIntegerEqual(equals.invokeStaticMethod(FLOAT_TO_INT_BITS, thisValue),
                            equals.invokeStaticMethod(FLOAT_TO_INT_BITS, thatValue))
                            .falseBranch().returnBoolean(false);
                    break;
                case "D": // double
                    equals.ifZero(equals.compareLong(equals.invokeStaticMethod(DOUBLE_TO_LONG_BITS, thisValue),
                            equals.invokeStaticMethod(DOUBLE_TO_LONG_BITS, thatValue)))
                            .falseBranch().returnBoolean(false);
                    break;
                case "[Z": // boolean[]
                    equals.ifTrue(equals.invokeStaticMethod(BOOLEAN_ARRAY_EQUALS, thisValue, thatValue))
                            .falseBranch().returnBoolean(false);
                    break;
                case "[B": // byte[]
                    equals.ifTrue(equals.invokeStaticMethod(BYTE_ARRAY_EQUALS, thisValue, thatValue))
                            .falseBranch().returnBoolean(false);
                    break;
                case "[S": // short[]
                    equals.ifTrue(equals.invokeStaticMethod(SHORT_ARRAY_EQUALS, thisValue, thatValue))
                            .falseBranch().returnBoolean(false);
                    break;
                case "[I": // int[]
                    equals.ifTrue(equals.invokeStaticMethod(INT_ARRAY_EQUALS, thisValue, thatValue))
                            .falseBranch().returnBoolean(false);
                    break;
                case "[J": // long[]
                    equals.ifTrue(equals.invokeStaticMethod(LONG_ARRAY_EQUALS, thisValue, thatValue))
                            .falseBranch().returnBoolean(false);
                    break;
                case "[F": // float[]
                    equals.ifTrue(equals.invokeStaticMethod(FLOAT_ARRAY_EQUALS, thisValue, thatValue))
                            .falseBranch().returnBoolean(false);
                    break;
                case "[D": // double[]
                    equals.ifTrue(equals.invokeStaticMethod(DOUBLE_ARRAY_EQUALS, thisValue, thatValue))
                            .falseBranch().returnBoolean(false);
                    break;
                case "[C": // char[]
                    equals.ifTrue(equals.invokeStaticMethod(CHAR_ARRAY_EQUALS, thisValue, thatValue))
                            .falseBranch().returnBoolean(false);
                    break;
                default:
                    if (field.getType().startsWith("L")) {
                        // Object (String, Class, enum, nested annotation)
                        equals.ifTrue(equals.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS, thisValue, thatValue))
                                .falseBranch().returnBoolean(false);
                    } else if (field.getType().startsWith("[L")) {
                        // Object[]
                        equals.ifTrue(equals.invokeStaticMethod(OBJECT_ARRAY_EQUALS, thisValue, thatValue))
                                .falseBranch().returnBoolean(false);
                    } else {
                        // multidimensional array is not a valid annotation member
                        throw new IllegalArgumentException("Invalid annotation member: " + field);
                    }
                    break;
            }
        }

        equals.returnBoolean(true);
    }

    private static void generateHashCode(ClassCreator clazz, AnnotationLiteralClassInfo literal) {
        MethodCreator hashCode = clazz.getMethodCreator("hashCode", int.class);

        if (literal.annotationMembers().isEmpty()) {
            // short-circuit for memberless annotations
            hashCode.returnInt(0);
        }

        AssignableResultHandle result = hashCode.createVariable(int.class);
        hashCode.assign(result, hashCode.load(0));

        for (MethodInfo annotationMember : literal.annotationMembers()) {
            // we could even precompute the member name hash code statically...
            ResultHandle memberName = hashCode.load(annotationMember.name());
            ResultHandle memberNameHash = hashCode.multiply(
                    hashCode.load(127),
                    hashCode.invokeVirtualMethod(MethodDescriptors.OBJECT_HASH_CODE, memberName));

            String type = annotationMember.returnType().name().toString();

            // can read directly from the field, that's what the method also does
            FieldDescriptor field = FieldDescriptor.of(clazz.getClassName(), annotationMember.name(), type);
            ResultHandle value = hashCode.readInstanceField(field, hashCode.getThis());

            ResultHandle memberValueHash;
            switch (field.getType()) {
                case "Z": // boolean
                    memberValueHash = hashCode.invokeStaticMethod(BOOLEAN_HASH_CODE, value);
                    break;
                case "B": // byte
                    memberValueHash = hashCode.invokeStaticMethod(BYTE_HASH_CODE, value);
                    break;
                case "S": // short
                    memberValueHash = hashCode.invokeStaticMethod(SHORT_HASH_CODE, value);
                    break;
                case "I": // int
                    memberValueHash = hashCode.invokeStaticMethod(INT_HASH_CODE, value);
                    break;
                case "J": // long
                    memberValueHash = hashCode.invokeStaticMethod(LONG_HASH_CODE, value);
                    break;
                case "F": // float
                    memberValueHash = hashCode.invokeStaticMethod(FLOAT_HASH_CODE, value);
                    break;
                case "D": // double
                    memberValueHash = hashCode.invokeStaticMethod(DOUBLE_HASH_CODE, value);
                    break;
                case "C": // char
                    memberValueHash = hashCode.invokeStaticMethod(CHAR_HASH_CODE, value);
                    break;
                case "[Z": // boolean[]
                    memberValueHash = hashCode.invokeStaticMethod(BOOLEAN_ARRAY_HASH_CODE, value);
                    break;
                case "[B": // byte[]
                    memberValueHash = hashCode.invokeStaticMethod(BYTE_ARRAY_HASH_CODE, value);
                    break;
                case "[S": // short[]
                    memberValueHash = hashCode.invokeStaticMethod(SHORT_ARRAY_HASH_CODE, value);
                    break;
                case "[I": // int[]
                    memberValueHash = hashCode.invokeStaticMethod(INT_ARRAY_HASH_CODE, value);
                    break;
                case "[J": // long[]
                    memberValueHash = hashCode.invokeStaticMethod(LONG_ARRAY_HASH_CODE, value);
                    break;
                case "[F": // float[]
                    memberValueHash = hashCode.invokeStaticMethod(FLOAT_ARRAY_HASH_CODE, value);
                    break;
                case "[D": // double[]
                    memberValueHash = hashCode.invokeStaticMethod(DOUBLE_ARRAY_HASH_CODE, value);
                    break;
                case "[C": // char[]
                    memberValueHash = hashCode.invokeStaticMethod(CHAR_ARRAY_HASH_CODE, value);
                    break;
                default:
                    if (field.getType().startsWith("L")) {
                        // Object (String, Class, enum, nested annotation)
                        memberValueHash = hashCode.invokeVirtualMethod(MethodDescriptors.OBJECT_HASH_CODE, value);
                    } else if (field.getType().startsWith("[L")) {
                        // Object[]
                        memberValueHash = hashCode.invokeStaticMethod(OBJECT_ARRAY_HASH_CODE, value);
                    } else {
                        // multidimensional array is not a valid annotation member
                        throw new IllegalArgumentException("Invalid annotation member: " + field);
                    }
                    break;
            }

            ResultHandle xor = hashCode.bitwiseXor(memberNameHash, memberValueHash);
            hashCode.assign(result, hashCode.add(result, xor));
        }

        hashCode.returnValue(result);
    }

    // CDI's `AnnotationLiteral` has special cases for `String` and `Class` values
    // and wraps arrays into "{...}" instead of "[...]", but that's not necessary
    private static void generateToString(ClassCreator clazz, AnnotationLiteralClassInfo literal) {
        MethodCreator toString = clazz.getMethodCreator("toString", String.class);

        if (literal.annotationMembers().isEmpty()) {
            // short-circuit for memberless annotations
            toString.returnValue(toString.load("@" + literal.annotationClass.name().toString() + "()"));
        }

        Gizmo.StringBuilderGenerator str = Gizmo.newStringBuilder(toString);

        str.append('@' + literal.annotationClass.name().toString() + '(');

        boolean first = true;
        for (MethodInfo annotationMember : literal.annotationMembers()) {
            if (first) {
                str.append(annotationMember.name() + "=");
            } else {
                str.append(", " + annotationMember.name() + "=");
            }

            String type = annotationMember.returnType().name().toString();
            FieldDescriptor field = FieldDescriptor.of(clazz.getClassName(), annotationMember.name(), type);

            ResultHandle value = toString.readInstanceField(field, toString.getThis());
            switch (field.getType()) {
                case "[Z": // boolean[]
                    str.append(toString.invokeStaticMethod(BOOLEAN_ARRAY_TO_STRING, value));
                    break;
                case "[B": // byte[]
                    str.append(toString.invokeStaticMethod(BYTE_ARRAY_TO_STRING, value));
                    break;
                case "[S": // short[]
                    str.append(toString.invokeStaticMethod(SHORT_ARRAY_TO_STRING, value));
                    break;
                case "[I": // int[]
                    str.append(toString.invokeStaticMethod(INT_ARRAY_TO_STRING, value));
                    break;
                case "[J": // long[]
                    str.append(toString.invokeStaticMethod(LONG_ARRAY_TO_STRING, value));
                    break;
                case "[F": // float[]
                    str.append(toString.invokeStaticMethod(FLOAT_ARRAY_TO_STRING, value));
                    break;
                case "[D": // double[]
                    str.append(toString.invokeStaticMethod(DOUBLE_ARRAY_TO_STRING, value));
                    break;
                case "[C": // char[]
                    str.append(toString.invokeStaticMethod(CHAR_ARRAY_TO_STRING, value));
                    break;
                default:
                    if (field.getType().startsWith("[L")) {
                        // Object[]
                        str.append(toString.invokeStaticMethod(OBJECT_ARRAY_TO_STRING, value));
                    } else if (field.getType().startsWith("[[")) {
                        // multidimensional array is not a valid annotation member
                        throw new IllegalArgumentException("Invalid annotation member: " + field);
                    } else {
                        // not an array, that is, any primitive or Object
                        str.append(value);
                    }
                    break;
            }

            first = false;
        }

        str.append(')');
        toString.returnValue(str.callToString());
    }
}
