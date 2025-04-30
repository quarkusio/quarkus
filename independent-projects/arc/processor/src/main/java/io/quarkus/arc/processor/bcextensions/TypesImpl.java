package io.quarkus.arc.processor.bcextensions;

import java.util.Arrays;

import jakarta.enterprise.inject.build.compatible.spi.Types;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.types.ArrayType;
import jakarta.enterprise.lang.model.types.ClassType;
import jakarta.enterprise.lang.model.types.ParameterizedType;
import jakarta.enterprise.lang.model.types.PrimitiveType;
import jakarta.enterprise.lang.model.types.Type;
import jakarta.enterprise.lang.model.types.VoidType;
import jakarta.enterprise.lang.model.types.WildcardType;

import org.jboss.jandex.DotName;

class TypesImpl implements Types {
    private final org.jboss.jandex.IndexView jandexIndex;
    private final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;

    TypesImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlay = annotationOverlay;
    }

    @Override
    public Type of(Class<?> clazz) {
        if (clazz.isArray()) {
            int dimensions = 1;
            Class<?> componentType = clazz.getComponentType();
            while (componentType.isArray()) {
                dimensions++;
                componentType = componentType.getComponentType();
            }
            return ofArray(of(componentType), dimensions);
        }

        if (clazz.isPrimitive()) {
            if (clazz == Void.TYPE) {
                return ofVoid();
            } else if (clazz == Boolean.TYPE) {
                return ofPrimitive(PrimitiveType.PrimitiveKind.BOOLEAN);
            } else if (clazz == Byte.TYPE) {
                return ofPrimitive(PrimitiveType.PrimitiveKind.BYTE);
            } else if (clazz == Short.TYPE) {
                return ofPrimitive(PrimitiveType.PrimitiveKind.SHORT);
            } else if (clazz == Integer.TYPE) {
                return ofPrimitive(PrimitiveType.PrimitiveKind.INT);
            } else if (clazz == Long.TYPE) {
                return ofPrimitive(PrimitiveType.PrimitiveKind.LONG);
            } else if (clazz == Float.TYPE) {
                return ofPrimitive(PrimitiveType.PrimitiveKind.FLOAT);
            } else if (clazz == Double.TYPE) {
                return ofPrimitive(PrimitiveType.PrimitiveKind.DOUBLE);
            } else if (clazz == Character.TYPE) {
                return ofPrimitive(PrimitiveType.PrimitiveKind.CHAR);
            } else {
                throw new IllegalArgumentException("Unknown primitive type " + clazz);
            }
        }

        org.jboss.jandex.Type jandexType = org.jboss.jandex.Type.create(DotName.createSimple(clazz.getName()),
                org.jboss.jandex.Type.Kind.CLASS);
        return new ClassTypeImpl(jandexIndex, annotationOverlay, jandexType.asClassType());

    }

    @Override
    public VoidType ofVoid() {
        org.jboss.jandex.Type jandexType = org.jboss.jandex.Type.create(DotName.createSimple("void"),
                org.jboss.jandex.Type.Kind.VOID);
        return new VoidTypeImpl(jandexIndex, annotationOverlay, jandexType.asVoidType());
    }

    @Override
    public PrimitiveType ofPrimitive(PrimitiveType.PrimitiveKind kind) {
        org.jboss.jandex.Type jandexType = org.jboss.jandex.Type.create(DotName.createSimple(kind.name().toLowerCase()),
                org.jboss.jandex.Type.Kind.PRIMITIVE);
        return new PrimitiveTypeImpl(jandexIndex, annotationOverlay, jandexType.asPrimitiveType());
    }

    @Override
    public ClassType ofClass(ClassInfo clazz) {
        org.jboss.jandex.Type jandexType = org.jboss.jandex.Type.create(((ClassInfoImpl) clazz).jandexDeclaration.name(),
                org.jboss.jandex.Type.Kind.CLASS);
        return new ClassTypeImpl(jandexIndex, annotationOverlay, jandexType.asClassType());
    }

    @Override
    public ClassType ofClass(String name) {
        DotName className = DotName.createSimple(name);
        org.jboss.jandex.ClassInfo jandexClass = jandexIndex.getClassByName(className);
        if (jandexClass == null) {
            return null;
        }
        org.jboss.jandex.Type jandexType = org.jboss.jandex.Type.create(className, org.jboss.jandex.Type.Kind.CLASS);
        return new ClassTypeImpl(jandexIndex, annotationOverlay, jandexType.asClassType());
    }

    @Override
    public ArrayType ofArray(Type componentType, int dimensions) {
        org.jboss.jandex.ArrayType jandexType = org.jboss.jandex.ArrayType.create(((TypeImpl<?>) componentType).jandexType,
                dimensions);
        return new ArrayTypeImpl(jandexIndex, annotationOverlay, jandexType);
    }

    @Override
    public ParameterizedType parameterized(Class<?> genericType, Class<?>... typeArguments) {
        DotName genericTypeName = DotName.createSimple(genericType.getName());
        Type[] transformedTypeArguments = Arrays.stream(typeArguments).map(this::of).toArray(Type[]::new);
        return parameterizedType(genericTypeName, transformedTypeArguments);
    }

    @Override
    public ParameterizedType parameterized(Class<?> genericType, Type... typeArguments) {
        DotName genericTypeName = DotName.createSimple(genericType.getName());
        return parameterizedType(genericTypeName, typeArguments);
    }

    @Override
    public ParameterizedType parameterized(ClassType genericType, Type... typeArguments) {
        DotName genericTypeName = ((TypeImpl<?>) genericType).jandexType.name();
        return parameterizedType(genericTypeName, typeArguments);
    }

    private ParameterizedType parameterizedType(DotName genericTypeName, Type... typeArguments) {
        org.jboss.jandex.Type[] jandexTypeArguments = Arrays.stream(typeArguments)
                .map(it -> ((TypeImpl<?>) it).jandexType)
                .toArray(org.jboss.jandex.Type[]::new);

        org.jboss.jandex.ParameterizedType jandexType = org.jboss.jandex.ParameterizedType.create(genericTypeName,
                jandexTypeArguments, null);
        return new ParameterizedTypeImpl(jandexIndex, annotationOverlay, jandexType);
    }

    @Override
    public WildcardType wildcardWithUpperBound(Type upperBound) {
        org.jboss.jandex.WildcardType jandexType = org.jboss.jandex.WildcardType
                .createUpperBound(((TypeImpl<?>) upperBound).jandexType);
        return new WildcardTypeImpl(jandexIndex, annotationOverlay, jandexType);
    }

    @Override
    public WildcardType wildcardWithLowerBound(Type lowerBound) {
        org.jboss.jandex.WildcardType jandexType = org.jboss.jandex.WildcardType
                .createLowerBound(((TypeImpl<?>) lowerBound).jandexType);
        return new WildcardTypeImpl(jandexIndex, annotationOverlay, jandexType);
    }

    @Override
    public WildcardType wildcardUnbounded() {
        org.jboss.jandex.WildcardType jandexType = org.jboss.jandex.WildcardType.UNBOUNDED;
        return new WildcardTypeImpl(jandexIndex, annotationOverlay, jandexType);
    }
}
