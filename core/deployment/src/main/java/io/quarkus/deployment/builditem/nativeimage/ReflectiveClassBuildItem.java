package io.quarkus.deployment.builditem.nativeimage;

import static java.util.Arrays.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used to register a class for reflection in native mode
 */
public final class ReflectiveClassBuildItem extends MultiBuildItem {

    private final List<String> className;
    private final boolean methods;
    private final boolean fields;
    private final boolean constructors;
    private final boolean weak;
    private final boolean serialization;
    private final boolean unsafeAllocated;

    public static Builder builder(Class<?>... classes) {
        String[] classNames = stream(classes)
                .map(aClass -> {
                    if (aClass == null) {
                        throw new NullPointerException();
                    }
                    return aClass.getName();
                })
                .toArray(String[]::new);

        return new Builder().className(classNames);
    }

    public static Builder builder(String... classNames) {
        return new Builder().className(classNames);
    }

    private ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, boolean weak, boolean serialization,
            boolean unsafeAllocated, Class<?>... classes) {
        List<String> names = new ArrayList<>();
        for (Class<?> i : classes) {
            if (i == null) {
                throw new NullPointerException();
            }
            names.add(i.getName());
        }
        this.className = names;
        this.methods = methods;
        this.fields = fields;
        this.constructors = constructors;
        this.weak = weak;
        this.serialization = serialization;
        this.unsafeAllocated = unsafeAllocated;
        if (weak && serialization) {
            throw new RuntimeException("Weak reflection not supported with serialization");
        }
    }

    /**
     * @deprecated Use {@link ReflectiveClassBuildItem#builder(Class...)} or {@link ReflectiveClassBuildItem#builder(String...)}
     *             instead.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public ReflectiveClassBuildItem(boolean methods, boolean fields, Class<?>... classes) {
        this(true, methods, fields, classes);
    }

    /**
     * @deprecated Use {@link ReflectiveClassBuildItem#builder(Class...)} or {@link ReflectiveClassBuildItem#builder(String...)}
     *             instead.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, Class<?>... classes) {
        this(constructors, methods, fields, false, false, false, classes);
    }

    /**
     * @deprecated Use {@link ReflectiveClassBuildItem#builder(Class...)} or {@link ReflectiveClassBuildItem#builder(String...)}
     *             instead.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public ReflectiveClassBuildItem(boolean methods, boolean fields, String... classNames) {
        this(true, methods, fields, classNames);
    }

    /**
     * @deprecated Use {@link ReflectiveClassBuildItem#builder(Class...)} or {@link ReflectiveClassBuildItem#builder(String...)}
     *             instead.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, String... classNames) {
        this(constructors, methods, fields, false, false, false, classNames);
    }

    /**
     * @deprecated Use {@link ReflectiveClassBuildItem#builder(Class...)} or {@link ReflectiveClassBuildItem#builder(String...)}
     *             instead.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, boolean serialization,
            String... classNames) {
        this(constructors, methods, fields, false, serialization, false, classNames);
    }

    public static ReflectiveClassBuildItem weakClass(String... classNames) {
        return ReflectiveClassBuildItem.builder(classNames).constructors().methods().fields().weak().build();
    }

    /**
     * @deprecated Use {@link ReflectiveClassBuildItem#builder(Class...)} or {@link ReflectiveClassBuildItem#builder(String...)}
     *             instead.
     */
    public static ReflectiveClassBuildItem weakClass(boolean constructors, boolean methods, boolean fields,
            String... classNames) {
        return ReflectiveClassBuildItem.builder(classNames).constructors(constructors).methods(methods).fields(fields).weak()
                .build();
    }

    public static ReflectiveClassBuildItem serializationClass(String... classNames) {
        return ReflectiveClassBuildItem.builder(classNames).serialization().build();
    }

    ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, boolean weak, boolean serialization,
            boolean unsafeAllocated, String... className) {
        for (String i : className) {
            if (i == null) {
                throw new NullPointerException();
            }
        }
        this.className = Arrays.asList(className);
        this.methods = methods;
        this.fields = fields;
        this.constructors = constructors;
        this.weak = weak;
        this.serialization = serialization;
        this.unsafeAllocated = unsafeAllocated;
    }

    public List<String> getClassNames() {
        return className;
    }

    public boolean isMethods() {
        return methods;
    }

    public boolean isFields() {
        return fields;
    }

    public boolean isConstructors() {
        return constructors;
    }

    /**
     * @deprecated As of GraalVM 21.2 finalFieldsWritable is no longer needed when registering fields for reflection. This will
     *             be removed in a future verion of Quarkus.
     */
    @Deprecated
    public boolean areFinalFieldsWritable() {
        return false;
    }

    public boolean isWeak() {
        return weak;
    }

    public boolean isSerialization() {
        return serialization;
    }

    public boolean isUnsafeAllocated() {
        return unsafeAllocated;
    }

    public static class Builder {
        private String[] className;
        private boolean constructors = true;
        private boolean methods;
        private boolean fields;
        private boolean weak;
        private boolean serialization;
        private boolean unsafeAllocated;

        private Builder() {
        }

        public Builder className(String[] className) {
            this.className = className;
            return this;
        }

        public Builder constructors(boolean constructors) {
            this.constructors = constructors;
            return this;
        }

        public Builder constructors() {
            return constructors(true);
        }

        public Builder methods(boolean methods) {
            this.methods = methods;
            return this;
        }

        public Builder methods() {
            return methods(true);
        }

        public Builder fields(boolean fields) {
            this.fields = fields;
            return this;
        }

        public Builder fields() {
            return fields(true);
        }

        /**
         * @deprecated As of GraalVM 21.2 finalFieldsWritable is no longer needed when registering fields for reflection. This
         *             will be removed in a future version of Quarkus.
         */
        @Deprecated(forRemoval = true)
        public Builder finalFieldsWritable(boolean finalFieldsWritable) {
            return this;
        }

        public Builder weak(boolean weak) {
            this.weak = weak;
            return this;
        }

        public Builder weak() {
            return weak(true);
        }

        public Builder serialization(boolean serialization) {
            this.serialization = serialization;
            return this;
        }

        public Builder serialization() {
            return serialization(true);
        }

        public Builder unsafeAllocated(boolean unsafeAllocated) {
            this.unsafeAllocated = unsafeAllocated;
            return this;
        }

        public Builder unsafeAllocated() {
            return unsafeAllocated(true);
        }

        public ReflectiveClassBuildItem build() {
            return new ReflectiveClassBuildItem(constructors, methods, fields, weak, serialization, unsafeAllocated, className);
        }
    }
}
