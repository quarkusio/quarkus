package io.quarkus.hibernate.accessor.deployment;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Registers fields, getter/setter methods and constructors for use with the Hibernate Accessor factory.
 * Extensions can produce this build item to contribute members without annotating the model classes.
 * <p>
 * Accessor methods are injected into the host type, where they can access its private members.
 * For strategies that use reflection, the registered members are also made available for reflection
 * in native executables. Use {@link Builder} to collect metadata from the Jandex index.
 */
public final class HibernateAccessorBuildItem extends MultiBuildItem implements Comparable<HibernateAccessorBuildItem> {

    private final TypeMetadata type;
    private final Set<FieldMetadata> fields;
    private final Set<MethodMetadata> getters;
    private final Set<MethodMetadata> setters;
    private final Set<ConstructorMetadata> constructors;

    public HibernateAccessorBuildItem(TypeMetadata type, Set<FieldMetadata> fields,
            Set<MethodMetadata> getters, Set<MethodMetadata> setters, Set<ConstructorMetadata> constructors) {
        this.type = type;
        this.fields = fields == null ? Set.of() : fields;
        this.getters = getters == null ? Set.of() : getters;
        this.setters = setters == null ? Set.of() : setters;
        this.constructors = constructors == null ? Set.of() : constructors;
    }

    /**
     * Returns the type and host into which accessor methods are injected.
     */
    public TypeMetadata getType() {
        return type;
    }

    /**
     * Returns fields registered for reading and writing.
     * <p>
     * Read-only fields and fields that remain final after
     * transformation do not receive generated writers.
     */
    public Set<FieldMetadata> getFields() {
        return fields;
    }

    /**
     * Returns methods registered for reading values.
     */
    public Set<MethodMetadata> getGetters() {
        return getters;
    }

    /**
     * Returns methods registered for writing values.
     */
    public Set<MethodMetadata> getSetters() {
        return setters;
    }

    /**
     * Returns constructors registered for instantiation.
     */
    public Set<ConstructorMetadata> getConstructors() {
        return constructors;
    }

    @Override
    public int compareTo(HibernateAccessorBuildItem o) {
        return this.type.compareTo(o.type);
    }

    @Override
    public String toString() {
        return "HibernateAccessorBuildItem{" +
                "type=" + type +
                '}';
    }

    /**
     * Collects members to register for a type. Members can be selected individually or with {@link #all(ClassInfo)}.
     */
    public static class Builder {
        private final String packageName;
        private final String type;
        private final String host;
        private final boolean hostIsPublic;
        private final boolean hostIsInterface;
        private final boolean record;
        private Set<FieldMetadata> fields;
        private Set<MethodMetadata> getters;
        private Set<MethodMetadata> setters;
        private Set<ConstructorMetadata> constructors;

        /**
         * Uses the model class itself as the host for the injected accessor methods.
         */
        public Builder(ClassInfo modelClass) {
            this.packageName = modelClass.name().packagePrefix();
            this.type = modelClass.name().toString();
            this.host = modelClass.name().toString();
            this.hostIsPublic = Modifier.isPublic(modelClass.flags());
            this.hostIsInterface = Modifier.isInterface(modelClass.flags());
            this.record = modelClass.isRecord();
        }

        /**
         * Uses an explicit host for the injected accessor methods. The host must have access to the registered members.
         *
         * @param packageName package of the registered type
         * @param type binary name of the registered type
         * @param host binary name of the type to transform
         * @param hostIsPublic whether the host is public
         * @param hostIsInterface whether the host is an interface
         * @param record whether fields should be registered as read-only, as required for records
         */
        public Builder(String packageName, String type, String host, boolean hostIsPublic, boolean hostIsInterface,
                boolean record) {
            this.packageName = packageName;
            this.type = type;
            this.host = host;
            this.hostIsPublic = hostIsPublic;
            this.hostIsInterface = hostIsInterface;
            this.record = record;
        }

        /**
         * Registers an instance field. Fields of a record are registered as read-only.
         */
        public Builder addField(FieldInfo field) {
            if (this.fields == null) {
                this.fields = new HashSet<>();
            }
            Type fieldType = field.type();
            this.fields.add(new FieldMetadata(field.name(), fieldType.descriptor(), fieldType.kind() == Type.Kind.PRIMITIVE,
                    field.declaringClass().name().toString(), record));

            return this;
        }

