package io.quarkus.panache.jpa;

import java.util.Map;

import org.jboss.jandex.ClassInfo;

public class EntityModel {

    final String name;
    final String superClassName;
    final Map<String, EntityField> fields;

    public EntityModel(ClassInfo classInfo, Map<String, EntityField> fields) {
        this.name = classInfo.name().toString();
        this.superClassName = classInfo.superName().toString();
        this.fields = fields;
    }

}
