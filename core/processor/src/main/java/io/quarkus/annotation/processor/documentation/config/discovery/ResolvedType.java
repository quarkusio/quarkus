package io.quarkus.annotation.processor.documentation.config.discovery;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public record ResolvedType(
        TypeMirror wrapperType,
        TypeMirror unwrappedType,
        String binaryName,
        String qualifiedName,
        String simplifiedName,
        boolean isPrimitive,
        boolean isMap,
        boolean isList,
        boolean isOptional,
        boolean isDeclared,
        boolean isInterface,
        boolean isClass,
        boolean isEnum,
        boolean isDuration,
        boolean isConfigGroup) {

    public TypeElement unwrappedTypeElement() {
        if (!isDeclared) {
            throw new IllegalStateException("Unable to get element as unwrappedType is not a DeclaredType: " + unwrappedType);
        }

        return (TypeElement) ((DeclaredType) unwrappedType).asElement();
    }

    @Override
    public final String toString() {
        return unwrappedType.toString();
    }

    public static ResolvedType ofPrimitive(TypeMirror unwrappedType, String typeName) {
        return new ResolvedType(unwrappedType, unwrappedType, typeName, typeName, typeName, true, false, false,
                false, false, false, false, false, false, false);
    }

    public static ResolvedType ofDeclaredType(TypeMirror type, String binaryName,
            String qualifiedName, String simpleName,
            boolean isInterface, boolean isClass, boolean isEnum, boolean isDuration, boolean isConfigGroup) {
        return new ResolvedType(type, type, binaryName, qualifiedName, simpleName, false, false, false, false, true,
                isInterface, isClass, isEnum, isDuration, isConfigGroup);
    }

    public static ResolvedType makeList(TypeMirror type, ResolvedType unwrappedResolvedType) {
        return new ResolvedType(type, unwrappedResolvedType.unwrappedType,
                unwrappedResolvedType.binaryName, unwrappedResolvedType.qualifiedName, unwrappedResolvedType.simplifiedName,
                unwrappedResolvedType.isPrimitive,
                unwrappedResolvedType.isMap, true,
                unwrappedResolvedType.isOptional,
                unwrappedResolvedType.isDeclared, unwrappedResolvedType.isInterface, unwrappedResolvedType.isClass,
                unwrappedResolvedType.isEnum, unwrappedResolvedType.isDuration, unwrappedResolvedType.isConfigGroup);
    }

    public static ResolvedType makeOptional(ResolvedType unwrappedResolvedType) {
        return new ResolvedType(unwrappedResolvedType.wrapperType, unwrappedResolvedType.unwrappedType,
                unwrappedResolvedType.binaryName, unwrappedResolvedType.qualifiedName, unwrappedResolvedType.simplifiedName,
                unwrappedResolvedType.isPrimitive,
                unwrappedResolvedType.isMap, unwrappedResolvedType.isList,
                true,
                unwrappedResolvedType.isDeclared, unwrappedResolvedType.isInterface, unwrappedResolvedType.isClass,
                unwrappedResolvedType.isEnum, unwrappedResolvedType.isDuration, unwrappedResolvedType.isConfigGroup);
    }

    public static ResolvedType makeMap(TypeMirror type, ResolvedType unwrappedResolvedType) {
        return new ResolvedType(type, unwrappedResolvedType.unwrappedType,
                unwrappedResolvedType.binaryName, unwrappedResolvedType.qualifiedName, unwrappedResolvedType.simplifiedName,
                unwrappedResolvedType.isPrimitive,
                true, false,
                unwrappedResolvedType.isOptional,
                unwrappedResolvedType.isDeclared, unwrappedResolvedType.isInterface, unwrappedResolvedType.isClass,
                unwrappedResolvedType.isEnum, unwrappedResolvedType.isDuration, unwrappedResolvedType.isConfigGroup);
    }
}