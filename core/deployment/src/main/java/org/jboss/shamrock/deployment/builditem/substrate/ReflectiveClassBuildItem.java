package org.jboss.shamrock.deployment.builditem.substrate;

import java.util.Arrays;
import java.util.List;

import org.jboss.builder.item.MultiBuildItem;

public final class ReflectiveClassBuildItem extends MultiBuildItem {

    private final List<String> className;
    private final boolean methods;
    private final boolean fields;

    public ReflectiveClassBuildItem(boolean methods, boolean fields, String... className) {
        this.className = Arrays.asList(className);
        this.methods = methods;
        this.fields = fields;
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
}
