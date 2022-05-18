package io.quarkus.qute.deployment;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;

import io.quarkus.arc.processor.DotNames;

public final class Types {

    static final String JAVA_LANG_PREFIX = "java.lang.";

    static Set<Type> getTypeClosure(ClassInfo classInfo, Map<TypeVariable, Type> resolvedTypeParameters,
            IndexView index) {
        Set<Type> types = new HashSet<>();
        List<TypeVariable> typeParameters = classInfo.typeParameters();

        if (typeParameters.isEmpty() || !resolvedTypeParameters.keySet().containsAll(typeParameters)) {
            // Not a parameterized type or a raw type
            types.add(Type.create(classInfo.name(), Kind.CLASS));
        } else {
            // Canonical ParameterizedType with unresolved type variables
            Type[] typeParams = new Type[typeParameters.size()];
            for (int i = 0; i < typeParameters.size(); i++) {
                typeParams[i] = resolvedTypeParameters.get(typeParameters.get(i));
            }
            types.add(ParameterizedType.create(classInfo.name(), typeParams, null));
        }
        // Interfaces
        for (Type interfaceType : classInfo.interfaceTypes()) {
            ClassInfo interfaceClassInfo = index.getClassByName(interfaceType.name());
            if (interfaceClassInfo != null) {
                Map<TypeVariable, Type> resolved = Collections.emptyMap();
                if (Kind.PARAMETERIZED_TYPE.equals(interfaceType.kind())) {
                    resolved = buildResolvedMap(interfaceType.asParameterizedType().arguments(),
                            interfaceClassInfo.typeParameters(), resolvedTypeParameters, index);
                }
                types.addAll(getTypeClosure(interfaceClassInfo, resolved, index));
            }
        }
        // Superclass
        if (classInfo.superClassType() != null) {
            ClassInfo superClassInfo = index.getClassByName(classInfo.superName());
            if (superClassInfo != null) {
                Map<TypeVariable, Type> resolved = Collections.emptyMap();
                if (Kind.PARAMETERIZED_TYPE.equals(classInfo.superClassType().kind())) {
                    resolved = buildResolvedMap(classInfo.superClassType().asParameterizedType().arguments(),
                            superClassInfo.typeParameters(),
                            resolvedTypeParameters, index);
                }
                types.addAll(getTypeClosure(superClassInfo, resolved, index));
            }
        }
        return types;
    }

    static <T extends Type> Map<TypeVariable, Type> buildResolvedMap(List<T> resolvedArguments,
            List<TypeVariable> typeVariables,
            Map<TypeVariable, Type> resolvedTypeParameters, IndexView index) {
        Map<TypeVariable, Type> resolvedMap = new HashMap<>();
        for (int i = 0; i < resolvedArguments.size(); i++) {
            resolvedMap.put(typeVariables.get(i), resolveTypeParam(resolvedArguments.get(i), resolvedTypeParameters, index));
        }
        return resolvedMap;
    }

    static Type resolveTypeParam(Type typeParam, Map<TypeVariable, Type> resolvedTypeParameters, IndexView index) {
        if (typeParam.kind() == Kind.CLASS) {
            ClassInfo classInfo = index.getClassByName(typeParam.name());
            if (classInfo == null && !typeParam.name().toString().contains(".")) {
                // If not indexed and no package then try the java.lang prefix
                classInfo = index.getClassByName(DotName.createSimple(JAVA_LANG_PREFIX + typeParam.name().toString()));
                if (classInfo != null) {
                    return Type.create(classInfo.name(), Kind.CLASS);
                }
            }
            return typeParam;
        } else if (typeParam.kind() == Kind.TYPE_VARIABLE) {
            return resolvedTypeParameters.getOrDefault(typeParam, typeParam);
        } else if (typeParam.kind() == Kind.PARAMETERIZED_TYPE) {
            ParameterizedType parameterizedType = typeParam.asParameterizedType();
            ClassInfo classInfo = index.getClassByName(parameterizedType.name());
            if (classInfo == null && !parameterizedType.name().toString().contains(".")) {
                // If not indexed and no package then try the java.lang prefix
                classInfo = index.getClassByName(DotName.createSimple(JAVA_LANG_PREFIX + parameterizedType.name().toString()));
            }
            if (classInfo != null) {
                List<TypeVariable> typeParameters = classInfo.typeParameters();
                List<Type> arguments = parameterizedType.arguments();
                Type[] typeParams = new Type[typeParameters.size()];
                for (int i = 0; i < typeParameters.size(); i++) {
                    typeParams[i] = resolveTypeParam(arguments.get(i), resolvedTypeParameters, index);
                }
                return ParameterizedType.create(parameterizedType.name(), typeParams, null);
            }
        }
        return typeParam;
    }

