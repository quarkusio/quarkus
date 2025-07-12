package io.quarkus.deployment.builditem.nativeimage;

import static java.util.Arrays.stream;

import java.util.Arrays;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.graal.GraalVM;

/**
 * Used to register a class for reflection in native mode
 */
public final class ReflectiveClassBuildItem extends MultiBuildItem {

    // The names of the classes that should be registered for reflection
    private final List<String> className;
    private final boolean methods;
    private final boolean queryMethods;
    private final boolean fields;
    private final boolean classes;
    private final boolean constructors;
    private final boolean publicConstructors;
    private final boolean queryConstructors;
    private final boolean weak;
    private final boolean serialization;
    private final boolean unsafeAllocated;
    private final String reason;

    private static final Logger log = Logger.getLogger(ReflectiveClassBuildItem.class);

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

    private ReflectiveClassBuildItem(boolean constructors, boolean queryConstructors, boolean methods, boolean queryMethods,
            boolean fields, boolean getClasses, boolean weak, boolean serialization, boolean unsafeAllocated, String reason,
            Class<?>... classes) {
        this(constructors, false, queryConstructors, methods, queryMethods, fields, getClasses, weak, serialization,
                unsafeAllocated, reason, stream(classes).map(Class::getName).toArray(String[]::new));
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
        this(constructors, false, methods, false, fields, false, false, false, false, null, classes);
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
        this(constructors, false, methods, false, fields, false, false, false, classNames);
    }

    /**
     * @deprecated Use {@link ReflectiveClassBuildItem#builder(Class...)} or {@link ReflectiveClassBuildItem#builder(String...)}
     *             instead.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, boolean serialization,
            String... classNames) {
        this(constructors, false, methods, false, fields, false, serialization, false, classNames);
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

    @Deprecated(since = "3.14", forRemoval = true)
    ReflectiveClassBuildItem(boolean constructors, boolean queryConstructors, boolean methods, boolean queryMethods,
            boolean fields, boolean weak, boolean serialization,
            boolean unsafeAllocated, String... className) {
        this(constructors, false, queryConstructors, methods, queryMethods, fields, false, weak, serialization, unsafeAllocated,
                null, className);
    }

    ReflectiveClassBuildItem(boolean constructors, boolean publicConstructors, boolean queryConstructors, boolean methods,
            boolean queryMethods,
            boolean fields, boolean classes, boolean weak, boolean serialization,
            boolean unsafeAllocated, String reason, String... className) {
        for (String i : className) {
            if (i == null) {
                throw new NullPointerException();
            }
        }
        this.className = Arrays.asList(className);
        this.methods = methods;
        if (methods && queryMethods) {
            log.warnf(
                    "Both methods and queryMethods are set to true for classes: %s. queryMethods is redundant and will be ignored",
                    String.join(", ", className));
            this.queryMethods = false;
        } else {
            this.queryMethods = queryMethods;
        }
        this.fields = fields;
        this.classes = classes;
        this.constructors = constructors;
        this.publicConstructors = publicConstructors;
        if (constructors && queryConstructors) {
            log.warnf(
                    "Both constructors and queryConstructors are set to true for classes: %s. queryConstructors is redundant and will be ignored",
                    String.join(", ", className));
            this.queryConstructors = false;
        } else {
            this.queryConstructors = queryConstructors;
        }
        this.weak = weak;
        this.serialization = serialization;
        this.unsafeAllocated = unsafeAllocated;
        this.reason = reason;
    }

    public List<String> getClassNames() {
        return className;
    }

    public boolean isMethods() {
        return methods;
    }

    public boolean isQueryMethods() {
        return queryMethods;
    }

    public boolean isFields() {
        return fields;
    }

    public boolean isClasses() {
        return classes;
    }

    public boolean isConstructors() {
        return constructors;
    }

    public boolean isPublicConstructors() {
        return publicConstructors;
    }

    public boolean isQueryConstructors() {
        return queryConstructors;
    }

    /**
     * @deprecated As of GraalVM 21.2 finalFieldsWritable is no longer needed when registering fields for reflection. This will
     *             be removed in a future version of Quarkus.
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

    public String getReason() {
        return reason;
    }

    public static class Builder {
        private String[] className;
        private boolean constructors = true;
        private boolean publicConstructors = false;
        private boolean queryConstructors;
        private boolean methods;
        private boolean queryMethods;
        private boolean fields;
        private boolean classes;
        private boolean weak;
        private boolean serialization;
        private boolean unsafeAllocated;
        private String reason;

        private Builder() {
        }

        public Builder className(String[] className) {
            this.className = className;
            return this;
        }

        /**
         * Configures whether constructors should be registered for reflection (true by default).
         * Setting this enables getting all declared constructors for the class as well as invoking them reflectively.
         */
        public Builder constructors(boolean constructors) {
            this.constructors = constructors;
            return this;
        }

        public Builder constructors() {
            return constructors(true);
        }

        /**
         * Configures whether public constructors should be registered for reflection.
         * Setting this enables getting all public constructors for the class as well as invoking them reflectively.
         */
        public Builder publicConstructors(boolean publicConstructors) {
            this.publicConstructors = publicConstructors;
            return this;
        }

        public Builder publicConstructors() {
            return publicConstructors(true);
        }

        /**
         * Configures whether constructors should be registered for reflection, for query purposes only.
         * Setting this enables getting all declared constructors for the class but does not allow invoking them reflectively.
         */
        public Builder queryConstructors(boolean queryConstructors) {
            this.queryConstructors = queryConstructors;
            return this;
        }

        public Builder queryConstructors() {
            return queryConstructors(true);
        }

        /**
         * Configures whether methods should be registered for reflection.
         * Setting this enables getting all declared methods for the class as well as invoking them reflectively.
         */
        public Builder methods(boolean methods) {
            this.methods = methods;
            return this;
        }

        public Builder methods() {
            return methods(true);
        }

        /**
         * Configures whether declared methods should be registered for reflection, for query purposes only,
         * i.e. {@link Class#getDeclaredMethods()}. Setting this enables getting all declared methods for the class but
         * does not allow invoking them reflectively.
         */
        public Builder queryMethods(boolean queryMethods) {
            this.queryMethods = queryMethods;
            return this;
        }

        public Builder queryMethods() {
            return queryMethods(true);
        }

        /**
         * Configures whether fields should be registered for reflection.
         * Setting this enables getting all declared fields for the class as well as accessing them reflectively.
         */
        public Builder fields(boolean fields) {
            this.fields = fields;
            return this;
        }

        public Builder fields() {
            return fields(true);
        }

        /**
         * Configures whether declared classes should be registered for reflection.
         * Setting this enables getting all declared classes through Class.getClasses().
         */
        public Builder classes(boolean classes) {
            this.classes = classes;
            return this;
        }

        public Builder classes() {
            return classes(true);
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

        /**
         * Configures whether serialization support should be enabled for the class.
         */
        public Builder serialization(boolean serialization) {
            this.serialization = serialization;
            return this;
        }

        public Builder serialization() {
            return serialization(true);
        }

        /**
         * Configures whether the class can be allocated in an unsafe manner (through JNI).
         */
        public Builder unsafeAllocated(boolean unsafeAllocated) {
            this.unsafeAllocated = unsafeAllocated;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder unsafeAllocated() {
            return unsafeAllocated(true);
        }

        public ReflectiveClassBuildItem build() {
            return new ReflectiveClassBuildItem(constructors, publicConstructors, queryConstructors, methods, queryMethods,
                    fields, classes, weak,
                    serialization, unsafeAllocated, reason, className);
        }
    }
}