        /**
         * Registers an instance method with no parameters and a non-void return type.
         */
        public Builder addGetter(MethodInfo getter) {
            if (this.getters == null) {
                this.getters = new HashSet<>();
            }
            Type returnType = getter.returnType();
            this.getters.add(new MethodMetadata(getter.name(), getter.descriptor(),
                    returnType.kind() == Type.Kind.PRIMITIVE, getter.declaringClass().name().toString(),
                    Modifier.isInterface(getter.declaringClass().flags()), returnType.descriptor()));

            return this;
        }

        /**
         * Registers an instance method with one parameter.
         * <p>
         * Any return value is discarded when writing.
         * The "discarding" part is to account for "builder style" setters that are returning the instance for chained calls.
         * That returned value has no value for accessors and is just discarded.
         */
        public Builder addSetter(MethodInfo setter) {
            if (this.setters == null) {
                this.setters = new HashSet<>();
            }
            Type valueType = setter.parameterType(0);
            this.setters.add(new MethodMetadata(setter.name(), setter.descriptor(), valueType.kind() == Type.Kind.PRIMITIVE,
                    setter.declaringClass().name().toString(),
                    Modifier.isInterface(setter.declaringClass().flags()), setter.returnType().descriptor()));

            return this;
        }

        /**
         * Registers a constructor, preserving the order and types of its parameters.
         */
        public Builder addConstructor(MethodInfo constructor) {
            if (this.constructors == null) {
                this.constructors = new HashSet<>();
            }
            String descriptor = constructor.descriptor();
            List<ParameterMetadata> parameterDescriptors = new ArrayList<>();
            for (MethodParameterInfo parameter : constructor.parameters()) {
                parameterDescriptors.add(new ParameterMetadata(parameter.nameOrDefault(), parameter.type().descriptor(),
                        parameter.type().kind() == Type.Kind.PRIMITIVE));
            }
            this.constructors.add(new ConstructorMetadata(
                    constructor.declaringClass().name().toString(), host, descriptor, parameterDescriptors));
            return this;
        }

        /**
         * Registers a no-argument constructor, including one added by another bytecode transformer.
         */
        public Builder addDefaultConstructor() {
            if (this.constructors == null) {
                this.constructors = new HashSet<>();
            }
            this.constructors.add(new ConstructorMetadata(type, host, "()V", List.of()));
            return this;
        }

        /**
         * Registers instance fields, no-argument methods returning a value,
         * and one-argument methods declared by the class.
         * Registers constructors unless the class is abstract or an enum.
         * Inherited members are not collected.
         * Method names need not follow JavaBeans conventions -- there are consumers (e.g. Hibernate Validator)
         * that allow redefining what is considered a getter and a setter.
         */
        public Builder all(ClassInfo classToAccess) {
            for (FieldInfo field : classToAccess.fields()) {
                if (!Modifier.isStatic(field.flags())) {
                    addField(field);
                }
            }

            for (MethodInfo method : classToAccess.methods()) {
                if (method.isConstructor()) {
                    // we won't be creating instances of abstract classes
                    // so we should skip adding constructors for abstract classes ...
                    //
                    // Same for enums ... not something we are going to create:
                    if (!Modifier.isAbstract(classToAccess.flags()) && !classToAccess.isEnum()) {
                        addConstructor(method);
                    }
                } else if (!Modifier.isStatic(method.flags())) {
                    if (method.parametersCount() == 0
                            && method.returnType().kind() != Type.Kind.VOID) {
                        addGetter(method);
                    }
                    if (method.parametersCount() == 1) {
                        addSetter(method);
                    }
                }
            }

            return this;
        }

        /**
         * Creates the build item from the collected metadata.
         */
        public HibernateAccessorBuildItem build() {
            return new HibernateAccessorBuildItem(new TypeMetadata(packageName, type, host, hostIsPublic, hostIsInterface),
                    fields, getters,
                    setters,
                    constructors);
        }
    }

    /**
     * Metadata for a field or getter/setter method.
     * For methods, {@link #isPrimitive()} describes the getter's return value or the setter's parameter.
     */
    public interface MemberMetadata extends Comparable<MemberMetadata> {
        Comparator<MemberMetadata> COMPARATOR = Comparator.comparing(MemberMetadata::declaringClass, nullsFirst(naturalOrder()))
                .thenComparing(MemberMetadata::name, nullsFirst(naturalOrder()))
                .thenComparing(MemberMetadata::descriptor, nullsFirst(naturalOrder()));

        /**
         * Returns the field or method name.
         */
        String name();

        /**
         * Returns the JVM field or method descriptor.
         */
        String descriptor();

        /**
         * Returns whether the value being read or written is primitive.
         */
        boolean isPrimitive();

