package io.quarkus.panache.jpa;

public class EntityField {

    final String name;
    final String descriptor;

    public EntityField(String name, String descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }

    public String getGetterName() {
        return JavaBeanUtil.getGetterName(name, descriptor);
    }

    public String getSetterName() {
        return JavaBeanUtil.getSetterName(name);
    }

}
