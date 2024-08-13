package io.quarkus.annotation.processor.documentation.config.discovery;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import io.quarkus.annotation.processor.documentation.config.util.TypeUtil;

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

    public static ResolvedType ofPrimitive(TypeMirror unwrappedType) {
        String primitiveName = unwrappedType.toString();

        return new ResolvedType(unwrappedType, unwrappedType, primitiveName, primitiveName, primitiveName, true, false, false,
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
                true, unwrappedResolvedType.isList,
                unwrappedResolvedType.isOptional
                        // backwards compatibility with versions before Quarkus 3.14
                        // see https://github.com/quarkusio/quarkus/issues/42505
                        || "java.lang.String".equals(unwrappedResolvedType.qualifiedName)
                        || TypeUtil.isPrimitiveWrapper(unwrappedResolvedType.qualifiedName),
                unwrappedResolvedType.isDeclared, unwrappedResolvedType.isInterface, unwrappedResolvedType.isClass,
                unwrappedResolvedType.isEnum, unwrappedResolvedType.isDuration, unwrappedResolvedType.isConfigGroup);
    }
}