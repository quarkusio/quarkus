package io.quarkus.arc.processor;

import io.quarkus.arc.impl.ComputingCache;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import jakarta.enterprise.util.AnnotationLiteral;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;

/**
 * Handles generating bytecode for annotation literals. The
 * {@link #create(BytecodeCreator, ClassInfo, AnnotationInstance) create()} method can be used
 * to generate a bytecode sequence for instantiating an annotation literal.
 * <p>
 * Behind the scenes, for each annotation literal, its class is also generated. This class
 * is a subclass of {@link AnnotationLiteral} and hence can be used at runtime. The generated
 * annotation literal classes are shared. That is, one class is generated for each annotation
 * type and the constructor of that class accepts values of all annotation members.
 * <p>
 * This construct is thread-safe.
 */
public class AnnotationLiteralProcessor {
    private static final String ANNOTATION_LITERAL_SUFFIX = "_Shared_AnnotationLiteral";

    private final ComputingCache<CacheKey, AnnotationLiteralClassInfo> cache;
    private final IndexView beanArchiveIndex;

    AnnotationLiteralProcessor(IndexView beanArchiveIndex, Predicate<DotName> applicationClassPredicate) {
        this.cache = new ComputingCache<>(key -> new AnnotationLiteralClassInfo(
                generateAnnotationLiteralClassName(key.annotationName()),
                applicationClassPredicate.test(key.annotationName()),
                key.annotationClass));
        this.beanArchiveIndex = beanArchiveIndex;
    }

    boolean hasLiteralsToGenerate() {
        return !cache.isEmpty();
    }

    ComputingCache<CacheKey, AnnotationLiteralClassInfo> getCache() {
        return cache;
    }

    /**
     * @deprecated annotation literal sharing is now always enabled, this method is superseded
     *             by {@link #create(BytecodeCreator, ClassInfo, AnnotationInstance)} and will be removed
     *             at some time after Quarkus 3.0
     */
    @Deprecated
    public ResultHandle process(BytecodeCreator bytecode, ClassOutput classOutput, ClassInfo annotationClass,
            AnnotationInstance annotationInstance, String targetPackage) {
        return create(bytecode, annotationClass, annotationInstance);
    }

    /**
     * Generates a bytecode sequence to create an instance of given annotation type, such that
     * the annotation members have the same values as the given annotation instance.
     * An implementation of the annotation type will be generated automatically, subclassing
     * the {@code AnnotationLiteral} class. Therefore, we also call it the annotation literal class.
     *
     * @param bytecode will receive the bytecode sequence for instantiating the annotation literal class
     *        as a sequence of {@link BytecodeCreator} method calls
     * @param annotationClass the annotation type
     * @param annotationInstance the annotation instance; must match the {@code annotationClass}
     * @return an annotation literal instance result handle
     */
    public ResultHandle create(BytecodeCreator bytecode, ClassInfo annotationClass, AnnotationInstance annotationInstance) {
        Objects.requireNonNull(annotationClass, "Annotation class not available: " + annotationInstance);
        AnnotationLiteralClassInfo literal = cache.getValue(new CacheKey(annotationClass));

        ResultHandle[] constructorParameters = new ResultHandle[literal.annotationMembers().size()];

        int constructorParameterIndex = 0;
        for (MethodInfo annotationMember : literal.annotationMembers()) {
            AnnotationValue value = annotationInstance.value(annotationMember.name());
            if (value == null) {
                value = annotationMember.defaultValue();
            }
            if (value == null) {
                throw new IllegalStateException(String.format(
                        "Value is not set for %s.%s(). Most probably an older version of Jandex was used to index an application dependency. Make sure that Jandex 2.1+ is used.",
                        annotationMember.declaringClass().name(), annotationMember.name()));
            }
            ResultHandle retValue = loadValue(bytecode, literal, annotationMember, value);
            constructorParameters[constructorParameterIndex] = retValue;

            constructorParameterIndex++;
        }

        return bytecode.newInstance(MethodDescriptor.ofConstructor(literal.generatedClassName,
                literal.annotationMembers().stream().map(m -> m.returnType().name().toString()).toArray()),
                constructorParameters);
    }

