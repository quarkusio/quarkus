package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.build.compatible.spi.AnnotationBuilder;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.AnnotationMember;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.types.ArrayType;
import jakarta.enterprise.lang.model.types.Type;

import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.Types;

class AnnotationBuilderImpl implements AnnotationBuilder {
    private final org.jboss.jandex.IndexView jandexIndex;
    private final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;

    private final DotName jandexClassName;
    private final List<org.jboss.jandex.AnnotationValue> jandexAnnotationMembers = new ArrayList<>();

    AnnotationBuilderImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            DotName jandexAnnotationName) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlay = annotationOverlay;
        this.jandexClassName = jandexAnnotationName;
    }

    @Override
    public AnnotationBuilder member(String name, AnnotationMember value) {
        org.jboss.jandex.AnnotationValue jandexValue = ((AnnotationMemberImpl) value).jandexAnnotationMember;
        switch (jandexValue.kind()) {
            case BOOLEAN:
                return member(name, jandexValue.asBoolean());
            case BYTE:
                return member(name, jandexValue.asByte());
            case SHORT:
                return member(name, jandexValue.asShort());
            case INTEGER:
                return member(name, jandexValue.asInt());
            case LONG:
                return member(name, jandexValue.asLong());
            case FLOAT:
                return member(name, jandexValue.asFloat());
            case DOUBLE:
                return member(name, jandexValue.asDouble());
            case CHARACTER:
                return member(name, jandexValue.asChar());
            case STRING:
                return member(name, jandexValue.asString());
            case ENUM:
                DotName enumTypeName = jandexValue.asEnumType();
                String enumValue = jandexValue.asEnum();
                jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createEnumValue(name, enumTypeName, enumValue));
                return this;
            case CLASS:
                org.jboss.jandex.Type jandexClass = jandexValue.asClass();
                jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createClassValue(name, jandexClass));
                return this;
            case NESTED:
                org.jboss.jandex.AnnotationInstance jandexAnnotation = jandexValue.asNested();
                jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createNestedAnnotationValue(name,
                        jandexAnnotation));
                return this;
            case ARRAY:
                switch (jandexValue.componentKind()) {
                    case BOOLEAN:
                        return member(name, jandexValue.asBooleanArray());
                    case BYTE:
                        return member(name, jandexValue.asByteArray());
                    case SHORT:
                        return member(name, jandexValue.asShortArray());
                    case INTEGER:
                        return member(name, jandexValue.asIntArray());
                    case LONG:
                        return member(name, jandexValue.asLongArray());
                    case FLOAT:
                        return member(name, jandexValue.asFloatArray());
                    case DOUBLE:
                        return member(name, jandexValue.asDoubleArray());
                    case CHARACTER:
                        return member(name, jandexValue.asCharArray());
                    case STRING:
                        return member(name, jandexValue.asStringArray());
                    case ENUM:
                        DotName[] enumTypeNames = jandexValue.asEnumTypeArray();
                        String[] enumValues = jandexValue.asEnumArray();
                        org.jboss.jandex.AnnotationValue[] enumArray = new org.jboss.jandex.AnnotationValue[enumValues.length];
                        for (int i = 0; i < enumValues.length; i++) {
                            enumArray[i] = org.jboss.jandex.AnnotationValue.createEnumValue(name, enumTypeNames[i],
                                    enumValues[i]);
                        }
                        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, enumArray));
                        return this;
                    case CLASS:
                        org.jboss.jandex.Type[] jandexClasses = jandexValue.asClassArray();
                        org.jboss.jandex.AnnotationValue[] classArray = new org.jboss.jandex.AnnotationValue[jandexClasses.length];
                        for (int i = 0; i < jandexClasses.length; i++) {
                            classArray[i] = org.jboss.jandex.AnnotationValue.createClassValue(name, jandexClasses[i]);
                        }
                        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, classArray));
                        return this;
                    case NESTED:
                        org.jboss.jandex.AnnotationInstance[] jandexAnnotations = jandexValue.asNestedArray();
                        org.jboss.jandex.AnnotationValue[] annotationArray = new org.jboss.jandex.AnnotationValue[jandexAnnotations.length];
                        for (int i = 0; i < jandexAnnotations.length; i++) {
                            annotationArray[i] = org.jboss.jandex.AnnotationValue.createNestedAnnotationValue(name,
                                    jandexAnnotations[i]);
                        }
                        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, annotationArray));
                        return this;
                    case UNKNOWN:
                        org.jboss.jandex.AnnotationValue[] emptyArray = new org.jboss.jandex.AnnotationValue[0];
                        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, emptyArray));
                        break;
                    case ARRAY:
                        throw new IllegalStateException("Array component is array, this should never happen: " + jandexValue);
                }
                break;
            case UNKNOWN:
                throw new IllegalStateException("Unknown annotation member, this should never happen: " + jandexValue);
        }
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, boolean value) {
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createBooleanValue(name, value));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, boolean[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = org.jboss.jandex.AnnotationValue.createBooleanValue(name, values[i]);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, byte value) {
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createByteValue(name, value));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, byte[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = org.jboss.jandex.AnnotationValue.createByteValue(name, values[i]);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, short value) {
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createShortValue(name, value));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, short[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = org.jboss.jandex.AnnotationValue.createShortValue(name, values[i]);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, int value) {
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createIntegerValue(name, value));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, int[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = org.jboss.jandex.AnnotationValue.createIntegerValue(name, values[i]);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, long value) {
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createLongValue(name, value));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, long[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = org.jboss.jandex.AnnotationValue.createLongValue(name, values[i]);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, float value) {
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createFloatValue(name, value));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, float[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = org.jboss.jandex.AnnotationValue.createFloatValue(name, values[i]);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, double value) {
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createDoubleValue(name, value));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, double[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = org.jboss.jandex.AnnotationValue.createDoubleValue(name, values[i]);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, char value) {
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createCharacterValue(name, value));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, char[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = org.jboss.jandex.AnnotationValue.createCharacterValue(name, values[i]);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, String value) {
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createStringValue(name, value));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, String[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = org.jboss.jandex.AnnotationValue.createStringValue(name, values[i]);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, Enum<?> value) {
        DotName enumTypeName = DotName.createSimple(value.getDeclaringClass().getName());
        String enumValue = value.name();
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createEnumValue(name, enumTypeName, enumValue));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, Enum<?>[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            DotName enumTypeName = DotName.createSimple(values[i].getDeclaringClass().getName());
            String enumValue = values[i].name();
            array[i] = org.jboss.jandex.AnnotationValue.createEnumValue(name, enumTypeName, enumValue);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, Class<? extends Enum<?>> enumType, String enumValue) {
        DotName enumTypeName = DotName.createSimple(enumType.getName());
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createEnumValue(name, enumTypeName, enumValue));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, Class<? extends Enum<?>> enumType, String[] enumValues) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[enumValues.length];
        DotName enumTypeName = DotName.createSimple(enumType.getName());
        for (int i = 0; i < enumValues.length; i++) {
            array[i] = org.jboss.jandex.AnnotationValue.createEnumValue(name, enumTypeName, enumValues[i]);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, ClassInfo enumType, String enumValue) {
        DotName enumTypeName = ((ClassInfoImpl) enumType).jandexDeclaration.name();
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createEnumValue(name, enumTypeName, enumValue));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, ClassInfo enumType, String[] enumValues) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[enumValues.length];
        DotName enumTypeName = ((ClassInfoImpl) enumType).jandexDeclaration.name();
        for (int i = 0; i < enumValues.length; i++) {
            array[i] = org.jboss.jandex.AnnotationValue.createEnumValue(name, enumTypeName, enumValues[i]);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, Class<?> value) {
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createClassValue(name, Types.jandexType(value)));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, Class<?>[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = org.jboss.jandex.AnnotationValue.createClassValue(name, Types.jandexType(values[i]));
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, ClassInfo value) {
        DotName className = ((ClassInfoImpl) value).jandexDeclaration.name();
        org.jboss.jandex.Type jandexClass = org.jboss.jandex.Type.create(className, org.jboss.jandex.Type.Kind.CLASS);
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createClassValue(name, jandexClass));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, ClassInfo[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            DotName className = ((ClassInfoImpl) values[i]).jandexDeclaration.name();
            org.jboss.jandex.Type jandexClass = org.jboss.jandex.Type.create(className, org.jboss.jandex.Type.Kind.CLASS);
            array[i] = org.jboss.jandex.AnnotationValue.createClassValue(name, jandexClass);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    private void validateType(Type type) {
        if (type instanceof VoidTypeImpl) {
            return;
        } else if (type instanceof PrimitiveTypeImpl) {
            return;
        } else if (type instanceof ClassTypeImpl) {
            return;
        } else if (type instanceof ArrayTypeImpl) {
            ArrayType arrayType = type.asArray();
            Type elementType = arrayType.componentType();
            while (elementType.isArray()) {
                elementType = elementType.asArray().componentType();
            }
            if (elementType instanceof PrimitiveTypeImpl) {
                return;
            } else if (elementType instanceof ClassTypeImpl) {
                return;
            }
        }

        throw new IllegalArgumentException("Illegal type " + type);
    }

    @Override
    public AnnotationBuilder member(String name, Type value) {
        validateType(value);
        org.jboss.jandex.Type jandexClass = ((TypeImpl<?>) value).jandexType;
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createClassValue(name, jandexClass));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, Type[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            validateType(values[i]);
            org.jboss.jandex.Type jandexClass = ((TypeImpl<?>) values[i]).jandexType;
            array[i] = org.jboss.jandex.AnnotationValue.createClassValue(name, jandexClass);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, AnnotationInfo value) {
        org.jboss.jandex.AnnotationInstance jandexAnnotation = ((AnnotationInfoImpl) value).jandexAnnotation;
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createNestedAnnotationValue(name, jandexAnnotation));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, AnnotationInfo[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            org.jboss.jandex.AnnotationInstance jandexAnnotation = ((AnnotationInfoImpl) values[i]).jandexAnnotation;
            array[i] = org.jboss.jandex.AnnotationValue.createNestedAnnotationValue(name, jandexAnnotation);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, Annotation value) {
        org.jboss.jandex.AnnotationInstance jandexAnnotation = Annotations.jandexAnnotation(value);
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createNestedAnnotationValue(name, jandexAnnotation));
        return this;
    }

    @Override
    public AnnotationBuilder member(String name, Annotation[] values) {
        org.jboss.jandex.AnnotationValue[] array = new org.jboss.jandex.AnnotationValue[values.length];
        for (int i = 0; i < values.length; i++) {
            org.jboss.jandex.AnnotationInstance jandexAnnotation = Annotations.jandexAnnotation(values[i]);
            array[i] = org.jboss.jandex.AnnotationValue.createNestedAnnotationValue(name, jandexAnnotation);
        }
        jandexAnnotationMembers.add(org.jboss.jandex.AnnotationValue.createArrayValue(name, array));
        return this;
    }

    @Override
    public AnnotationInfo build() {
        org.jboss.jandex.ClassInfo jandexAnnotationClass = jandexIndex.getClassByName(jandexClassName);
        if (jandexAnnotationClass == null) {
            throw new IllegalStateException("Annotation class " + jandexClassName + " not present in the bean archive");
        }
        if (!jandexAnnotationClass.isAnnotation()) {
            throw new IllegalStateException("Class " + jandexClassName + " is not an annotation type");
        }
        for (org.jboss.jandex.MethodInfo jandexAnnotationMember : jandexAnnotationClass.methods()
                .stream()
                .filter(MethodPredicates.IS_METHOD_JANDEX)
                .toList()) {
            if (jandexAnnotationMember.defaultValue() != null) {
                continue;
            }
            if (jandexAnnotationMembers.stream()
                    .filter(it -> it.name().equals(jandexAnnotationMember.name()))
                    .findAny()
                    .isEmpty()) {
                throw new IllegalStateException("Annotation member " + jandexAnnotationClass.simpleName() + "."
                        + jandexAnnotationMember.name() + " not added to the AnnotationBuilder,"
                        + " and it doesn't have a default value");
            }
        }

        org.jboss.jandex.AnnotationInstance jandexAnnotation = org.jboss.jandex.AnnotationInstance.create(
                jandexClassName, null, jandexAnnotationMembers);
        return new AnnotationInfoImpl(jandexIndex, annotationOverlay, jandexAnnotation);
    }
}
