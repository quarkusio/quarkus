package io.quarkus.panache.common.deployment;

import io.quarkus.deployment.bean.JavaBeanUtil;

public class EntityField {

    public final String name;
    public final String descriptor;
    public String signature;

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