    /**
     * Generates a bytecode sequence to load given annotation member value.
     *
     * @param bytecode will receive the bytecode sequence for loading the annotation member value
     *        as a sequence of {@link BytecodeCreator} method calls
     * @param literal data about the annotation literal class currently being generated
     * @param annotationMember the annotation member whose value we're loading
     * @param annotationMemberValue the annotation member value we're loading
     * @return an annotation member value result handle
     */
    private ResultHandle loadValue(BytecodeCreator bytecode, AnnotationLiteralClassInfo literal,
            MethodInfo annotationMember, AnnotationValue annotationMemberValue) {
        ResultHandle retValue;
        switch (annotationMemberValue.kind()) {
            case BOOLEAN:
                retValue = bytecode.load(annotationMemberValue.asBoolean());
                break;
            case BYTE:
                retValue = bytecode.load(annotationMemberValue.asByte());
                break;
            case SHORT:
                retValue = bytecode.load(annotationMemberValue.asShort());
                break;
            case INTEGER:
                retValue = bytecode.load(annotationMemberValue.asInt());
                break;
            case LONG:
                retValue = bytecode.load(annotationMemberValue.asLong());
                break;
            case FLOAT:
                retValue = bytecode.load(annotationMemberValue.asFloat());
                break;
            case DOUBLE:
                retValue = bytecode.load(annotationMemberValue.asDouble());
                break;
            case CHARACTER:
                retValue = bytecode.load(annotationMemberValue.asChar());
                break;
            case STRING:
                retValue = bytecode.load(annotationMemberValue.asString());
                break;
            case ENUM:
                retValue = bytecode.readStaticField(FieldDescriptor.of(annotationMemberValue.asEnumType().toString(),
                        annotationMemberValue.asEnum(), annotationMemberValue.asEnumType().toString()));
                break;
            case CLASS:
                if (annotationMemberValue.equals(annotationMember.defaultValue())) {
                    retValue = bytecode.readStaticField(FieldDescriptor.of(literal.generatedClassName,
                            AnnotationLiteralGenerator.defaultValueStaticFieldName(annotationMember),
                            annotationMember.returnType().name().toString()));
                } else {
                    retValue = bytecode.loadClass(annotationMemberValue.asClass().name().toString());
                }
                break;
            case NESTED:
                AnnotationInstance nestedAnnotation = annotationMemberValue.asNested();
                DotName annotationName = nestedAnnotation.name();
                ClassInfo annotationClass = beanArchiveIndex.getClassByName(annotationName);
                if (annotationClass == null) {
                    throw new IllegalStateException("Class of nested annotation " + nestedAnnotation + " missing");
                }
                retValue = create(bytecode, annotationClass, nestedAnnotation);
                break;
            case ARRAY:
                retValue = loadArrayValue(bytecode, literal, annotationMember, annotationMemberValue);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported value: " + annotationMemberValue);
        }
        return retValue;
    }

