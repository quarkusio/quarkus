package io.quarkus.arc.processor;

import java.lang.constant.ClassDesc;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.WildcardType;

import io.quarkus.arc.impl.GenericArrayTypeImpl;
import io.quarkus.arc.impl.ParameterizedTypeImpl;
import io.quarkus.arc.impl.TypeVariableImpl;
import io.quarkus.arc.impl.TypeVariableReferenceImpl;
import io.quarkus.arc.impl.WildcardTypeImpl;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.smallrye.common.annotation.SuppressForbidden;

/**
 * Utility to create runtime representation of Jandex {@link Type}s. In the code,
 * the runtime values are Gizmo 2 {@link LocalVar}s and at actual runtime, they
 * implement {@link java.lang.reflect.Type} (often, they're simply {@link Class}es).
 * <p>
 * The creator is created using {@link #of(BlockCreator)} for the given Gizmo 2
 * {@link BlockCreator} and must be used only in that block.
 * <p>
 * The creator supports optional caching of type values (call {@link #withCache(Var)})
 * and optional usage of a Jandex index to look up enclosing classes (call
 * {@link #withIndex(IndexView)}).
 */
public class RuntimeTypeCreator {
    private static final class TypeVariables {
        private final Map<String, LocalVar> typeVariables = new HashMap<>();
        private final Map<String, LocalVar> typeVariableReferences = new HashMap<>();

        LocalVar getTypeVariable(String identifier) {
            return typeVariables.get(identifier);
        }

        void setTypeVariable(String identifier, LocalVar localVar) {
            typeVariables.put(identifier, localVar);
        }

        LocalVar getTypeVariableReference(String identifier) {
            return typeVariableReferences.get(identifier);
        }

        void setTypeVariableReference(String identifier, LocalVar localVar) {
            typeVariableReferences.put(identifier, localVar);
        }

        void patchTypeVariableReferences(BlockCreator bc) {
            typeVariableReferences.forEach((identifier, reference) -> {
                LocalVar typeVar = typeVariables.get(identifier);
                if (typeVar != null) {
                    bc.invokeVirtual(MethodDesc.of(TypeVariableReferenceImpl.class,
                            "setDelegate", void.class, TypeVariableImpl.class), reference, typeVar);
                }
            });
        }
    }

    private final BlockCreator bc;

    // these fields are optional (possibly `null`)
    private final IndexView index;
    private final Var cache;
    private final Var tccl;

    /**
     * Returns a {@code RuntimeTypeCreator} for the given {@linkplain BlockCreator block}.
     * It may not be used outside of that block creator.
     *
     * @param bc the block creator, must not be {@code null}
     * @return a new runtime type creator
     */
    public static RuntimeTypeCreator of(BlockCreator bc) {
        Objects.requireNonNull(bc);
        return new RuntimeTypeCreator(bc, null, null, null);
    }

    /**
     * Returns a new {@code RuntimeTypeCreator} with the given {@link IndexView}
     * <p>
     * The other properties are taken from this instance.
     *
     * @param index the index, must not be {@code null}
     * @return a new runtime type creator
     */
    public RuntimeTypeCreator withIndex(IndexView index) {
        Objects.requireNonNull(index);
        return new RuntimeTypeCreator(bc, index, cache, tccl);
    }

    /**
     * Returns a new {@code RuntimeTypeCreator} with the given {@code cache}
     * as the backing {@link Map} of a type cache.
     * <p>
     * The other properties are taken from this instance.
     *
     * @param cache the type cache, must not be {@code null} and must represent
     *        a variable of type {@code Map}
     * @return a new runtime type creator
     */
    public RuntimeTypeCreator withCache(Var cache) {
        Objects.requireNonNull(cache);
        return new RuntimeTypeCreator(bc, index, cache, tccl);
    }

    /**
     * Returns a new {@code RuntimeTypeCreator} with the given {@code tccl}.
     * The local variable {@code tccl} must be in scope in the block for which
     * this {@code RuntimeTypeCreator} was created.
     * <p>
     * The other properties are taken from this instance.
     *
     * @param tccl the current thread's context class loader, must not be {@code null}
     * @return a new runtime type creator
     */
    public RuntimeTypeCreator withTCCL(Var tccl) {
        Objects.requireNonNull(tccl);
        return new RuntimeTypeCreator(bc, index, cache, tccl);
    }

    private RuntimeTypeCreator(BlockCreator bc, IndexView index, Var cache, Var tccl) {
        this.bc = bc;
        this.index = index;
        this.cache = cache;
        this.tccl = tccl;
    }

