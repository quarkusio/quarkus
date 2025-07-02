package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.WildcardType;
import org.jboss.logging.Logger;

import io.quarkus.arc.impl.GenericArrayTypeImpl;
import io.quarkus.arc.impl.ParameterizedTypeImpl;
import io.quarkus.arc.impl.TypeVariableImpl;
import io.quarkus.arc.impl.TypeVariableReferenceImpl;
import io.quarkus.arc.impl.WildcardTypeImpl;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 *
 * @author Martin Kouba
 */
public final class Types {

    static final Logger LOGGER = Logger.getLogger(Types.class);

    private static final Set<String> PRIMITIVE_CLASS_NAMES = Set.of(
            "boolean",
            "byte",
            "short",
            "int",
            "long",
            "float",
            "double",
            "char");

    // we ban these interfaces because of mismatch between building JDK version and target JDK version
    // TODO:  add a extensible banning mechanism based on predicates if we find that this set needs to grow...
    private static final Set<DotName> BANNED_INTERFACE_TYPES = Set.of(DotName.createSimple("java.util.SequencedCollection"));

    private Types() {
    }

    @Deprecated(forRemoval = true, since = "3.30")
    private static class TypeVariables {
        private final Map<String, ResultHandle> typeVariable = new HashMap<>();
        private final Map<String, ResultHandle> typeVariableReference = new HashMap<>();

        ResultHandle getTypeVariable(String identifier) {
            return typeVariable.get(identifier);
        }

        void setTypeVariable(String identifier, ResultHandle handle) {
            typeVariable.put(identifier, handle);
        }

        ResultHandle getTypeVariableReference(String identifier) {
            return typeVariableReference.get(identifier);
        }

        void setTypeVariableReference(String identifier, ResultHandle handle) {
            typeVariableReference.put(identifier, handle);
        }

        void patchReferences(BytecodeCreator creator) {
            typeVariableReference.forEach((identifier, reference) -> {
                ResultHandle typeVar = typeVariable.get(identifier);
                if (typeVar != null) {
                    creator.invokeVirtualMethod(MethodDescriptor.ofMethod(TypeVariableReferenceImpl.class,
                            "setDelegate", void.class, TypeVariableImpl.class), reference, typeVar);
                }
            });
        }
    }

    @Deprecated(forRemoval = true, since = "3.30")
    public static ResultHandle getTypeHandle(BytecodeCreator creator, Type type) {
        return getTypeHandle(creator, type, null);
    }

    @Deprecated(forRemoval = true, since = "3.30")
    public static ResultHandle getTypeHandle(BytecodeCreator creator, Type type, ResultHandle tccl) {
        return getTypeHandle(creator, type, tccl, null);
    }

    @Deprecated(forRemoval = true, since = "3.30")
    public static ResultHandle getTypeHandle(BytecodeCreator creator, Type type, ResultHandle tccl, IndexView index) {
        AssignableResultHandle result = creator.createVariable(Object.class);
        TypeVariables typeVariables = new TypeVariables();
        getTypeHandle(result, creator, type, tccl, typeVariables, index);
        typeVariables.patchReferences(creator);
        return result;
    }