    /**
     * Generates a bytecode sequence to load given array-typed annotation member value.
     *
     * @param bytecode will receive the bytecode sequence for loading the annotation member value
     *        as a sequence of {@link BytecodeCreator} method calls
     * @param literal data about the annotation literal class currently being generated
     * @param annotationMember the annotation member whose value we're loading
     * @param annotationMemberValue the annotation member value we're loading
     * @return an annotation member value result handle
     */
    private ResultHandle loadArrayValue(BytecodeCreator bytecode, AnnotationLiteralClassInfo literal,
            MethodInfo annotationMember, AnnotationValue annotationMemberValue) {
        ResultHandle retValue;
        AnnotationValue.Kind componentKind = annotationMemberValue.componentKind();
        switch (componentKind) {
            case BOOLEAN:
                boolean[] booleanArray = annotationMemberValue.asBooleanArray();
                retValue = bytecode.newArray(componentType(annotationMember), booleanArray.length);
                for (int i = 0; i < booleanArray.length; i++) {
                    bytecode.writeArrayValue(retValue, i, bytecode.load(booleanArray[i]));
                }
                break;
            case BYTE:
                byte[] byteArray = annotationMemberValue.asByteArray();
                retValue = bytecode.newArray(componentType(annotationMember), byteArray.length);
                for (int i = 0; i < byteArray.length; i++) {
                    bytecode.writeArrayValue(retValue, i, bytecode.load(byteArray[i]));
                }
                break;
            case SHORT:
                short[] shortArray = annotationMemberValue.asShortArray();
                retValue = bytecode.newArray(componentType(annotationMember), shortArray.length);
                for (int i = 0; i < shortArray.length; i++) {
                    bytecode.writeArrayValue(retValue, i, bytecode.load(shortArray[i]));
                }
                break;
            case INTEGER:
                int[] intArray = annotationMemberValue.asIntArray();
                retValue = bytecode.newArray(componentType(annotationMember), intArray.length);
                for (int i = 0; i < intArray.length; i++) {
                    bytecode.writeArrayValue(retValue, i, bytecode.load(intArray[i]));
                }
                break;
            case LONG:
                long[] longArray = annotationMemberValue.asLongArray();
                retValue = bytecode.newArray(componentType(annotationMember), longArray.length);
                for (int i = 0; i < longArray.length; i++) {
                    bytecode.writeArrayValue(retValue, i, bytecode.load(longArray[i]));
                }
                break;
            case FLOAT:
                float[] floatArray = annotationMemberValue.asFloatArray();
                retValue = bytecode.newArray(componentType(annotationMember), floatArray.length);
                for (int i = 0; i < floatArray.length; i++) {
                    bytecode.writeArrayValue(retValue, i, bytecode.load(floatArray[i]));
                }
                break;
            case DOUBLE:
                double[] doubleArray = annotationMemberValue.asDoubleArray();
                retValue = bytecode.newArray(componentType(annotationMember), doubleArray.length);
                for (int i = 0; i < doubleArray.length; i++) {
                    bytecode.writeArrayValue(retValue, i, bytecode.load(doubleArray[i]));
                }
                break;
            case CHARACTER:
                char[] charArray = annotationMemberValue.asCharArray();
                retValue = bytecode.newArray(componentType(annotationMember), charArray.length);
                for (int i = 0; i < charArray.length; i++) {
                    bytecode.writeArrayValue(retValue, i, bytecode.load(charArray[i]));
                }
                break;
            case STRING:
                String[] stringArray = annotationMemberValue.asStringArray();
                retValue = bytecode.newArray(componentType(annotationMember), stringArray.length);
                for (int i = 0; i < stringArray.length; i++) {
                    bytecode.writeArrayValue(retValue, i, bytecode.load(stringArray[i]));
                }
                break;
            case ENUM:
                String[] enumArray = annotationMemberValue.asEnumArray();
                DotName[] enumTypeArray = annotationMemberValue.asEnumTypeArray();
                retValue = bytecode.newArray(componentType(annotationMember), enumArray.length);
                for (int i = 0; i < enumArray.length; i++) {
                    ResultHandle enumValue = bytecode.readStaticField(FieldDescriptor.of(
                            enumTypeArray[i].toString(), enumArray[i], enumTypeArray[i].toString()));
                    bytecode.writeArrayValue(retValue, i, enumValue);
                }
                break;
            case CLASS:
                if (annotationMemberValue.equals(annotationMember.defaultValue())) {
                    retValue = bytecode.readStaticField(FieldDescriptor.of(literal.generatedClassName,
                            AnnotationLiteralGenerator.defaultValueStaticFieldName(annotationMember),
                            annotationMember.returnType().name().toString()));
                } else {
                    Type[] classArray = annotationMemberValue.asClassArray();
                    retValue = bytecode.newArray(componentType(annotationMember), classArray.length);
                    for (int i = 0; i < classArray.length; i++) {
                        bytecode.writeArrayValue(retValue, i, bytecode.loadClass(classArray[i].name().toString()));
                    }
                }
                break;
            case NESTED:
                AnnotationInstance[] nestedArray = annotationMemberValue.asNestedArray();
                retValue = bytecode.newArray(componentType(annotationMember), nestedArray.length);
                for (int i = 0; i < nestedArray.length; i++) {
                    AnnotationInstance nestedAnnotation = nestedArray[i];
                    DotName annotationName = nestedAnnotation.name();
                    ClassInfo annotationClass = beanArchiveIndex.getClassByName(annotationName);
                    if (annotationClass == null) {
                        throw new IllegalStateException("Class of nested annotation " + nestedAnnotation + " missing");
                    }
                    ResultHandle nestedAnnotationValue = create(bytecode, annotationClass, nestedAnnotation);

                    bytecode.writeArrayValue(retValue, i, nestedAnnotationValue);
                }
                break;
            case UNKNOWN: // empty array
                DotName componentName = componentTypeName(annotationMember);
                // Use empty array constants for common component kinds
                if (PrimitiveType.BOOLEAN.name().equals(componentName)) {
                    retValue = bytecode.readStaticField(FieldDescriptors.ANNOTATION_LITERALS_EMPTY_BOOLEAN_ARRAY);
                } else if (PrimitiveType.BYTE.name().equals(componentName)) {
                    retValue = bytecode.readStaticField(FieldDescriptors.ANNOTATION_LITERALS_EMPTY_BYTE_ARRAY);
                } else if (PrimitiveType.SHORT.name().equals(componentName)) {
                    retValue = bytecode.readStaticField(FieldDescriptors.ANNOTATION_LITERALS_EMPTY_SHORT_ARRAY);
                } else if (PrimitiveType.INT.name().equals(componentName)) {
                    retValue = bytecode.readStaticField(FieldDescriptors.ANNOTATION_LITERALS_EMPTY_INT_ARRAY);
                } else if (PrimitiveType.LONG.name().equals(componentName)) {
                    retValue = bytecode.readStaticField(FieldDescriptors.ANNOTATION_LITERALS_EMPTY_LONG_ARRAY);
                } else if (PrimitiveType.FLOAT.name().equals(componentName)) {
                    retValue = bytecode.readStaticField(FieldDescriptors.ANNOTATION_LITERALS_EMPTY_FLOAT_ARRAY);
                } else if (PrimitiveType.DOUBLE.name().equals(componentName)) {
                    retValue = bytecode.readStaticField(FieldDescriptors.ANNOTATION_LITERALS_EMPTY_DOUBLE_ARRAY);
                } else if (PrimitiveType.CHAR.name().equals(componentName)) {
                    retValue = bytecode.readStaticField(FieldDescriptors.ANNOTATION_LITERALS_EMPTY_CHAR_ARRAY);
                } else if (DotNames.STRING.equals(componentName)) {
                    retValue = bytecode.readStaticField(FieldDescriptors.ANNOTATION_LITERALS_EMPTY_STRING_ARRAY);
                } else if (DotNames.CLASS.equals(componentName)) {
                    retValue = bytecode.readStaticField(FieldDescriptors.ANNOTATION_LITERALS_EMPTY_CLASS_ARRAY);
                } else {
                    retValue = bytecode.newArray(componentName.toString(), bytecode.load(0));
                }
                break;
            default:
                // at this point, the only possible componend kind is "array"
                throw new IllegalStateException("Array component kind is " + componentKind + ", this should never happen");
        }
        return retValue;
    }