    static boolean containsTypeVariable(Type type) {
        if (type.kind() == Type.Kind.TYPE_VARIABLE) {
            return true;
        }
        if (type instanceof ParameterizedType) {
            for (Type t : type.asParameterizedType().arguments()) {
                if (containsTypeVariable(t)) {
                    return true;
                }
            }
        }
        if (type.kind() == Type.Kind.ARRAY) {
            return containsTypeVariable(type.asArrayType().component());
        }
        return false;
    }

    static boolean isAssignableFrom(Type type1, Type type2, IndexView index, Map<DotName, AssignableInfo> assignableCache) {
        // TODO consider type params in assignability rules
        if (type1.kind() == Kind.ARRAY && type2.kind() == Kind.ARRAY) {
            return isAssignableFrom(type1.asArrayType().component(), type2.asArrayType().component(), index, assignableCache);
        }
        return Types.isAssignableFrom(box(type1).name(), box(type2).name(), index, assignableCache);
    }

    static class AssignableInfo {

        final Set<DotName> subclasses;
        final Set<DotName> implementors;
        final Set<DotName> extendingInterfaces;

        public AssignableInfo(Collection<ClassInfo> subclasses, Collection<ClassInfo> implementors,
                Set<DotName> extendingInterfaces) {
            this.subclasses = new HashSet<>();
            for (ClassInfo subclass : subclasses) {
                this.subclasses.add(subclass.name());
            }
            this.implementors = new HashSet<>();
            for (ClassInfo implementor : implementors) {
                this.implementors.add(implementor.name());
            }
            this.extendingInterfaces = extendingInterfaces;
        }

        boolean isAssignableFrom(DotName clazz) {
            return subclasses.contains(clazz) || implementors.contains(clazz) || extendingInterfaces.contains(clazz);
        }

    }

    static boolean isAssignableFrom(DotName class1, DotName class2, IndexView index,
            Map<DotName, AssignableInfo> assignableCache) {
        // java.lang.Object is assignable from any type
        if (class1.equals(DotNames.OBJECT)) {
            return true;
        }
        // type1 is the same as type2
        if (class1.equals(class2)) {
            return true;
        }
        AssignableInfo assignableInfo = assignableCache.get(class1);
        if (assignableInfo == null) {
            assignableInfo = new AssignableInfo(index.getAllKnownSubclasses(class1), index.getAllKnownImplementors(class1),
                    getAllInterfacesExtending(class1, index));
            assignableCache.put(class1, assignableInfo);
        }
        return assignableInfo.isAssignableFrom(class2);
    }

    static Type box(Type type) {
        if (type.kind() == Kind.PRIMITIVE) {
            return box(type.asPrimitiveType().primitive());
        }
        return type;
    }

    static Type box(Primitive primitive) {
        switch (primitive) {
            case BOOLEAN:
                return Type.create(DotNames.BOOLEAN, Kind.CLASS);
            case DOUBLE:
                return Type.create(DotNames.DOUBLE, Kind.CLASS);
            case FLOAT:
                return Type.create(DotNames.FLOAT, Kind.CLASS);
            case LONG:
                return Type.create(DotNames.LONG, Kind.CLASS);
            case INT:
                return Type.create(DotNames.INTEGER, Kind.CLASS);
            case BYTE:
                return Type.create(DotNames.BYTE, Kind.CLASS);
            case CHAR:
                return Type.create(DotNames.CHARACTER, Kind.CLASS);
            case SHORT:
                return Type.create(DotNames.SHORT, Kind.CLASS);
            default:
                throw new IllegalArgumentException("Unsupported primitive: " + primitive);
        }
    }

    private static Set<DotName> getAllInterfacesExtending(DotName target, IndexView index) {
        Set<DotName> ret = new HashSet<>();
        for (ClassInfo clazz : index.getKnownClasses()) {
            if (!Modifier.isInterface(clazz.flags())
                    || clazz.isAnnotation()
                    || clazz.isEnum()) {
                continue;
            }
            if (clazz.interfaceNames().contains(target)) {
                ret.add(clazz.name());
            }
        }
        return ret;
    }

}
