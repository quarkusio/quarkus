package io.quarkus.arc.processor.bcextensions;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import jakarta.enterprise.lang.model.declarations.ParameterInfo;
import jakarta.enterprise.lang.model.types.Type;
import jakarta.enterprise.lang.model.types.TypeVariable;

import org.jboss.jandex.DotName;

class MethodInfoImpl extends DeclarationInfoImpl<org.jboss.jandex.MethodInfo> implements MethodInfo {
    // only for equals/hashCode
    private final DotName className;
    private final String name;
    private final List<org.jboss.jandex.Type> parameterTypes;

    MethodInfoImpl(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays,
            org.jboss.jandex.MethodInfo jandexDeclaration) {
        super(jandexIndex, annotationOverlays, jandexDeclaration);
        this.className = jandexDeclaration.declaringClass().name();
        this.name = jandexDeclaration.name();
        this.parameterTypes = jandexDeclaration.parameterTypes();
    }

    @Override
    public String name() {
        if (isConstructor()) {
            return jandexDeclaration.declaringClass().name().toString();
        }
        return jandexDeclaration.name();
    }

    @Override
    public List<ParameterInfo> parameters() {
        List<ParameterInfo> result = new ArrayList<>(jandexDeclaration.parametersCount());
        for (org.jboss.jandex.MethodParameterInfo jandexParameter : jandexDeclaration.parameters()) {
            result.add(new ParameterInfoImpl(jandexIndex, annotationOverlays, jandexParameter));
        }
        return result;
    }

    @Override
    public Type returnType() {
        if (isConstructor()) {
            // Jandex returns a void type as a return type of a constructor,
            // but it has the correct (type use) annotations
            //
            // so we just copy those annotations to a class type
            org.jboss.jandex.AnnotationInstance[] typeAnnotations = jandexDeclaration.returnType().annotations()
                    .toArray(new org.jboss.jandex.AnnotationInstance[0]);
            org.jboss.jandex.Type classType = org.jboss.jandex.Type.createWithAnnotations(
                    jandexDeclaration.declaringClass().name(), org.jboss.jandex.Type.Kind.CLASS, typeAnnotations);
            return TypeImpl.fromJandexType(jandexIndex, annotationOverlays, classType);
        }
        return TypeImpl.fromJandexType(jandexIndex, annotationOverlays, jandexDeclaration.returnType());
    }

    @Override
    public Type receiverType() {
        // static method
        if (Modifier.isStatic(jandexDeclaration.flags())) {
            return null;
        }

        // constructor of a top-level class or a static nested class
        if (MethodPredicates.IS_CONSTRUCTOR_JANDEX.test(jandexDeclaration)) {
            org.jboss.jandex.ClassInfo declaringClass = jandexDeclaration.declaringClass();
            org.jboss.jandex.ClassInfo.NestingType nestingType = declaringClass.nestingType();
            if (nestingType == org.jboss.jandex.ClassInfo.NestingType.TOP_LEVEL) {
                return null;
            }
            if (nestingType == org.jboss.jandex.ClassInfo.NestingType.INNER
                    && Modifier.isStatic(declaringClass.flags())) {
                return null;
            }
        }

        return TypeImpl.fromJandexType(jandexIndex, annotationOverlays, jandexDeclaration.receiverType());
    }

    @Override
    public List<Type> throwsTypes() {
        return jandexDeclaration.exceptions()
                .stream()
                .map(it -> TypeImpl.fromJandexType(jandexIndex, annotationOverlays, it))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<TypeVariable> typeParameters() {
        return jandexDeclaration.typeParameters()
                .stream()
                .map(it -> TypeImpl.fromJandexType(jandexIndex, annotationOverlays, it))
                .filter(Type::isTypeVariable) // not necessary, just as a precaution
                .map(Type::asTypeVariable) // not necessary, just as a precaution
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public boolean isConstructor() {
        return MethodPredicates.IS_CONSTRUCTOR_JANDEX.test(jandexDeclaration);
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(jandexDeclaration.flags());
    }

    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(jandexDeclaration.flags());
    }

    @Override
    public boolean isFinal() {
        return Modifier.isFinal(jandexDeclaration.flags());
    }

    @Override
    public int modifiers() {
        return jandexDeclaration.flags();
    }

    @Override
    public ClassInfo declaringClass() {
        return new ClassInfoImpl(jandexIndex, annotationOverlays, jandexDeclaration.declaringClass());
    }

    @Override
    AnnotationsOverlay<org.jboss.jandex.MethodInfo> annotationsOverlay() {
        return annotationOverlays.methods;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MethodInfoImpl that = (MethodInfoImpl) o;
        return Objects.equals(className, that.className)
                && Objects.equals(name, that.name)
                && Objects.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, name, parameterTypes);
    }
}
