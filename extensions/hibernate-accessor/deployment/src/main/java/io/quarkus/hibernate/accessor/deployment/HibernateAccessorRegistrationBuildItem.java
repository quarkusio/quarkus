package io.quarkus.hibernate.accessor.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.accessor.asm.aot.AotConstructorMetadata;
import org.hibernate.accessor.asm.aot.AotFieldMetadata;
import org.hibernate.accessor.asm.aot.AotMethodMetadata;
import org.hibernate.accessor.asm.aot.AotParameterMetadata;
import org.hibernate.accessor.asm.aot.AotTypeMetadata;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.accessor.runtime.FieldAccess;

/**
 * Registers fields, getter/setter methods and constructors for use with the Hibernate Accessor factory.
 * Extensions can produce this build item to contribute members without annotating the model classes.
 * <p>
 * For strategies that use generated code, accessor methods are injected into the host type, where they can access its private
 * members.
 * For strategies that use reflection, the registered members are also made available for reflection
 * in native executables.
 * <p>
 * Use {@link Builder} to collect metadata from the Jandex index.
 */
public final class HibernateAccessorRegistrationBuildItem extends MultiBuildItem
        implements Comparable<HibernateAccessorRegistrationBuildItem> {

    private final AotTypeMetadata type;
    private final Set<AotFieldMetadata> fieldReaders;
    private final Set<AotFieldMetadata> fieldWriters;
    private final Set<AotMethodMetadata> getters;
    private final Set<AotMethodMetadata> setters;
    private final Set<AotConstructorMetadata> constructors;

    private HibernateAccessorRegistrationBuildItem(AotTypeMetadata type, Set<AotFieldMetadata> fieldReaders,
            Set<AotFieldMetadata> fieldWriters,
            Set<AotMethodMetadata> getters, Set<AotMethodMetadata> setters, Set<AotConstructorMetadata> constructors) {
        this.type = type;
        this.fieldReaders = Set.copyOf(fieldReaders);
        this.fieldWriters = Set.copyOf(fieldWriters);
        this.getters = getters == null ? Set.of() : Set.copyOf(getters);
        this.setters = setters == null ? Set.of() : Set.copyOf(setters);
        this.constructors = constructors == null ? Set.of() : Set.copyOf(constructors);
    }

    /**
     * Returns metadata identifying the model type and its accessor host.
     * Generated strategies inject accessor methods into the host.
     */
    public AotTypeMetadata getType() {
        return type;
    }

    /** Returns fields requested for reading. */
    public Set<AotFieldMetadata> getFieldReaders() {
        return fieldReaders;
    }

    /** Returns fields requested for writing, including fields to unfinal for generated access. */
    public Set<AotFieldMetadata> getFieldWriters() {
        return fieldWriters;
    }

    /**
     * Returns methods registered for reading values.
     */
    public Set<AotMethodMetadata> getGetters() {
        return getters;
    }

    /**
     * Returns methods registered for writing values.
     */
    public Set<AotMethodMetadata> getSetters() {
        return setters;
    }

    /**
     * Returns constructors registered for instantiation.
     */
    public Set<AotConstructorMetadata> getConstructors() {
        return constructors;
    }

    @Override
    public int compareTo(HibernateAccessorRegistrationBuildItem o) {
        return this.type.compareTo(o.type);
    }

    @Override
    public String toString() {
        return "HibernateAccessorRegistrationBuildItem{" +
                "type=" + type +
                '}';
    }

    /**
     * Collects members to register for a type. Members can be selected individually or with
     * {@link #all(ClassInfo, FieldAccess)}.
     */
    public static class Builder {
        private final String packageName;
        private final String type;
        private final String host;
        private final boolean hostIsPublic;
        private final boolean hostIsInterface;
        private String source = "programmatic accessor registration";
        private final Set<AotFieldMetadata> fieldReaders = new HashSet<>();
        private final Set<AotFieldMetadata> fieldWriters = new HashSet<>();
        private Set<AotMethodMetadata> getters;
        private Set<AotMethodMetadata> setters;
        private Set<AotConstructorMetadata> constructors;

        /**
         * Uses the model class itself as the host for the injected accessor methods.
         */
        public Builder(ClassInfo modelClass) {
            this.packageName = modelClass.name().packagePrefix();
            this.type = modelClass.name().toString();
            this.host = modelClass.name().toString();
            this.hostIsPublic = Modifier.isPublic(modelClass.flags());
            this.hostIsInterface = Modifier.isInterface(modelClass.flags());
        }

        /** Identifies the requesting extension or annotation in registration errors. */
        public Builder source(String source) {
            this.source = Objects.requireNonNull(source);
            return this;
        }

        /**
         * Registers an instance field declared by the model class for reading without requesting any modifier changes.
         */
        public Builder addFieldReader(FieldInfo field) {
            fieldReaders.add(fieldMetadata(field));
            return this;
        }

        /**
         * Registers an instance field declared by the model class for writing.
         * Generated strategies remove {@code final} from this field.
         * Record fields cannot be written, regardless of the configured strategy.
         */
        public Builder addFieldWriter(FieldInfo field) {
            if (field.declaringClass().isRecord()) {
                throw new IllegalArgumentException("Cannot register a writer for record field "
                        + field.declaringClass().name() + "#" + field.name() + " requested by " + source);
            }
            fieldWriters.add(fieldMetadata(field));
            return this;
        }

        private AotFieldMetadata fieldMetadata(FieldInfo field) {
            if (!host.equals(field.declaringClass().name().toString())) {
                throw new IllegalArgumentException("Field " + field.declaringClass().name() + "#" + field.name()
                        + " must use its declaring class as accessor host, not " + host + "; requested by " + source);
            }
            if (Modifier.isStatic(field.flags())) {
                throw new IllegalArgumentException("Cannot register static field " + field.declaringClass().name()
                        + "#" + field.name() + " requested by " + source);
            }
            Type fieldType = field.type();
            return new AotFieldMetadata(field.name(), fieldType.descriptor(), fieldType.kind() == Type.Kind.PRIMITIVE,
                    field.declaringClass().name().toString());
        }

        /**
         * Registers an instance method with no parameters and a non-void return type.
         */
        public Builder addGetter(MethodInfo getter) {
            checkNotStatic(getter);
            if (this.getters == null) {
                this.getters = new HashSet<>();
            }
            Type returnType = getter.returnType();
            this.getters.add(new AotMethodMetadata(getter.name(), getter.descriptor(),
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
            checkNotStatic(setter);
            if (this.setters == null) {
                this.setters = new HashSet<>();
            }
            Type valueType = setter.parameterType(0);
            this.setters.add(new AotMethodMetadata(setter.name(), setter.descriptor(), valueType.kind() == Type.Kind.PRIMITIVE,
                    setter.declaringClass().name().toString(),
                    Modifier.isInterface(setter.declaringClass().flags()), setter.returnType().descriptor()));

            return this;
        }

        private void checkNotStatic(MethodInfo method) {
            // Generated dispatch always invokes the member on the target instance, so a static method
            // would produce invalid bytecode.
            if (Modifier.isStatic(method.flags())) {
                throw new IllegalArgumentException("Cannot register static method " + method.declaringClass().name()
                        + "#" + method.name() + " requested by " + source);
            }
        }

        /**
         * Registers a constructor, preserving the order and types of its parameters.
         */
        public Builder addConstructor(MethodInfo constructor) {
            if (this.constructors == null) {
                this.constructors = new HashSet<>();
            }
            String descriptor = constructor.descriptor();
            List<AotParameterMetadata> parameterDescriptors = new ArrayList<>();
            for (MethodParameterInfo parameter : constructor.parameters()) {
                parameterDescriptors.add(new AotParameterMetadata(parameter.nameOrDefault(), parameter.type().descriptor(),
                        parameter.type().kind() == Type.Kind.PRIMITIVE));
            }
            this.constructors.add(new AotConstructorMetadata(
                    constructor.declaringClass().name().toString(), host, descriptor, parameterDescriptors));
            return this;
        }

        /**
         * Registers a no-argument constructor.
         * <p>
         * This works even if the constructor is generated by another bytecode transformer.
         */
        public Builder addDefaultConstructor() {
            if (this.constructors == null) {
                this.constructors = new HashSet<>();
            }
            this.constructors.add(new AotConstructorMetadata(type, host, "()V", List.of()));
            return this;
        }

        /**
         * Registers instance fields, no-argument methods returning a value,
         * and one-argument methods declared by the class.
         * Registers constructors unless the class is abstract or an enum.
         * Fields use the requested access mode; {@code null} skips fields.
         * Static and inherited members are not collected.
         * Method names need not follow JavaBeans conventions -- there are consumers (e.g. Hibernate Validator)
         * that allow redefining what is considered a getter and a setter.
         */
        public Builder all(ClassInfo classToAccess, FieldAccess fieldAccess) {
            return all(classToAccess, new MemberSelection() {
                @Override
                public FieldAccess fieldAccess(FieldInfo field) {
                    return fieldAccess;
                }

                @Override
                public boolean accepts(MethodInfo method) {
                    return true;
                }
            });
        }

        /**
         * Applies the same member selection as {@link #all(ClassInfo, FieldAccess)}, but lets the caller
         * decide member by member what is registered and how. This is the single place where the selection
         * rules live, so callers never have to restate them.
         */
        Builder all(ClassInfo classToAccess, MemberSelection selection) {
            for (FieldInfo field : classToAccess.fields()) {
                if (Modifier.isStatic(field.flags())) {
                    selection.rejected(field);
                    continue;
                }
                FieldAccess fieldAccess = selection.fieldAccess(field);
                if (fieldAccess == null) {
                    continue;
                }
                if (fieldAccess != FieldAccess.WRITE) {
                    addFieldReader(field);
                }
                if (fieldAccess != FieldAccess.READ) {
                    addFieldWriter(field);
                }
            }

            for (MethodInfo method : classToAccess.methods()) {
                if (method.isConstructor()) {
                    // we won't be creating instances of abstract classes
                    // so we should skip adding constructors for abstract classes ...
                    //
                    // Same for enums ... not something we are going to create:
                    if (Modifier.isAbstract(classToAccess.flags()) || classToAccess.isEnum()) {
                        selection.rejected(method);
                    } else if (selection.accepts(method)) {
                        addConstructor(method);
                    }
                    continue;
                }
                if (!Modifier.isStatic(method.flags())) {
                    if (method.parametersCount() == 0
                            && method.returnType().kind() != Type.Kind.VOID) {
                        if (selection.accepts(method)) {
                            addGetter(method);
                        }
                        continue;
                    }
                    if (method.parametersCount() == 1) {
                        if (selection.accepts(method)) {
                            addSetter(method);
                        }
                        continue;
                    }
                }
                selection.rejected(method);
            }

            return this;
        }

        /**
         * Decides what {@link Builder#all(ClassInfo, MemberSelection)} registers among the members its
         * selection rules accept. Members those rules reject are reported to the {@code rejected} methods,
         * so that a caller can turn an explicit request for such a member into an error.
         */
        interface MemberSelection {

            /** Returns the access to register for an instance field, or {@code null} to skip the field. */
            FieldAccess fieldAccess(FieldInfo field);

            /** Returns whether a selected constructor, getter or setter is registered. */
            boolean accepts(MethodInfo method);

            /** Called for every field the selection rules reject. */
            default void rejected(FieldInfo field) {
            }

            /** Called for every method and constructor the selection rules reject. */
            default void rejected(MethodInfo method) {
            }
        }

        /**
         * Creates the build item from the collected metadata.
         */
        public HibernateAccessorRegistrationBuildItem build() {
            return new HibernateAccessorRegistrationBuildItem(
                    new AotTypeMetadata(packageName, type, host, hostIsPublic, hostIsInterface),
                    fieldReaders, fieldWriters, getters,
                    setters,
                    constructors);
        }
    }
}