        /**
         * Returns the binary name of the class or interface declaring the member.
         */
        String declaringClass();

        @Override
        default int compareTo(MemberMetadata o) {
            return MemberMetadata.COMPARATOR.compare(this, o);
        }
    }

    /**
     * Metadata for an instance field.
     *
     * @param descriptor JVM field descriptor
     * @param declaringClass binary name of the declaring class
     * @param readOnly whether to omit the field writer independently of the transformed field's modifiers
     */
    public record FieldMetadata(String name, String descriptor, boolean isPrimitive,
            String declaringClass, boolean readOnly) implements MemberMetadata {
    }

    /**
     * Metadata for an instance getter or setter.
     *
     * @param descriptor JVM method descriptor, including parameter and return types
     * @param isPrimitive whether the getter's return value or the setter's parameter is primitive
     * @param declaringClass binary name of the declaring class or interface
     * @param isInterface whether invocation must use {@code invokeinterface}
     * @param returnDescriptor JVM descriptor of the return type, including {@code V} for void
     */
    public record MethodMetadata(String name, String descriptor, boolean isPrimitive,
            String declaringClass, boolean isInterface,
            String returnDescriptor) implements MemberMetadata {
        /**
         * Returns parameter type names suitable for native reflection registration, in declaration order.
         */
        public String[] parameterTypes() {
            org.objectweb.asm.Type[] argTypes = org.objectweb.asm.Type.getArgumentTypes(descriptor);
            String[] paramClassNames = new String[argTypes.length];
            for (int i = 0; i < argTypes.length; i++) {
                paramClassNames[i] = argTypes[i].getClassName();
            }
            return paramClassNames;
        }
    }

    /**
     * Metadata for a constructor.
     *
     * @param declaringClass binary name of the class to instantiate
     * @param host binary name of the type hosting the injected instantiation method
     * @param descriptor JVM constructor descriptor, with a void return type
     * @param parameters parameter metadata in declaration order
     */
    public record ConstructorMetadata(String declaringClass, String host, String descriptor,
            List<ParameterMetadata> parameters) implements Comparable<ConstructorMetadata> {

        private static final Comparator<ConstructorMetadata> COMPARATOR = Comparator
                .comparing(ConstructorMetadata::declaringClass, nullsFirst(naturalOrder()))
                .thenComparing(ConstructorMetadata::descriptor, nullsFirst(naturalOrder()));

        @Override
        public int compareTo(ConstructorMetadata o) {
            return COMPARATOR.compare(this, o);
        }
    }

    /**
     * Metadata for a constructor parameter, using a JVM type descriptor.
     */
    public record ParameterMetadata(String name, String descriptor, boolean isPrimitive) {
    }

    /**
     * Identifies a registered type and the host of its injected accessor methods.
     *
     * @param packageName package of the registered type; {@code null} is normalized to the empty string
     * @param name binary name of the registered type
     * @param host binary name of the type to transform
     * @param isPublic whether the host is public
     * @param isInterface whether the host is an interface
     */
    public record TypeMetadata(String packageName, String name, String host,
            boolean isPublic, boolean isInterface) implements Comparable<TypeMetadata> {

        public TypeMetadata(String packageName, String name, String host, boolean isPublic, boolean isInterface) {
            this.packageName = packageName == null ? "" : packageName;
            this.name = name;
            this.host = host;
            this.isPublic = isPublic;
            this.isInterface = isInterface;
        }

        // A non-public host (class or interface) gets injected PUBLIC STATIC accessor methods, but the
        // JVM still denies access to them from outside the host's package because the *declaring type*
        // itself isn't visible there. The generated accessor classes always live in a fixed runtime
        // package, so they can't call those methods directly. The bridge is a synthetic PUBLIC class
        // generated in the same package as the host, which can see the host's methods and simply
        // forwards to them; dispatch then targets the bridge instead of the host.
        boolean needsBridge() {
            return !isPublic();
        }

        String dispatchTarget() {
            if (needsBridge()) {
                return HibernateAccessorBridgeGenerator.bridgeFqcn(host);
            }
            return host;
        }

        // The bridge (when present) is always a plain class, even if the host itself is an interface.
        boolean dispatchTargetIsInterface() {
            return isInterface() && !needsBridge();
        }

        private static final Comparator<TypeMetadata> COMPARATOR = Comparator.comparing(TypeMetadata::packageName)
                .thenComparing(TypeMetadata::name);

        @Override
        public int compareTo(TypeMetadata o) {
            return COMPARATOR.compare(this, o);
        }
    }

}
