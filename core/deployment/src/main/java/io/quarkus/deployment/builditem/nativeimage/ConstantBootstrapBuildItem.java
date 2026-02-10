package io.quarkus.deployment.builditem.nativeimage;

import static java.lang.constant.ConstantDescs.CD_Class;
import static java.lang.constant.ConstantDescs.CD_MethodHandles_Lookup;
import static java.lang.constant.ConstantDescs.CD_MethodType;
import static java.lang.constant.ConstantDescs.CD_String;

import java.lang.constant.ClassDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Objects;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * A constant bootstrap method which needs to be registered with GraalVM.
 * Constant bootstrap methods are {@code static} methods which are used to initialize
 * a <em>dynamic constant</em> or an <em>{@code invokedynamic} call site</em>.
 * The method must define at least these parameters, in this order:
 * <ul>
 * <li>{@code MethodHandles.Lookup}</li>
 * <li>{@code String} (the constant name)</li>
 * <li>For dynamic constants, {@code Class} (the constant type); for {@code invokedynamic} call sites, {@code MethodType} (the
 * call site method type); for bootstraps that can be used either way, the type should be
 * {@code java.lang.invoke.TypeDescriptor}</li>
 * </ul>
 * Furthermore, the return type of dynamic constant bootstraps must match the {@code Class} that is passed in,
 * whereas the return type of {@code invokedynamic} bootstraps must be {@code CallSite} or a subclass thereof.
 * <p>
 * The constructor of this item will validate as many of these constraints as possible.
 */
public final class ConstantBootstrapBuildItem extends MultiBuildItem {
    private static final Set<String> validArg2Types = Set.of(
            CD_Class.descriptorString(),
            CD_MethodType.descriptorString(),
            ClassDesc.of("java.lang.invoke.TypeDescriptor").descriptorString());

    private final String moduleName;
    private final ClassDesc classDesc;
    private String className;
    private final String methodName;
    private final MethodTypeDesc methodTypeDesc;
    private final int hashCode;

    /**
     * Construct a new instance.
     *
     * @param moduleName the module name of the type containing the method (must not be {@code null})
     * @param classDesc the descriptor of the type containing the method (must not be {@code null})
     * @param methodName the name of the bootstrap method (must not be {@code null})
     * @param methodTypeDesc the descriptor of the type of the bootstrap method (must not be {@code null})
     */
    public ConstantBootstrapBuildItem(final String moduleName, final ClassDesc classDesc, final String methodName,
            final MethodTypeDesc methodTypeDesc) {
        // todo: null check
        this.moduleName = moduleName;
        this.classDesc = Assert.checkNotNullParam("classDesc", classDesc);
        if (!classDesc.isClassOrInterface()) {
            throw new IllegalArgumentException("classDesc must refer to a class or interface");
        }
        this.methodName = Assert.checkNotEmptyParam("methodName", Assert.checkNotNullParam("methodName", methodName));
        this.methodTypeDesc = Assert.checkNotNullParam("methodTypeDesc", methodTypeDesc);
        int pc = methodTypeDesc.parameterCount();
        if (pc < 3) {
            throw new IllegalArgumentException("Bootstrap methods have at least 3 parameters");
        }
        if (!methodTypeDesc.parameterType(0).descriptorString().equals(CD_MethodHandles_Lookup.descriptorString())) {
            throw new IllegalArgumentException(
                    "Bootstrap method must have class MethodHandles.Lookup as the first parameter type");
        }
        if (!methodTypeDesc.parameterType(1).descriptorString().equals(CD_String.descriptorString())) {
            throw new IllegalArgumentException("Bootstrap method must have class String as the second parameter type");
        }
        if (!validArg2Types.contains(methodTypeDesc.parameterType(2).descriptorString())) {
            throw new IllegalArgumentException(
                    "Bootstrap method must have a valid constant type descriptor type class as the third parameter type");
        }
        hashCode = Objects.hash(moduleName, classDesc.descriptorString(), methodName, methodTypeDesc.descriptorString());
    }

    /**
     * Construct a new instance.
     *
     * @param moduleName the module name of the type containing the method (must not be {@code null})
     * @param desc the descriptor of the bootstrap method (must not be {@code null})
     */
    public ConstantBootstrapBuildItem(final String moduleName, final DirectMethodHandleDesc desc) {
        this(moduleName, Assert.checkNotNullParam("desc", desc).owner(), desc.methodName(), desc.invocationType());
    }

    /**
     * {@return the module name (not {@code null})}
     */
    public String moduleName() {
        return moduleName;
    }

    /**
     * {@return the owner's class descriptor (not {@code null})}
     */
    public ClassDesc classDesc() {
        return classDesc;
    }

    /**
     * {@return the owner's class name (not {@code null})}
     */
    public String className() {
        String className = this.className;
        if (className == null) {
            String ds = classDesc.descriptorString();
            className = this.className = ds.substring(1, ds.length() - 1).replace('/', '.');
        }
        return className;
    }

    /**
     * {@return the bootstrap method name (not {@code null})}
     */
    public String methodName() {
        return methodName;
    }

    /**
     * {@return the bootstrap method type descriptor (not {@code null})}
     */
    public MethodTypeDesc methodTypeDesc() {
        return methodTypeDesc;
    }

    /**
     * {@return {@code true} if this item is equal to the given object, or {@code false} if it is not}
     *
     * @param obj the other object
     */
    public boolean equals(final Object obj) {
        return obj instanceof ConstantBootstrapBuildItem other && equals(other);
    }

    /**
     * {@return {@code true} if this item is equal to the given item, or {@code false} if it is not}
     *
     * @param other the other item
     */
    public boolean equals(final ConstantBootstrapBuildItem other) {
        return this == other || other != null && hashCode == other.hashCode
                && moduleName.equals(other.moduleName)
                && classDesc.descriptorString().equals(other.classDesc.descriptorString())
                && methodName.equals(other.methodName)
                && methodTypeDesc.descriptorString().equals(other.methodTypeDesc.descriptorString());
    }

    /**
     * {@return the object hash code}
     */
    public int hashCode() {
        return hashCode;
    }

    /**
     * {@return a human-readable representation of this object (not {@code null})}
     */
    public String toString() {
        return "ConstantBootstrapBuildItem[" + moduleName + "/" + className() + ":" + methodName()
                + methodTypeDesc.descriptorString() + "]";
    }
}