    /**
     * Returns a runtime representation of the given build-time type.
     *
     * @param type the Jandex type, must not be {@code null}
     * @return the runtime type as a Gizmo 2 local variable, never {@code null}
     */
    public LocalVar create(Type type) {
        Objects.requireNonNull(type);

        LocalVar result = bc.localVar("type", Const.ofNull(java.lang.reflect.Type.class));
        TypeVariables typeVariables = new TypeVariables();
        create(type, result, bc, typeVariables);
        typeVariables.patchTypeVariableReferences(bc);
        return result;
    }

    // bt* -- build time, Jandex representation
    // rt* -- runtime, Gizmo representation
    private void create(Type btType, LocalVar rtType, BlockCreator bc, TypeVariables typeVariables) {
        if (cache != null) {
            LocalVar rtCachedType = getFromCache(bc, btType);
            bc.ifElse(bc.eq(rtCachedType, Const.ofNull(java.lang.reflect.Type.class)), b1 -> {
                doCreate(btType, rtType, b1, typeVariables);
            }, b1 -> {
                b1.set(rtType, rtCachedType);
            });
        } else {
            doCreate(btType, rtType, bc, typeVariables);
        }
    }

    private void doCreate(Type btType, LocalVar rtType, BlockCreator bc, TypeVariables typeVariables) {
        if (Type.Kind.VOID.equals(btType.kind())) {
            bc.set(rtType, Const.of(void.class));
        } else if (Type.Kind.PRIMITIVE.equals(btType.kind())) {
            bc.set(rtType, switch (btType.asPrimitiveType().primitive()) {
                case BOOLEAN -> Const.of(boolean.class);
                case BYTE -> Const.of(byte.class);
                case SHORT -> Const.of(short.class);
                case INT -> Const.of(int.class);
                case LONG -> Const.of(long.class);
                case FLOAT -> Const.of(float.class);
                case DOUBLE -> Const.of(double.class);
                case CHAR -> Const.of(char.class);
            });
        } else if (Type.Kind.CLASS.equals(btType.kind())) {
            ClassType btClass = btType.asClassType();
            bc.set(rtType, doLoadClass(btClass.name().toString(), bc));
            putToCache(bc, btType, rtType);
        } else if (Type.Kind.ARRAY.equals(btType.kind())) {
            ArrayType btArray = btType.asArrayType();

            // only used to figure out if the target should be `Class` or `GenericArrayType`
            Type btElementType = btArray.elementType();

            if (btElementType.kind() == Type.Kind.PRIMITIVE || btElementType.kind() == Type.Kind.CLASS) {
                // can produce a java.lang.Class representation of the array type
                // E.g. String[] -> String[].class
                bc.set(rtType, doLoadClass(btArray.name().toString(), bc));
            } else {
                // E.g. List<String>[] -> new GenericArrayTypeImpl(new ParameterizedTypeImpl(List.class, String.class))
                Type btComponentType = btType.asArrayType().componentType();
                LocalVar rtComponentType = bc.localVar("component", Const.ofNull(java.lang.reflect.Type.class));
                create(btComponentType, rtComponentType, bc, typeVariables);
                bc.set(rtType, bc.new_(
                        ConstructorDesc.of(GenericArrayTypeImpl.class, java.lang.reflect.Type.class),
                        rtComponentType));
            }
            putToCache(bc, btType, rtType);
        } else if (Type.Kind.PARAMETERIZED_TYPE.equals(btType.kind())) {
            // E.g. List<String> -> new ParameterizedTypeImpl(List.class, String.class)
            ParameterizedType btParamType = btType.asParameterizedType();
            LocalVar rtTypeArgs = bc.localVar("typeArgs", bc.newArray(java.lang.reflect.Type.class,
                    btParamType.arguments(),
                    btTypeArg -> {
                        LocalVar rtTypeArg = bc.localVar("typeArg", Const.ofNull(java.lang.reflect.Type.class));
                        create(btTypeArg, rtTypeArg, bc, typeVariables);
                        return rtTypeArg;
                    }));
            ClassType btGenericType = ClassType.create(btParamType.name());
            LocalVar rtGenericType = bc.localVar("genericType", doLoadClass(btGenericType.name().toString(), bc));
            LocalVar rtOwner = bc.localVar("owner", Const.ofNull(java.lang.reflect.Type.class));
            if (btParamType.owner() != null) {
                create(btParamType.owner(), rtOwner, bc, typeVariables);
            } else if (index != null) {
                ClassInfo btGenericClass = index.getClassByName(btParamType.name());
                if (btGenericClass != null && btGenericClass.enclosingClass() != null) {
                    // this is not entirely precise, but generic classes with more than 1 level of nesting are very rare
                    ClassType btOwner = ClassType.create(btGenericClass.enclosingClass());
                    create(btOwner, rtOwner, bc, typeVariables);
                }
            }
            bc.set(rtType, bc.new_(
                    ConstructorDesc.of(ParameterizedTypeImpl.class, java.lang.reflect.Type.class,
                            java.lang.reflect.Type[].class, java.lang.reflect.Type.class),
                    rtGenericType, rtTypeArgs, rtOwner));
            putToCache(bc, btParamType, rtType);
        } else if (Type.Kind.TYPE_VARIABLE.equals(btType.kind())) {
            // E.g. T -> new TypeVariableImpl("T")
            TypeVariable btTypeVar = btType.asTypeVariable();
            String btIdentifier = btTypeVar.identifier();

            LocalVar rtTypeVar = typeVariables.getTypeVariable(btIdentifier);
            if (rtTypeVar == null) {
                bc.set(rtType, bc.new_(
                        ConstructorDesc.of(TypeVariableImpl.class, String.class, java.lang.reflect.Type[].class),
                        Const.of(btIdentifier), bc.newArray(java.lang.reflect.Type.class, btTypeVar.bounds(),
                                btBound -> {
                                    LocalVar rtBound = bc.localVar("bound", Const.ofNull(java.lang.reflect.Type.class));
                                    create(btBound, rtBound, bc, typeVariables);
                                    return rtBound;
                                })));
                typeVariables.setTypeVariable(btIdentifier, rtType);
                putToCache(bc, btTypeVar, rtType);
            } else {
                bc.set(rtType, rtTypeVar);
            }
        } else if (Type.Kind.TYPE_VARIABLE_REFERENCE.equals(btType.kind())) {
            String btIdentifier = btType.asTypeVariableReference().identifier();

            LocalVar rtTypeVarRef = typeVariables.getTypeVariableReference(btIdentifier);
            if (rtTypeVarRef == null) {
                bc.set(rtType, bc.new_(
                        ConstructorDesc.of(TypeVariableReferenceImpl.class, String.class),
                        Const.of(btIdentifier)));
                typeVariables.setTypeVariableReference(btIdentifier, rtType);
            } else {
                bc.set(rtType, rtTypeVarRef);
            }
        } else if (Type.Kind.WILDCARD_TYPE.equals(btType.kind())) {
            // E.g. ? extends Number -> WildcardTypeImpl.withUpperBound(Number.class)
            WildcardType btWildcard = btType.asWildcardType();
            if (btWildcard.superBound() == null) {
                Type btUpperBound = btWildcard.extendsBound();
                LocalVar rtUpperBound = bc.localVar("upperBound", Const.ofNull(java.lang.reflect.Type.class));
                create(btUpperBound, rtUpperBound, bc, typeVariables);
                bc.set(rtType, bc.invokeStatic(
                        MethodDesc.of(WildcardTypeImpl.class, "withUpperBound",
                                java.lang.reflect.WildcardType.class, java.lang.reflect.Type.class),
                        rtUpperBound));
            } else {
                Type btLowerBound = btWildcard.superBound();
                LocalVar rtLowerBound = bc.localVar("lowerBound", Const.ofNull(java.lang.reflect.Type.class));
                create(btLowerBound, rtLowerBound, bc, typeVariables);
                bc.set(rtType, bc.invokeStatic(
                        MethodDesc.of(WildcardTypeImpl.class, "withLowerBound",
                                java.lang.reflect.WildcardType.class, java.lang.reflect.Type.class),
                        rtLowerBound));
            }
            putToCache(bc, btWildcard, rtType);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + btType.kind() + ", " + btType);
        }
    }

    @SuppressForbidden(reason = "Using Type.toString() to build a cache key")
    private LocalVar getFromCache(BlockCreator bc, org.jboss.jandex.Type type) {
        if (cache == null) {
            return null;
        }
        return bc.localVar("cachedType", java.lang.reflect.Type.class,
                bc.withMap(cache).get(Const.of(type.kind() + ":" + type)));
    }

    @SuppressForbidden(reason = "Using Type.toString() to build a cache key")
    private void putToCache(BlockCreator bc, org.jboss.jandex.Type type, LocalVar value) {
        if (cache == null) {
            return;
        }
        bc.withMap(cache).put(Const.of(type.kind() + ":" + type), value);
    }

    private Expr doLoadClass(String className, BlockCreator bc) {
        if (className.startsWith("java.")) {
            return Const.of(ClassDesc.of(className));
        } else {
            Expr cl = tccl != null ? tccl : bc.invokeVirtual(MethodDescs.THREAD_GET_TCCL, bc.currentThread());

            // we need to use Class.forName as the class may be package private
            return bc.invokeStatic(MethodDescs.CL_FOR_NAME, Const.of(className), Const.of(false), cl);
        }
    }
}
