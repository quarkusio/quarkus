package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import io.quarkus.arc.impl.GenericArrayTypeImpl;
import io.quarkus.arc.impl.ParameterizedTypeImpl;
import io.quarkus.arc.impl.TypeVariableImpl;
import io.quarkus.arc.impl.TypeVariableReferenceImpl;
import io.quarkus.arc.impl.WildcardTypeImpl;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import jakarta.enterprise.inject.spi.DefinitionException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.WildcardType;
import org.jboss.logging.Logger;

/**
 *
 * @author Martin Kouba
 */
public final class Types {

    static final Logger LOGGER = Logger.getLogger(Types.class);

    private static final Type OBJECT_TYPE = Type.create(DotNames.OBJECT, Kind.CLASS);

    private static final Set<String> PRIMITIVE_CLASS_NAMES = new HashSet<>();

    static {
        PRIMITIVE_CLASS_NAMES.add("byte");
        PRIMITIVE_CLASS_NAMES.add("char");
        PRIMITIVE_CLASS_NAMES.add("double");
        PRIMITIVE_CLASS_NAMES.add("float");
        PRIMITIVE_CLASS_NAMES.add("int");
        PRIMITIVE_CLASS_NAMES.add("long");
        PRIMITIVE_CLASS_NAMES.add("short");
        PRIMITIVE_CLASS_NAMES.add("boolean");
    }

    // we ban these interfaces because they are new to Java 12 and are used by java.lang.String which
    // means that they cannot be included in bytecode if we want to have application built with Java 12+ but targeting Java 8 - 11
    // actually run on those older versions
    // TODO:  add a extensible banning mechanism based on predicates if we find that this set needs to grow...
    private static final Set<DotName> BANNED_INTERFACE_TYPES = new HashSet<>(
            Arrays.asList(DotName.createSimple("java.lang.constant.ConstantDesc"),
                    DotName.createSimple("java.lang.constant.Constable")));

    private Types() {
    }

    public static ResultHandle getTypeHandle(BytecodeCreator creator, Type type) {
        return getTypeHandle(creator, type, null);
    }

    public static ResultHandle getTypeHandle(BytecodeCreator creator, Type type, ResultHandle tccl) {
        AssignableResultHandle result = creator.createVariable(Object.class);
        getTypeHandle(result, creator, type, tccl, null, new ArrayDeque<>());
        return result;
    }

    private static class TypeVariableInfo {
        final String name;
        ResultHandle handle;

        TypeVariableInfo(String name) {
            this.name = name;
        }

        static TypeVariableInfo find(String name, Deque<TypeVariableInfo> typeVariableStack) {
            for (TypeVariableInfo typeVariableInfo : typeVariableStack) {
                if (typeVariableInfo.name.equals(name)) {
                    return typeVariableInfo;
                }
            }
            return null;
        }
    }

    static void getTypeHandle(AssignableResultHandle variable, BytecodeCreator creator, Type type, ResultHandle tccl,
            TypeCache cache) {
        getTypeHandle(variable, creator, type, tccl, cache, new ArrayDeque<>());
    }

