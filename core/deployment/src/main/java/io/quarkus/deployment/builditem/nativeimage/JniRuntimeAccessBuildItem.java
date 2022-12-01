package io.quarkus.deployment.builditem.nativeimage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used to register a class for JNI runtime access.
 */
public final class JniRuntimeAccessBuildItem extends MultiBuildItem {

    private final List<String> className;
    private final boolean constructors;
    private final boolean methods;
    private final boolean fields;
    private final boolean finalFieldsWriteable;

    public JniRuntimeAccessBuildItem(boolean constructors, boolean methods, boolean fields, Class<?>... classes) {
        this(constructors, methods, fields, false, classes);
    }

    public JniRuntimeAccessBuildItem(boolean constructors, boolean methods, boolean fields, String... className) {
        this(constructors, methods, fields, false, className);
    }

    public JniRuntimeAccessBuildItem(boolean constructors, boolean methods, boolean fields, boolean finalFieldsWriteable,
            Class<?>... classes) {
        List<String> names = new ArrayList<>();
        for (Class<?> i : classes) {
            if (i == null) {
                throw new NullPointerException();
            }
            names.add(i.getName());
        }
        this.className = names;
        this.constructors = constructors;
        this.methods = methods;
        this.fields = fields;
        this.finalFieldsWriteable = finalFieldsWriteable;
    }

    public JniRuntimeAccessBuildItem(boolean constructors, boolean methods, boolean fields, boolean finalFieldsWriteable,
            String... className) {
        for (String i : className) {
            if (i == null) {
                throw new NullPointerException();
            }
        }
        this.className = Arrays.asList(className);
        this.constructors = constructors;
        this.methods = methods;
        this.fields = fields;
        this.finalFieldsWriteable = finalFieldsWriteable;
    }

    public List<String> getClassNames() {
        return className;
    }

    public boolean isConstructors() {
        return constructors;
    }

    public boolean isMethods() {
        return methods;
    }

    public boolean isFields() {
        return fields;
    }

    public boolean isFinalFieldsWriteable() {
        return finalFieldsWriteable;
    }
}