    private static String componentType(MethodInfo method) {
        return componentTypeName(method).toString();
    }

    private static DotName componentTypeName(MethodInfo method) {
        ArrayType arrayType = method.returnType().asArrayType();
        return arrayType.component().name();
    }

    private static String generateAnnotationLiteralClassName(DotName annotationName) {
        // when the annotation is a java.lang annotation we need to use a different package in which to generate the literal
        // otherwise a security exception will be thrown when the literal is loaded
        boolean isJavaLang = annotationName.toString().startsWith("java.lang");
        String nameToUse = isJavaLang
                ? AbstractGenerator.DEFAULT_PACKAGE + annotationName.withoutPackagePrefix()
                : annotationName.toString();

        // com.foo.MyQualifier -> com.foo.MyQualifier_Shared_AnnotationLiteral
        return nameToUse + ANNOTATION_LITERAL_SUFFIX;
    }

    static class CacheKey {
        final ClassInfo annotationClass;

        CacheKey(ClassInfo annotationClass) {
            this.annotationClass = annotationClass;
        }

        DotName annotationName() {
            return annotationClass.name();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(annotationClass.name(), cacheKey.annotationClass.name());
        }

        @Override
        public int hashCode() {
            return Objects.hash(annotationClass.name());
        }
    }

    static class AnnotationLiteralClassInfo {
        /**
         * Name of the generated annotation literal class.
         */
        final String generatedClassName;
        /**
         * Whether the generated annotation literal class is an application class.
         * Only used when sharing is enabled.
         */
        final boolean isApplicationClass;

        /**
         * The annotation type. The generated annotation literal class will implement this interface
         * (and extend {@code AnnotationLiteral<this interface>}). The process that generates
         * the annotation literal class may consult this, for example, to know the set of annotation members.
         */
        final ClassInfo annotationClass;

        AnnotationLiteralClassInfo(String generatedClassName, boolean isApplicationClass, ClassInfo annotationClass) {
            this.generatedClassName = generatedClassName;
            this.isApplicationClass = isApplicationClass;
            this.annotationClass = annotationClass;
        }

        DotName annotationName() {
            return annotationClass.name();
        }

        List<MethodInfo> annotationMembers() {
            List<MethodInfo> result = new ArrayList<>();
            for (MethodInfo method : annotationClass.methods()) {
                if (!method.name().equals(Methods.CLINIT) && !method.name().equals(Methods.INIT)) {
                    result.add(method);
                }
            }
            return result;
        }
    }
}
