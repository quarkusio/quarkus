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
    private final boolean finalFieldsWritable;
    private final boolean weak;
    private final boolean serialization;

    public ReflectiveClassBuildItem(boolean methods, boolean fields, Class<?>... className) {
        this(true, methods, fields, className);
    }

    public ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, Class<?>... className) {
        this(constructors, methods, fields, false, false, className);
    }

    private ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, boolean finalFieldsWritable,
            boolean weak, Class<?>... className) {
        this(constructors, methods, fields, false, false, false, className);
    }

    private ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, boolean finalFieldsWritable,
            boolean weak, boolean serialization, Class<?>... className) {
        List<String> names = new ArrayList<>();
        for (Class<?> i : className) {
            if (i == null) {
                throw new NullPointerException();
            }
            names.add(i.getName());
        }
        this.className = names;
        this.methods = methods;
        this.fields = fields;
        this.constructors = constructors;
        this.finalFieldsWritable = finalFieldsWritable;
        this.weak = weak;
        this.serialization = serialization;
        if (weak) {
            if (serialization) {
                throw new RuntimeException("Weak reflection not supported with serialization");
            }
            if (finalFieldsWritable) {
                throw new RuntimeException("Weak reflection not supported with finalFieldsWritable");
            }
        }
    }

    public ReflectiveClassBuildItem(boolean methods, boolean fields, String... className) {
        this(true, methods, fields, className);
    }

    public ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, String... className) {
        this(constructors, methods, fields, false, false, className);
    }

    public ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, boolean serialization,
            String... className) {
        this(constructors, methods, fields, false, false, serialization, className);
    }

    public static ReflectiveClassBuildItem weakClass(String... className) {
        return new ReflectiveClassBuildItem(true, true, true, false, true, className);
    }

    public static ReflectiveClassBuildItem serializationClass(boolean methods, boolean fields, String... className) {
        return new ReflectiveClassBuildItem(true, methods, fields, false, false, true, className);
    }

    private ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, boolean finalFieldsWritable,
            boolean weak, String... className) {
        this(constructors, methods, fields, finalFieldsWritable, weak, false, className);
    }

    private ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, boolean finalFieldsWritable,
            boolean weak, boolean serialization, String... className) {
        for (String i : className) {
            if (i == null) {
                throw new NullPointerException();
            }
        }
        this.className = Arrays.asList(className);
        this.methods = methods;
        this.fields = fields;
        this.constructors = constructors;
        this.finalFieldsWritable = finalFieldsWritable;
        this.weak = weak;
        this.serialization = serialization;
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

    public boolean areFinalFieldsWritable() {
        return finalFieldsWritable;
    }

    public boolean isWeak() {
        return weak;
    }

    public boolean isSerialization() {
        return serialization;
    }

    public static Builder builder(Class<?>... className) {
        String[] classNameStrings = stream(className)
                .map(aClass -> {
                    if (aClass == null) {
                        throw new NullPointerException();
                    }
                    return aClass.getName();
                })
                .toArray(String[]::new);

        return new Builder()
                .className(classNameStrings);
    }

    public static Builder builder(String... className) {
        return new Builder()
                .className(className);
    }

    public static class Builder {
        private String[] className;
        private boolean constructors = true;
        private boolean methods;
        private boolean fields;
        private boolean finalFieldsWritable;
        private boolean weak;
        private boolean serialization;

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

        public Builder methods(boolean methods) {
            this.methods = methods;
            return this;
        }

        public Builder fields(boolean fields) {
            this.fields = fields;
            return this;
        }

        public Builder finalFieldsWritable(boolean finalFieldsWritable) {
            this.finalFieldsWritable = finalFieldsWritable;
            return this;
        }

        public Builder weak(boolean weak) {
            this.weak = weak;
            return this;
        }

        public Builder serialization(boolean serialize) {
            this.serialization = serialization;
            return this;
        }

        public ReflectiveClassBuildItem build() {
            return new ReflectiveClassBuildItem(constructors, methods, fields, finalFieldsWritable, weak, className);
        }
    }
}