    private static void getTypeHandle(AssignableResultHandle variable, BytecodeCreator creator, Type type,
            ResultHandle tccl, TypeCache cache, Deque<TypeVariableInfo> typeVariableStack) {
        if (cache != null) {
            ResultHandle cachedType = cache.get(type, creator);
            BranchResult cachedNull = creator.ifNull(cachedType);
            cachedNull.falseBranch().assign(variable, cachedType);
            creator = cachedNull.trueBranch();
        }
        if (Kind.CLASS.equals(type.kind())) {
            String className = type.asClassType().name().toString();
            ResultHandle classHandle = doLoadClass(creator, className, tccl);
            if (cache != null) {
                cache.put(type, classHandle, creator);
            }
            creator.assign(variable, classHandle);
        } else if (Kind.TYPE_VARIABLE.equals(type.kind())) {
            // E.g. T -> new TypeVariableImpl("T")
            TypeVariable typeVariable = type.asTypeVariable();
            typeVariableStack.push(new TypeVariableInfo(typeVariable.identifier()));
            ResultHandle boundsHandle;
            List<Type> bounds = typeVariable.bounds();
            if (bounds.isEmpty()) {
                boundsHandle = creator.newArray(java.lang.reflect.Type.class, creator.load(0));
            } else {
                boundsHandle = creator.newArray(java.lang.reflect.Type.class, creator.load(bounds.size()));
                for (int i = 0; i < bounds.size(); i++) {
                    AssignableResultHandle boundHandle = creator.createVariable(Object.class);
                    getTypeHandle(boundHandle, creator, bounds.get(i), tccl, cache, typeVariableStack);
                    creator.writeArrayValue(boundsHandle, i, boundHandle);
                }
            }
            ResultHandle typeVariableHandle = creator.newInstance(
                    MethodDescriptor.ofConstructor(TypeVariableImpl.class, String.class, java.lang.reflect.Type[].class),
                    creator.load(typeVariable.identifier()), boundsHandle);
            if (cache != null) {
                cache.put(typeVariable, typeVariableHandle, creator);
            }
            creator.assign(variable, typeVariableHandle);

            TypeVariableInfo recursive = typeVariableStack.pop();
            if (recursive.handle != null) {
                creator.invokeVirtualMethod(MethodDescriptor.ofMethod(TypeVariableReferenceImpl.class, "setDelegate",
                        void.class, TypeVariableImpl.class), recursive.handle, typeVariableHandle);
            }

        } else if (Kind.PARAMETERIZED_TYPE.equals(type.kind())) {
            // E.g. List<String> -> new ParameterizedTypeImpl(List.class, String.class)
            getParameterizedType(variable, creator, tccl, type.asParameterizedType(), cache, typeVariableStack);

        } else if (Kind.ARRAY.equals(type.kind())) {
            Type componentType = type.asArrayType().component();
            // E.g. String[] -> new GenericArrayTypeImpl(String.class)
            AssignableResultHandle componentTypeHandle = creator.createVariable(Object.class);
            getTypeHandle(componentTypeHandle, creator, componentType, tccl, cache, typeVariableStack);
            ResultHandle arrayHandle = creator.newInstance(
                    MethodDescriptor.ofConstructor(GenericArrayTypeImpl.class, java.lang.reflect.Type.class),
                    componentTypeHandle);
            if (cache != null) {
                cache.put(type, arrayHandle, creator);
            }
            creator.assign(variable, arrayHandle);

        } else if (Kind.WILDCARD_TYPE.equals(type.kind())) {
            // E.g. ? extends Number -> WildcardTypeImpl.withUpperBound(Number.class)
            WildcardType wildcardType = type.asWildcardType();
            ResultHandle wildcardHandle;
            if (wildcardType.superBound() == null) {
                AssignableResultHandle extendsBoundHandle = creator.createVariable(Object.class);
                getTypeHandle(extendsBoundHandle, creator, wildcardType.extendsBound(), tccl, cache, typeVariableStack);
                wildcardHandle = creator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(WildcardTypeImpl.class, "withUpperBound",
                                java.lang.reflect.WildcardType.class, java.lang.reflect.Type.class),
                        extendsBoundHandle);
            } else {
                AssignableResultHandle superBoundHandle = creator.createVariable(Object.class);
                getTypeHandle(superBoundHandle, creator, wildcardType.superBound(), tccl, cache, typeVariableStack);
                wildcardHandle = creator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(WildcardTypeImpl.class, "withLowerBound",
                                java.lang.reflect.WildcardType.class, java.lang.reflect.Type.class),
                        superBoundHandle);
            }
            if (cache != null) {
                cache.put(wildcardType, wildcardHandle, creator);
            }
            creator.assign(variable, wildcardHandle);
        } else if (Kind.PRIMITIVE.equals(type.kind())) {
            switch (type.asPrimitiveType().primitive()) {
                case INT:
                    creator.assign(variable, creator.loadClass(int.class));
                    break;
                case LONG:
                    creator.assign(variable, creator.loadClass(long.class));
                    break;
                case BOOLEAN:
                    creator.assign(variable, creator.loadClass(boolean.class));
                    break;
                case BYTE:
                    creator.assign(variable, creator.loadClass(byte.class));
                    break;
                case CHAR:
                    creator.assign(variable, creator.loadClass(char.class));
                    break;
                case DOUBLE:
                    creator.assign(variable, creator.loadClass(double.class));
                    break;
                case FLOAT:
                    creator.assign(variable, creator.loadClass(float.class));
                    break;
                case SHORT:
                    creator.assign(variable, creator.loadClass(short.class));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported primitive type: " + type);
            }
        } else if (Kind.UNRESOLVED_TYPE_VARIABLE.equals(type.kind())) {
            String identifier = type.asUnresolvedTypeVariable().identifier();
            TypeVariableInfo recursive = TypeVariableInfo.find(identifier, typeVariableStack);
            if (recursive != null) {
                ResultHandle typeVariableHandle = creator.newInstance(
                        MethodDescriptor.ofConstructor(TypeVariableReferenceImpl.class, String.class),
                        creator.load(identifier));
                creator.assign(variable, typeVariableHandle);
                recursive.handle = typeVariableHandle;
                return;
            }

            throw new IllegalArgumentException("Can't resolve type variable: " + type);
        } else {
            throw new IllegalArgumentException("Unsupported bean type: " + type.kind() + ", " + type);
        }
    }

    private static void getParameterizedType(AssignableResultHandle variable, BytecodeCreator creator, ResultHandle tccl,
            ParameterizedType parameterizedType, TypeCache cache, Deque<TypeVariableInfo> typeVariableStack) {
        List<Type> arguments = parameterizedType.arguments();
        ResultHandle typeArgsHandle = creator.newArray(java.lang.reflect.Type.class, creator.load(arguments.size()));
        for (int i = 0; i < arguments.size(); i++) {
            AssignableResultHandle argumentHandle = creator.createVariable(Object.class);
            getTypeHandle(argumentHandle, creator, arguments.get(i), tccl, cache, typeVariableStack);
            creator.writeArrayValue(typeArgsHandle, i, argumentHandle);
        }
        Type rawType = Type.create(parameterizedType.name(), Kind.CLASS);
        ResultHandle rawTypeHandle = null;
        if (cache != null) {
            rawTypeHandle = cache.get(rawType, creator);
        }
        if (rawTypeHandle == null) {
            rawTypeHandle = doLoadClass(creator, parameterizedType.name().toString(), tccl);
            if (cache != null) {
                cache.put(rawType, rawTypeHandle, creator);
            }
        }
        ResultHandle parameterizedTypeHandle = creator.newInstance(
                MethodDescriptor.ofConstructor(ParameterizedTypeImpl.class, java.lang.reflect.Type.class,
                        java.lang.reflect.Type[].class),
                rawTypeHandle, typeArgsHandle);
        if (cache != null) {
            cache.put(parameterizedType, parameterizedTypeHandle, creator);
        }
        creator.assign(variable, parameterizedTypeHandle);
    }

    public static void getParameterizedType(AssignableResultHandle variable, BytecodeCreator creator, ResultHandle tccl,
            ParameterizedType parameterizedType) {
        getParameterizedType(variable, creator, tccl, parameterizedType, null, new ArrayDeque<>());
    }

    public static ResultHandle getParameterizedType(BytecodeCreator creator, ResultHandle tccl,
            ParameterizedType parameterizedType) {
        AssignableResultHandle result = creator.createVariable(Object.class);
        getParameterizedType(result, creator, tccl, parameterizedType, null, new ArrayDeque<>());
        return result;
    }

    private static ResultHandle doLoadClass(BytecodeCreator creator, String className, ResultHandle tccl) {
        if (className.startsWith("java.")) {
            return creator.loadClass(className);
        } else {
            //we need to use Class.forName as the class may be package private
            if (tccl == null) {
                ResultHandle currentThread = creator
                        .invokeStaticMethod(MethodDescriptors.THREAD_CURRENT_THREAD);
                tccl = creator.invokeVirtualMethod(MethodDescriptors.THREAD_GET_TCCL, currentThread);
            }
            return creator.invokeStaticMethod(MethodDescriptors.CL_FOR_NAME, creator.load(className), creator.load(false),
                    tccl);
        }
    }

    static Type getProviderType(ClassInfo classInfo) {
        List<TypeVariable> typeParameters = classInfo.typeParameters();
        if (!typeParameters.isEmpty()) {
            return ParameterizedType.create(classInfo.name(), typeParameters.toArray(new Type[] {}), null);
        } else {
            return Type.create(classInfo.name(), Kind.CLASS);
        }
    }

    static Set<Type> getProducerMethodTypeClosure(MethodInfo producerMethod, BeanDeployment beanDeployment) {
        Set<Type> types;
        Type returnType = producerMethod.returnType();
        if (returnType.kind() == Kind.TYPE_VARIABLE) {
            throw new DefinitionException("A type variable is not a legal bean type: " + producerMethod);
        }
        if (returnType.kind() == Kind.PRIMITIVE || returnType.kind() == Kind.ARRAY) {
            types = new HashSet<>();
            types.add(returnType);
            types.add(OBJECT_TYPE);
            return types;
        } else {
            ClassInfo returnTypeClassInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), returnType);
            if (returnTypeClassInfo == null) {
                throw new IllegalArgumentException(
                        "Producer method return type not found in index: " + producerMethod.returnType().name());
            }
            if (Kind.CLASS.equals(returnType.kind())) {
                types = getTypeClosure(returnTypeClassInfo, producerMethod, Collections.emptyMap(), beanDeployment, null);
            } else if (Kind.PARAMETERIZED_TYPE.equals(returnType.kind())) {
                types = getTypeClosure(returnTypeClassInfo, producerMethod,
                        buildResolvedMap(returnType.asParameterizedType().arguments(), returnTypeClassInfo.typeParameters(),
                                Collections.emptyMap(), beanDeployment.getBeanArchiveIndex()),
                        beanDeployment, null);
            } else {
                throw new IllegalArgumentException("Unsupported return type");
            }
        }
        return restrictBeanTypes(types, beanDeployment.getAnnotations(producerMethod));
    }

    static Set<Type> getProducerFieldTypeClosure(FieldInfo producerField, BeanDeployment beanDeployment) {
        Set<Type> types;
        Type fieldType = producerField.type();
        if (fieldType.kind() == Kind.TYPE_VARIABLE) {
            throw new DefinitionException("A type variable is not a legal bean type: " + producerField);
        }
        if (fieldType.kind() == Kind.PRIMITIVE || fieldType.kind() == Kind.ARRAY) {
            types = new HashSet<>();
            types.add(fieldType);
            types.add(OBJECT_TYPE);
        } else {
            ClassInfo fieldClassInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), producerField.type());
            if (fieldClassInfo == null) {
                throw new IllegalArgumentException("Producer field type not found in index: " + producerField.type().name());
            }
            if (Kind.CLASS.equals(fieldType.kind())) {
                types = getTypeClosure(fieldClassInfo, producerField, Collections.emptyMap(), beanDeployment, null);
            } else if (Kind.PARAMETERIZED_TYPE.equals(fieldType.kind())) {
                types = getTypeClosure(fieldClassInfo, producerField,
                        buildResolvedMap(fieldType.asParameterizedType().arguments(), fieldClassInfo.typeParameters(),
                                Collections.emptyMap(), beanDeployment.getBeanArchiveIndex()),
                        beanDeployment, null);
            } else {
                throw new IllegalArgumentException("Unsupported return type");
            }
        }
        return restrictBeanTypes(types, beanDeployment.getAnnotations(producerField));
    }

    static Set<Type> getClassBeanTypeClosure(ClassInfo classInfo, BeanDeployment beanDeployment) {
        Set<Type> types;
        List<TypeVariable> typeParameters = classInfo.typeParameters();
        if (typeParameters.isEmpty()) {
            types = getTypeClosure(classInfo, null, Collections.emptyMap(), beanDeployment, null);
        } else {
            types = getTypeClosure(classInfo, null, buildResolvedMap(typeParameters, typeParameters,
                    Collections.emptyMap(), beanDeployment.getBeanArchiveIndex()), beanDeployment, null);
        }
        return restrictBeanTypes(types, beanDeployment.getAnnotations(classInfo));
    }

    static List<Type> getResolvedParameters(ClassInfo classInfo, MethodInfo method, IndexView index) {
        return getResolvedParameters(classInfo, Collections.emptyMap(), method, index);
    }

    static List<Type> getResolvedParameters(ClassInfo classInfo, Map<TypeVariable, Type> resolvedMap,
            MethodInfo method, IndexView index) {
        List<TypeVariable> typeParameters = classInfo.typeParameters();
        // E.g. Foo, T, List<String>
        List<Type> parameters = method.parameterTypes();
        if (typeParameters.isEmpty()) {
            return parameters;
        } else {
            resolvedMap = buildResolvedMap(typeParameters, typeParameters,
                    resolvedMap, index);
            List<Type> resolved = new ArrayList<>();
            for (Type param : parameters) {
                switch (param.kind()) {
                    case ARRAY:
                    case PRIMITIVE:
                    case CLASS:
                        resolved.add(param);
                        break;
                    case TYPE_VARIABLE:
                    case PARAMETERIZED_TYPE:
                        resolved.add(resolveTypeParam(param, resolvedMap, index));
                    default:
                        break;
                }
            }
            return resolved;
        }
    }

    static Set<Type> getTypeClosure(ClassInfo classInfo, AnnotationTarget producerFieldOrMethod,
            Map<TypeVariable, Type> resolvedTypeParameters,
            BeanDeployment beanDeployment, BiConsumer<ClassInfo, Map<TypeVariable, Type>> resolvedTypeVariablesConsumer) {
        Set<Type> types = new HashSet<>();
        List<TypeVariable> typeParameters = classInfo.typeParameters();

        if (typeParameters.isEmpty() || !typeParameters.stream().allMatch(resolvedTypeParameters::containsKey)) {
            // Not a parameterized type or a raw type
            types.add(Type.create(classInfo.name(), Kind.CLASS));
        } else {
            // Canonical ParameterizedType with unresolved type variables
            Type[] typeParams = new Type[typeParameters.size()];
            boolean skipThisType = false;
            for (int i = 0; i < typeParameters.size(); i++) {
                typeParams[i] = resolvedTypeParameters.get(typeParameters.get(i));
                // this should only be the case for producers; wildcard is not a legal bean type
                // see https://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#legal_bean_types
                if (typeParams[i].kind().equals(Kind.WILDCARD_TYPE) && producerFieldOrMethod != null) {
                    LOGGER.info("Producer " +
                            (producerFieldOrMethod.kind().equals(AnnotationTarget.Kind.FIELD) ? "field " : "method ") +
                            producerFieldOrMethod +
                            " contains a parameterized typed with a wildcard. This type is not a legal bean type" +
                            " according to CDI specification and will be ignored during bean resolution.");
                    skipThisType = true;
                }
            }
            if (resolvedTypeVariablesConsumer != null) {
                Map<TypeVariable, Type> resolved = new HashMap<>();
                for (int i = 0; i < typeParameters.size(); i++) {
                    resolved.put(typeParameters.get(i), typeParams[i]);
                }
                resolvedTypeVariablesConsumer.accept(classInfo, resolved);
            }
            if (!skipThisType) {
                types.add(ParameterizedType.create(classInfo.name(), typeParams, null));
            }
        }
        // Interfaces
        for (Type interfaceType : classInfo.interfaceTypes()) {
            if (BANNED_INTERFACE_TYPES.contains(interfaceType.name())) {
                continue;
            }
            ClassInfo interfaceClassInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), interfaceType.name());
            if (interfaceClassInfo != null) {
                Map<TypeVariable, Type> resolved = Collections.emptyMap();
                if (Kind.PARAMETERIZED_TYPE.equals(interfaceType.kind())) {
                    resolved = buildResolvedMap(interfaceType.asParameterizedType().arguments(),
                            interfaceClassInfo.typeParameters(), resolvedTypeParameters, beanDeployment.getBeanArchiveIndex());
                }
                types.addAll(getTypeClosure(interfaceClassInfo, producerFieldOrMethod, resolved, beanDeployment,
                        resolvedTypeVariablesConsumer));
            }
        }
        // Superclass
        if (classInfo.superClassType() != null) {
            ClassInfo superClassInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), classInfo.superName());
            if (superClassInfo != null) {
                Map<TypeVariable, Type> resolved = Collections.emptyMap();
                if (Kind.PARAMETERIZED_TYPE.equals(classInfo.superClassType().kind())) {
                    resolved = buildResolvedMap(classInfo.superClassType().asParameterizedType().arguments(),
                            superClassInfo.typeParameters(),
                            resolvedTypeParameters, beanDeployment.getBeanArchiveIndex());
                }
                types.addAll(getTypeClosure(superClassInfo, producerFieldOrMethod, resolved, beanDeployment,
                        resolvedTypeVariablesConsumer));
            }
        }
        return types;
    }

    static Set<Type> getDelegateTypeClosure(InjectionPointInfo delegateInjectionPoint, BeanDeployment beanDeployment) {
        Set<Type> types;
        Type delegateType = delegateInjectionPoint.getRequiredType();
        if (delegateType.kind() == Kind.TYPE_VARIABLE
                || delegateType.kind() == Kind.PRIMITIVE
                || delegateType.kind() == Kind.ARRAY) {
            throw new DefinitionException("Illegal delegate type declared:" + delegateInjectionPoint.getTargetInfo());
        }
        ClassInfo delegateTypeClass = getClassByName(beanDeployment.getBeanArchiveIndex(), delegateType);
        if (delegateTypeClass == null) {
            throw new IllegalArgumentException("Delegate type not found in index: " + delegateType);
        }
        if (Kind.CLASS.equals(delegateType.kind())) {
            types = getTypeClosure(delegateTypeClass, delegateInjectionPoint.getTarget(), Collections.emptyMap(),
                    beanDeployment, null);
        } else if (Kind.PARAMETERIZED_TYPE.equals(delegateType.kind())) {
            types = getTypeClosure(delegateTypeClass, delegateInjectionPoint.getTarget(),
                    buildResolvedMap(delegateType.asParameterizedType().arguments(), delegateTypeClass.typeParameters(),
                            Collections.emptyMap(), beanDeployment.getBeanArchiveIndex()),
                    beanDeployment, null);
        } else {
            throw new IllegalArgumentException("Unsupported return type");
        }
        return types;
    }

    static Map<ClassInfo, Map<TypeVariable, Type>> resolvedTypeVariables(ClassInfo classInfo,
            BeanDeployment beanDeployment) {
        Map<ClassInfo, Map<TypeVariable, Type>> resolvedTypeVariables = new HashMap<>();
        getTypeClosure(classInfo, null, Collections.emptyMap(), beanDeployment, resolvedTypeVariables::put);
        return resolvedTypeVariables;
    }

    static Set<Type> restrictBeanTypes(Set<Type> types, Collection<AnnotationInstance> annotations) {
        AnnotationInstance typed = annotations.stream().filter(a -> a.name().equals(DotNames.TYPED))
                .findFirst().orElse(null);
        if (typed != null) {
            AnnotationValue typedValue = typed.value();
            if (typedValue == null) {
                types.clear();
                types.add(OBJECT_TYPE);
            } else {
                Set<DotName> typedClasses = new HashSet<>();
                for (Type type : typedValue.asClassArray()) {
                    typedClasses.add(type.name());
                }
                for (Iterator<Type> iterator = types.iterator(); iterator.hasNext();) {
                    Type nextType = iterator.next();
                    if (!typedClasses.contains(nextType.name()) && !DotNames.OBJECT.equals(nextType.name())) {
                        iterator.remove();
                    }
                }
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
        if (typeParam.kind() == Kind.TYPE_VARIABLE) {
            return resolvedTypeParameters.getOrDefault(typeParam, typeParam);
        } else if (typeParam.kind() == Kind.PARAMETERIZED_TYPE) {
            ParameterizedType parameterizedType = typeParam.asParameterizedType();
            ClassInfo classInfo = getClassByName(index, parameterizedType.name());
            if (classInfo != null) {
                List<TypeVariable> typeParameters = classInfo.typeParameters();
                List<Type> arguments = parameterizedType.arguments();
                Map<TypeVariable, Type> resolvedMap = buildResolvedMap(arguments, typeParameters,
                        resolvedTypeParameters, index);
                Type[] typeParams = new Type[typeParameters.size()];
                for (int i = 0; i < typeParameters.size(); i++) {
                    typeParams[i] = resolveTypeParam(arguments.get(i), resolvedMap, index);
                }
                return ParameterizedType.create(parameterizedType.name(), typeParams, null);
            }
        }
        return typeParam;
    }

    static String getPackageName(String className) {
        className = className.replace('/', '.');
        return className.contains(".") ? className.substring(0, className.lastIndexOf(".")) : "";
    }

    static String getSimpleName(String className) {
        return className.contains(".") ? className.substring(className.lastIndexOf(".") + 1, className.length()) : className;
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

    static boolean isPrimitiveClassName(String className) {
        return PRIMITIVE_CLASS_NAMES.contains(className);
    }

    static boolean containsTypeVariable(Type type) {
        if (type.kind() == Kind.TYPE_VARIABLE) {
            return true;
        } else if (type.kind() == Kind.PARAMETERIZED_TYPE) {
            for (Type arg : type.asParameterizedType().arguments()) {
                if (containsTypeVariable(arg)) {
                    return true;
                }
            }
        }
        return false;
    }

    interface TypeCache {

        void initialize(MethodCreator method);

        /**
         *
         * @param type
         * @param bytecode
         * @return the cached value or {@code null}
         */
        ResultHandle get(Type type, BytecodeCreator bytecode);

        /**
         *
         * @param type
         * @param value
         * @param bytecode
         */
        void put(Type type, ResultHandle value, BytecodeCreator bytecode);

    }

}
