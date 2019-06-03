package io.quarkus.hibernate.orm.panache.deployment;

public class EntityField {

    final String name;
    final String descriptor;
    String signature;

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
