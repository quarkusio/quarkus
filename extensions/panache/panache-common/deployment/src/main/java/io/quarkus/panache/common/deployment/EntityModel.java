package io.quarkus.panache.common.deployment;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.jandex.ClassInfo;

public class EntityModel {

    public final String name;
    public final String superClassName;
    // VERY IMPORTANT: field traversal order should not change
    public final Map<String, EntityField> fields = new LinkedHashMap<>();

    public EntityModel(ClassInfo classInfo) {
        this.name = classInfo.name().toString();
        this.superClassName = classInfo.superName().toString();
    }

    public void addField(EntityField field) {
        fields.put(field.name, field);
    }
}