    @Deprecated(forRemoval = true, since = "3.30")
    private static void getTypeHandle(AssignableResultHandle variable, BytecodeCreator creator, Type type,
            ResultHandle tccl, TypeVariables typeVariables, IndexView index) {
        if (Kind.CLASS.equals(type.kind())) {
            String className = type.asClassType().name().toString();
            ResultHandle classHandle = doLoadClass(creator, className, tccl);
            creator.assign(variable, classHandle);
        } else if (Kind.TYPE_VARIABLE.equals(type.kind())) {
            // E.g. T -> new TypeVariableImpl("T")
            TypeVariable typeVariable = type.asTypeVariable();
            String identifier = typeVariable.identifier();

            ResultHandle typeVariableHandle = typeVariables.getTypeVariable(identifier);
            if (typeVariableHandle == null) {
                ResultHandle boundsHandle;
                List<Type> bounds = typeVariable.bounds();
                if (bounds.isEmpty()) {
                    boundsHandle = creator.newArray(java.lang.reflect.Type.class, creator.load(0));
                } else {
                    boundsHandle = creator.newArray(java.lang.reflect.Type.class, creator.load(bounds.size()));
                    for (int i = 0; i < bounds.size(); i++) {
                        AssignableResultHandle boundHandle = creator.createVariable(Object.class);
                        getTypeHandle(boundHandle, creator, bounds.get(i), tccl, typeVariables, index);
                        creator.writeArrayValue(boundsHandle, i, boundHandle);
                    }
                }
                typeVariableHandle = creator.newInstance(
                        MethodDescriptor.ofConstructor(TypeVariableImpl.class, String.class, java.lang.reflect.Type[].class),
                        creator.load(identifier), boundsHandle);
                typeVariables.setTypeVariable(identifier, typeVariableHandle);
            }
            creator.assign(variable, typeVariableHandle);

        } else if (Kind.PARAMETERIZED_TYPE.equals(type.kind())) {
            // E.g. List<String> -> new ParameterizedTypeImpl(List.class, String.class)
            getParameterizedType(variable, creator, tccl, type.asParameterizedType(), typeVariables, index);

        } else if (Kind.ARRAY.equals(type.kind())) {
            ArrayType array = type.asArrayType();
            Type elementType = array.elementType();

            ResultHandle arrayHandle;
            if (elementType.kind() == Kind.PRIMITIVE || elementType.kind() == Kind.CLASS) {
                // can produce a java.lang.Class representation of the array type
                // E.g. String[] -> String[].class
                arrayHandle = doLoadClass(creator, array.name().toString(), tccl);
            } else {
                // E.g. List<String>[] -> new GenericArrayTypeImpl(new ParameterizedTypeImpl(List.class, String.class))
                Type componentType = type.asArrayType().constituent();
                AssignableResultHandle componentTypeHandle = creator.createVariable(Object.class);
                getTypeHandle(componentTypeHandle, creator, componentType, tccl, typeVariables, index);
                arrayHandle = creator.newInstance(
                        MethodDescriptor.ofConstructor(GenericArrayTypeImpl.class, java.lang.reflect.Type.class),
                        componentTypeHandle);
            }
            creator.assign(variable, arrayHandle);

        } else if (Kind.WILDCARD_TYPE.equals(type.kind())) {
            // E.g. ? extends Number -> WildcardTypeImpl.withUpperBound(Number.class)
            WildcardType wildcardType = type.asWildcardType();
            ResultHandle wildcardHandle;
            if (wildcardType.superBound() == null) {
                AssignableResultHandle extendsBoundHandle = creator.createVariable(Object.class);
                getTypeHandle(extendsBoundHandle, creator, wildcardType.extendsBound(), tccl, typeVariables, index);
                wildcardHandle = creator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(WildcardTypeImpl.class, "withUpperBound",
                                java.lang.reflect.WildcardType.class, java.lang.reflect.Type.class),
                        extendsBoundHandle);
            } else {
                AssignableResultHandle superBoundHandle = creator.createVariable(Object.class);
                getTypeHandle(superBoundHandle, creator, wildcardType.superBound(), tccl, typeVariables, index);
                wildcardHandle = creator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(WildcardTypeImpl.class, "withLowerBound",
                                java.lang.reflect.WildcardType.class, java.lang.reflect.Type.class),
                        superBoundHandle);
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
        } else if (Kind.VOID.equals(type.kind())) {
            creator.assign(variable, creator.loadClass(void.class));
        } else if (Kind.TYPE_VARIABLE_REFERENCE.equals(type.kind())) {
            String identifier = type.asTypeVariableReference().identifier();

            ResultHandle typeVariableReferenceHandle = typeVariables.getTypeVariableReference(identifier);
            if (typeVariableReferenceHandle == null) {
                typeVariableReferenceHandle = creator.newInstance(
                        MethodDescriptor.ofConstructor(TypeVariableReferenceImpl.class, String.class),
                        creator.load(identifier));
                typeVariables.setTypeVariableReference(identifier, typeVariableReferenceHandle);
            }

            creator.assign(variable, typeVariableReferenceHandle);
        } else {
            throw new IllegalArgumentException("Unsupported bean type: " + type.kind() + ", " + type);
        }
    }

    @Deprecated(forRemoval = true, since = "3.30")
    private static void getParameterizedType(AssignableResultHandle variable, BytecodeCreator creator, ResultHandle tccl,
            ParameterizedType parameterizedType, TypeVariables typeVariables, IndexView index) {
        List<Type> arguments = parameterizedType.arguments();
        ResultHandle typeArgsHandle = creator.newArray(java.lang.reflect.Type.class, creator.load(arguments.size()));
        for (int i = 0; i < arguments.size(); i++) {
            AssignableResultHandle argumentHandle = creator.createVariable(Object.class);
            getTypeHandle(argumentHandle, creator, arguments.get(i), tccl, typeVariables, index);
            creator.writeArrayValue(typeArgsHandle, i, argumentHandle);
        }
        ResultHandle rawTypeHandle = doLoadClass(creator, parameterizedType.name().toString(), tccl);
        AssignableResultHandle ownerTypeHandle = creator.createVariable(Object.class);
        if (parameterizedType.owner() != null) {
            getTypeHandle(ownerTypeHandle, creator, parameterizedType.owner(), tccl, typeVariables, index);
        } else if (index != null) {
            ClassInfo clazz = index.getClassByName(parameterizedType.name());
            if (clazz != null && clazz.enclosingClass() != null) {
                // this is not entirely precise, but generic classes with more than 1 level of nesting are very rare
                ClassType owner = ClassType.create(clazz.enclosingClass());
                getTypeHandle(ownerTypeHandle, creator, owner, tccl, typeVariables, index);
            } else {
                creator.assign(ownerTypeHandle, creator.loadNull());
            }
        } else {
            creator.assign(ownerTypeHandle, creator.loadNull());
        }
        ResultHandle parameterizedTypeHandle = creator.newInstance(
                MethodDescriptor.ofConstructor(ParameterizedTypeImpl.class, java.lang.reflect.Type.class,
                        java.lang.reflect.Type[].class, java.lang.reflect.Type.class),
                rawTypeHandle, typeArgsHandle, ownerTypeHandle);
        creator.assign(variable, parameterizedTypeHandle);
    }

    @Deprecated(forRemoval = true, since = "3.30")
    public static void getParameterizedType(AssignableResultHandle variable, BytecodeCreator creator, ResultHandle tccl,
            ParameterizedType parameterizedType) {
        TypeVariables typeVariables = new TypeVariables();
        getParameterizedType(variable, creator, tccl, parameterizedType, typeVariables, null);
        typeVariables.patchReferences(creator);
    }

    @Deprecated(forRemoval = true, since = "3.30")
    public static ResultHandle getParameterizedType(BytecodeCreator creator, ResultHandle tccl,
            ParameterizedType parameterizedType) {
        AssignableResultHandle result = creator.createVariable(Object.class);
        TypeVariables typeVariables = new TypeVariables();
        getParameterizedType(result, creator, tccl, parameterizedType, typeVariables, null);
        typeVariables.patchReferences(creator);
        return result;
    }

    @Deprecated(forRemoval = true, since = "3.30")
    private static final MethodDescriptor THREAD_CURRENT_THREAD = MethodDescriptor.ofMethod(Thread.class, "currentThread",
            Thread.class);

    @Deprecated(forRemoval = true, since = "3.30")
    private static final MethodDescriptor THREAD_GET_TCCL = MethodDescriptor.ofMethod(Thread.class, "getContextClassLoader",
            ClassLoader.class);

    @Deprecated(forRemoval = true, since = "3.30")
    private static final MethodDescriptor CL_FOR_NAME = MethodDescriptor.ofMethod(Class.class, "forName",
            Class.class, String.class, boolean.class, ClassLoader.class);

    @Deprecated(forRemoval = true, since = "3.30")
    private static ResultHandle doLoadClass(BytecodeCreator creator, String className, ResultHandle tccl) {
        if (className.startsWith("java.")) {
            return creator.loadClass(className);
        } else {
            //we need to use Class.forName as the class may be package private
            if (tccl == null) {
                ResultHandle currentThread = creator.invokeStaticMethod(THREAD_CURRENT_THREAD);
                tccl = creator.invokeVirtualMethod(THREAD_GET_TCCL, currentThread);
            }
            return creator.invokeStaticMethod(CL_FOR_NAME, creator.load(className), creator.load(false), tccl);
        }
    }

    /**
     * @deprecated use {@link Type#create(Class)}
     */
    @Deprecated(forRemoval = true, since = "3.30")
    public static Type jandexType(Class<?> clazz) {
        return Type.create(clazz);
    }

    public static Type jandexType(java.lang.reflect.Type type) {
        if (type instanceof java.lang.Class<?> c) {
            return jandexType(c);
        } else if (type instanceof java.lang.reflect.ParameterizedType p) {
            org.jboss.jandex.ParameterizedType.Builder builder = org.jboss.jandex.ParameterizedType
                    .builder((Class<?>) p.getRawType());
            for (java.lang.reflect.Type typeArgument : p.getActualTypeArguments()) {
                builder.addArgument(jandexType(typeArgument));
            }
            return builder.build();
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    static Type getProviderType(ClassInfo classInfo) {
        List<TypeVariable> typeParameters = classInfo.typeParameters();
        if (!typeParameters.isEmpty()) {
            return ParameterizedType.create(classInfo.name(), typeParameters.toArray(new Type[0]), null);
        } else {
            return ClassType.create(classInfo.name());
        }
    }

    static TypeClosure getProducerMethodTypeClosure(MethodInfo producerMethod, BeanDeployment beanDeployment) {
        Set<Type> types;
        Set<Type> unrestrictedBeanTypes = new HashSet<>();
        Type returnType = producerMethod.returnType();
        if (returnType.kind() == Kind.TYPE_VARIABLE) {
            throw new DefinitionException("A type variable is not a legal bean type: " + producerMethod);
        }
        if (returnType.kind() == Kind.ARRAY) {
            checkArrayType(returnType.asArrayType(), producerMethod);
        }
        if (returnType.kind() == Kind.PRIMITIVE || returnType.kind() == Kind.ARRAY) {
            types = new HashSet<>();
            types.add(returnType);
            types.add(ClassType.OBJECT_TYPE);
            return new TypeClosure(types);
        } else {
            ClassInfo returnTypeClassInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), returnType);
            if (returnTypeClassInfo == null) {
                throw new IllegalArgumentException(
                        "Producer method return type not found in index: " + producerMethod.returnType().name());
            }
            if (Kind.CLASS.equals(returnType.kind())) {
                types = getTypeClosure(returnTypeClassInfo, producerMethod, Collections.emptyMap(), beanDeployment, null,
                        unrestrictedBeanTypes);
            } else if (Kind.PARAMETERIZED_TYPE.equals(returnType.kind())) {
                types = getTypeClosure(returnTypeClassInfo, producerMethod,
                        buildResolvedMap(returnType.asParameterizedType().arguments(), returnTypeClassInfo.typeParameters(),
                                Collections.emptyMap(), beanDeployment.getBeanArchiveIndex()),
                        beanDeployment, null, unrestrictedBeanTypes);
            } else {
                throw new IllegalArgumentException("Unsupported return type");
            }
        }
        return new TypeClosure(restrictBeanTypes(types, unrestrictedBeanTypes, beanDeployment.getAnnotations(producerMethod),
                beanDeployment.getBeanArchiveIndex(), producerMethod), unrestrictedBeanTypes);
    }

    static TypeClosure getProducerFieldTypeClosure(FieldInfo producerField, BeanDeployment beanDeployment) {
        Set<Type> types;
        Set<Type> unrestrictedBeanTypes = new HashSet<>();
        Type fieldType = producerField.type();
        if (fieldType.kind() == Kind.TYPE_VARIABLE) {
            throw new DefinitionException("A type variable is not a legal bean type: " + producerField);
        }
        if (fieldType.kind() == Kind.ARRAY) {
            checkArrayType(fieldType.asArrayType(), producerField);
        }
        if (fieldType.kind() == Kind.PRIMITIVE || fieldType.kind() == Kind.ARRAY) {
            types = new HashSet<>();
            types.add(fieldType);
            types.add(ClassType.OBJECT_TYPE);
            return new TypeClosure(types);
        } else {
            ClassInfo fieldClassInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), producerField.type());
            if (fieldClassInfo == null) {
                throw new IllegalArgumentException("Producer field type not found in index: " + producerField.type().name());
            }
            if (Kind.CLASS.equals(fieldType.kind())) {
                types = getTypeClosure(fieldClassInfo, producerField, Collections.emptyMap(), beanDeployment, null,
                        unrestrictedBeanTypes);
            } else if (Kind.PARAMETERIZED_TYPE.equals(fieldType.kind())) {
                types = getTypeClosure(fieldClassInfo, producerField,
                        buildResolvedMap(fieldType.asParameterizedType().arguments(), fieldClassInfo.typeParameters(),
                                Collections.emptyMap(), beanDeployment.getBeanArchiveIndex()),
                        beanDeployment, null, unrestrictedBeanTypes);
            } else {
                throw new IllegalArgumentException("Unsupported return type");
            }
        }
        return new TypeClosure(restrictBeanTypes(types, unrestrictedBeanTypes, beanDeployment.getAnnotations(producerField),
                beanDeployment.getBeanArchiveIndex(), producerField), unrestrictedBeanTypes);
    }

    static TypeClosure getClassBeanTypeClosure(ClassInfo classInfo, BeanDeployment beanDeployment) {
        Set<Type> types;
        Set<Type> unrestrictedBeanTypes = new HashSet<>();
        List<TypeVariable> typeParameters = classInfo.typeParameters();
        if (typeParameters.isEmpty()) {
            types = getTypeClosure(classInfo, null, Collections.emptyMap(), beanDeployment, null, unrestrictedBeanTypes);
        } else {
            types = getTypeClosure(classInfo, null, buildResolvedMap(typeParameters, typeParameters,
                    Collections.emptyMap(), beanDeployment.getBeanArchiveIndex()), beanDeployment, null, unrestrictedBeanTypes);
        }
        return new TypeClosure(restrictBeanTypes(types, unrestrictedBeanTypes, beanDeployment.getAnnotations(classInfo),
                beanDeployment.getBeanArchiveIndex(), classInfo), unrestrictedBeanTypes);
    }

    record TypeClosure(Set<Type> types, Set<Type> unrestrictedTypes) {

        TypeClosure(Set<Type> types) {
            this(types, types);
        }
    }

    static TypeClosure getTypeClosureFromJandexType(Type jandexType, BeanDeployment beanDeployment) {
        Set<Type> types;
        Set<Type> unrestrictedBeanTypes = new HashSet<>();
        if (jandexType.kind() == Kind.TYPE_VARIABLE) {
            throw new IllegalStateException("A type variable is not a legal bean type");
        }
        if (jandexType.kind() == Kind.PRIMITIVE || jandexType.kind() == Kind.ARRAY) {
            types = new HashSet<>();
            types.add(jandexType);
            types.add(ClassType.OBJECT_TYPE);
            return new TypeClosure(types);
        } else {
            ClassInfo jandexTypeClassInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), jandexType);
            if (jandexTypeClassInfo == null) {
                throw new IllegalArgumentException(
                        "Provided Jandex type not found in index: " + jandexType.name());
            }
            if (Kind.CLASS.equals(jandexType.kind())) {
                types = getTypeClosure(jandexTypeClassInfo, null, Collections.emptyMap(), beanDeployment, null,
                        unrestrictedBeanTypes);
            } else if (Kind.PARAMETERIZED_TYPE.equals(jandexType.kind())) {
                types = getTypeClosure(jandexTypeClassInfo, null,
                        buildResolvedMap(jandexType.asParameterizedType().arguments(), jandexTypeClassInfo.typeParameters(),
                                Collections.emptyMap(), beanDeployment.getBeanArchiveIndex()),
                        beanDeployment, null, unrestrictedBeanTypes);
            } else {
                throw new IllegalArgumentException("Unsupported return type");
            }
        }
        return new TypeClosure(types, unrestrictedBeanTypes);
    }

    static Set<Type> getClassUnrestrictedTypeClosure(ClassInfo classInfo, BeanDeployment beanDeployment) {
        Set<Type> types;
        Set<Type> unrestrictedBeanTypes = new HashSet<>();
        List<TypeVariable> typeParameters = classInfo.typeParameters();
        if (typeParameters.isEmpty()) {
            types = getTypeClosure(classInfo, null, Collections.emptyMap(), beanDeployment, null, unrestrictedBeanTypes);
        } else {
            types = getTypeClosure(classInfo, null, buildResolvedMap(typeParameters, typeParameters,
                    Collections.emptyMap(), beanDeployment.getBeanArchiveIndex()), beanDeployment, null, unrestrictedBeanTypes);
        }
        return types;
    }

    static Map<String, Type> resolveDecoratedTypeParams(ClassInfo decoratedTypeClass, DecoratorInfo decorator) {
        // A decorated type can declare type parameters
        // For example Converter<String> should result in a T -> String mapping
        List<TypeVariable> typeParameters = decoratedTypeClass.typeParameters();
        Map<String, org.jboss.jandex.Type> resolvedTypeParameters = Collections.emptyMap();
        if (!typeParameters.isEmpty()) {
            resolvedTypeParameters = new HashMap<>();
            // The delegate type can be used to infer the parameter types
            org.jboss.jandex.Type type = decorator.getDelegateType();
            if (type.kind() == Kind.PARAMETERIZED_TYPE) {
                List<org.jboss.jandex.Type> typeArguments = type.asParameterizedType().arguments();
                for (int i = 0; i < typeParameters.size(); i++) {
                    resolvedTypeParameters.put(typeParameters.get(i).identifier(), typeArguments.get(i));
                }
            }
        }
        return resolvedTypeParameters;
    }

    static List<Type> getResolvedParameters(ClassInfo classInfo, Map<String, Type> resolvedMap,
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

    /**
     * Throws {@code DefinitionException} if given {@code type} is not a legal bean type.
     * <p>
     * This method is currently only used for synthetic beans. Legal bean types are checked
     * for producers through other means (see {@link #checkArrayType(ArrayType, AnnotationTarget) checkArrayType()}
     * and {@link #containsWildcard(Type, AnnotationTarget, boolean) containsWildcard()}).
     */
    static void checkLegalBeanType(Type type, Object beanDescription) {
        if (type.kind() == Kind.TYPE_VARIABLE) {
            throw new DefinitionException("Type variable is not a legal bean type: " + beanDescription);
        } else if (type.kind() == Kind.PARAMETERIZED_TYPE) {
            checkWildcard(type, beanDescription);
        } else if (type.kind() == Kind.ARRAY) {
            checkLegalBeanType(type.asArrayType().elementType(), beanDescription);
        }
    }

    private static void checkWildcard(Type type, Object beanDescription) {
        if (type.kind() == Kind.WILDCARD_TYPE) {
            throw new DefinitionException("Wildcard type is not a legal bean type: " + beanDescription);
        } else if (type.kind() == Kind.PARAMETERIZED_TYPE) {
            for (Type typeArgument : type.asParameterizedType().arguments()) {
                checkWildcard(typeArgument, beanDescription);
            }
        }
    }

    /**
     * Throws {@code DefinitionException} if given {@code producerFieldOrMethod},
     * whose type is given {@code arrayType}, is invalid due to the rules for arrays.
     */
    static void checkArrayType(ArrayType arrayType, AnnotationTarget producerFieldOrMethod) {
        Type elementType = arrayType.elementType();
        if (elementType.kind() == Kind.TYPE_VARIABLE) {
            throw new DefinitionException("A type variable array is not a legal bean type: " + producerFieldOrMethod);
        }
        containsWildcard(elementType, producerFieldOrMethod, true);
    }

    /**
     * Detects wildcard for given type.
     * In case the annotation target is a producer and the boolean parameter is true, throws a {@link DefinitionException}
     * based on the boolean parameter.
     * Returns true if a wildcard is detected, false otherwise.
     */
    static boolean containsWildcard(Type type, AnnotationTarget producerFieldOrMethod, boolean throwIfDetected) {
        if (type.kind().equals(Kind.WILDCARD_TYPE)) {
            if (throwIfDetected && producerFieldOrMethod != null) {
                // a producer method that has wildcard directly in its return type
                throw new DefinitionException("Producer " +
                        (producerFieldOrMethod.kind().equals(AnnotationTarget.Kind.FIELD) ? "field " : "method ") +
                        producerFieldOrMethod +
                        " declared on class " +
                        (producerFieldOrMethod.kind().equals(AnnotationTarget.Kind.FIELD)
                                ? producerFieldOrMethod.asField().declaringClass().name()
                                : producerFieldOrMethod.asMethod().declaringClass().name())
                        +
                        " contains a parameterized type with a wildcard. This type is not a legal bean type" +
                        " according to CDI specification.");
            } else {
                // a producer method with wildcard in the type hierarchy of the return type
                // OR wildcard detection for class-based beans, these still need to be skipped as they aren't valid bean types
                return true;
            }
        } else if (type.kind().equals(Kind.PARAMETERIZED_TYPE)) {
            for (Type t : type.asParameterizedType().arguments()) {
                // recursive check of all type arguments
                if (containsWildcard(t, producerFieldOrMethod, throwIfDetected)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<Type> getTypeClosure(ClassInfo classInfo, AnnotationTarget producerFieldOrMethod,
            boolean throwOnProducerWildcard,
            Map<String, Type> resolvedTypeParameters,
            BeanDeployment beanDeployment, BiConsumer<ClassInfo, Map<String, Type>> resolvedTypeVariablesConsumer,
            Set<Type> unrestrictedBeanTypes, boolean rawGeneric) {
        Set<Type> types = new HashSet<>();
        List<TypeVariable> typeParameters = classInfo.typeParameters();

        if (typeParameters.isEmpty()
                || !typeParameters.stream().allMatch(it -> resolvedTypeParameters.containsKey(it.identifier()))
                || rawGeneric) {
            // Not a parameterized type or a raw type
            types.add(Type.create(classInfo.name(), Kind.CLASS));
        } else {
            // Canonical ParameterizedType with unresolved type variables
            Type[] typeParams = new Type[typeParameters.size()];
            boolean skipThisType = false;
            for (int i = 0; i < typeParameters.size(); i++) {
                typeParams[i] = resolvedTypeParameters.get(typeParameters.get(i).identifier());
                // for producers, wildcard is not a legal bean type and results in a definition error
                // see https://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#legal_bean_types
                // NOTE: wildcard can be nested, such as List<Set<? extends Number>>
                skipThisType = containsWildcard(typeParams[i], producerFieldOrMethod, throwOnProducerWildcard);
            }
            if (resolvedTypeVariablesConsumer != null) {
                Map<String, Type> resolved = new HashMap<>();
                for (int i = 0; i < typeParameters.size(); i++) {
                    resolved.put(typeParameters.get(i).identifier(), typeParams[i]);
                }
                resolvedTypeVariablesConsumer.accept(classInfo, resolved);
            }
            if (!skipThisType) {
                types.add(ParameterizedType.create(classInfo.name(), typeParams, null));
            } else {
                unrestrictedBeanTypes.add(ParameterizedType.create(classInfo.name(), typeParams, null));
            }
        }
        // Interfaces
        for (Type interfaceType : classInfo.interfaceTypes()) {
            if (BANNED_INTERFACE_TYPES.contains(interfaceType.name())) {
                continue;
            }
            ClassInfo interfaceClassInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), interfaceType.name());
            if (interfaceClassInfo != null) {
                Map<String, Type> resolved = Collections.emptyMap();
                if (Kind.PARAMETERIZED_TYPE.equals(interfaceType.kind())) {
                    resolved = buildResolvedMap(interfaceType.asParameterizedType().arguments(),
                            interfaceClassInfo.typeParameters(), resolvedTypeParameters, beanDeployment.getBeanArchiveIndex());
                }
                types.addAll(getTypeClosure(interfaceClassInfo, producerFieldOrMethod, false, resolved, beanDeployment,
                        resolvedTypeVariablesConsumer, unrestrictedBeanTypes,
                        rawGeneric || isRawGeneric(interfaceType, interfaceClassInfo)));
            }
        }
        // Superclass
        if (classInfo.superClassType() != null) {
            ClassInfo superClassInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), classInfo.superName());
            if (superClassInfo != null) {
                Map<String, Type> resolved = Collections.emptyMap();
                if (Kind.PARAMETERIZED_TYPE.equals(classInfo.superClassType().kind())) {
                    resolved = buildResolvedMap(classInfo.superClassType().asParameterizedType().arguments(),
                            superClassInfo.typeParameters(),
                            resolvedTypeParameters, beanDeployment.getBeanArchiveIndex());
                }
                types.addAll(getTypeClosure(superClassInfo, producerFieldOrMethod, false, resolved, beanDeployment,
                        resolvedTypeVariablesConsumer, unrestrictedBeanTypes,
                        rawGeneric || isRawGeneric(classInfo.superClassType(), superClassInfo)));
            }
        }
        unrestrictedBeanTypes.addAll(types);
        return types;
    }

    // if the superclass type is CLASS *AND* and superclass info has type parameters, then it's raw type
    private static boolean isRawGeneric(Type superClassType, ClassInfo superClassInfo) {
        return Kind.CLASS.equals(superClassType.kind()) && !superClassInfo.typeParameters().isEmpty();
    }

    static Set<Type> getTypeClosure(ClassInfo classInfo, AnnotationTarget producerFieldOrMethod,
            Map<String, Type> resolvedTypeParameters,
            BeanDeployment beanDeployment, BiConsumer<ClassInfo, Map<String, Type>> resolvedTypeVariablesConsumer,
            Set<Type> unrestrictedBeanTypes) {
        return getTypeClosure(classInfo, producerFieldOrMethod, true, resolvedTypeParameters, beanDeployment,
                resolvedTypeVariablesConsumer, unrestrictedBeanTypes, false);
    }

    static Set<Type> getDelegateTypeClosure(InjectionPointInfo delegateInjectionPoint, BeanDeployment beanDeployment) {
        Set<Type> types;
        Set<Type> unrestrictedBeanTypes = new HashSet<>();
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
            types = getTypeClosure(delegateTypeClass, delegateInjectionPoint.getAnnotationTarget(), Collections.emptyMap(),
                    beanDeployment, null, unrestrictedBeanTypes);
        } else if (Kind.PARAMETERIZED_TYPE.equals(delegateType.kind())) {
            types = getTypeClosure(delegateTypeClass, delegateInjectionPoint.getAnnotationTarget(),
                    buildResolvedMap(delegateType.asParameterizedType().arguments(), delegateTypeClass.typeParameters(),
                            Collections.emptyMap(), beanDeployment.getBeanArchiveIndex()),
                    beanDeployment, null, unrestrictedBeanTypes);
        } else {
            throw new IllegalArgumentException("Unsupported return type");
        }
        return types;
    }

    static Map<ClassInfo, Map<String, Type>> resolvedTypeVariables(ClassInfo classInfo,
            BeanDeployment beanDeployment) {
        Map<ClassInfo, Map<String, Type>> resolvedTypeVariables = new HashMap<>();
        getTypeClosure(classInfo, null, Collections.emptyMap(), beanDeployment, resolvedTypeVariables::put, new HashSet<>());
        return resolvedTypeVariables;
    }

    static Set<Type> restrictBeanTypes(Set<Type> types, Set<Type> unrestrictedBeanTypes,
            Collection<AnnotationInstance> annotations, IndexView index,
            AnnotationTarget target) {
        AnnotationInstance typed = null;
        for (AnnotationInstance a : annotations) {
            if (a.name().equals(DotNames.TYPED)) {
                typed = a;
                break;
            }
        }
        Set<DotName> typedClasses = Collections.emptySet();
        if (typed != null) {
            AnnotationValue typedValue = typed.value();
            if (typedValue == null) {
                types.clear();
                types.add(ClassType.OBJECT_TYPE);
            } else {
                typedClasses = new HashSet<>();
                for (Type type : typedValue.asClassArray()) {
                    typedClasses.add(type.name());
                }
            }
        }
        // check if all classes declared in @Typed match some class in the unrestricted bean type set
        for (DotName typeName : typedClasses) {
            if (unrestrictedBeanTypes.stream().noneMatch(type -> type.name().equals(typeName))) {
                throw new DefinitionException(
                        "Cannot limit bean types to types outside of the transitive closure of bean types. Bean: " + target
                                + " illegal bean types: " + typedClasses);
            }
        }
        for (Iterator<Type> it = types.iterator(); it.hasNext();) {
            Type next = it.next();
            if (DotNames.OBJECT.equals(next.name())) {
                continue;
            }
            if (typed != null && !typedClasses.contains(next.name())) {
                it.remove();
                continue;
            }
            String className = next.name().toString();
            if (className.startsWith("java.")) {
                ClassInfo classInfo = index.getClassByName(next.name());
                if (classInfo == null || !Modifier.isPublic(classInfo.flags())) {
                    // and remove all non-public jdk types
                    it.remove();
                }
            }
        }
        return types;
    }

    static <T extends Type> Map<String, Type> buildResolvedMap(List<T> resolvedArguments,
            List<TypeVariable> typeVariables,
            Map<String, Type> resolvedTypeParameters, IndexView index) {
        Map<String, Type> resolvedMap = new HashMap<>();
        for (int i = 0; i < resolvedArguments.size(); i++) {
            resolvedMap.put(typeVariables.get(i).identifier(),
                    resolveTypeParam(resolvedArguments.get(i), resolvedTypeParameters, index));
        }
        return resolvedMap;
    }

    static Type resolveTypeParam(Type typeParam, Map<String, Type> resolvedTypeParameters, IndexView index) {
        if (typeParam.kind() == Kind.TYPE_VARIABLE) {
            return resolvedTypeParameters.getOrDefault(typeParam.asTypeVariable().identifier(), typeParam);
        } else if (typeParam.kind() == Kind.PARAMETERIZED_TYPE) {
            ParameterizedType parameterizedType = typeParam.asParameterizedType();
            ClassInfo classInfo = getClassByName(index, parameterizedType.name());
            if (classInfo != null) {
                List<TypeVariable> typeParameters = classInfo.typeParameters();
                List<Type> arguments = parameterizedType.arguments();
                Map<String, Type> resolvedMap = buildResolvedMap(arguments, typeParameters,
                        resolvedTypeParameters, index);
                Type[] typeParams = new Type[typeParameters.size()];
                for (int i = 0; i < typeParameters.size(); i++) {
                    typeParams[i] = resolveTypeParam(typeParameters.get(i), resolvedMap, index);
                }
                return ParameterizedType.create(parameterizedType.name(), typeParams, null);
            }
        }
        return typeParam;
    }

    static Type box(Type type) {
        if (type.kind() == Kind.PRIMITIVE) {
            return PrimitiveType.box(type.asPrimitiveType());
        }
        return type;
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

}
