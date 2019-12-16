package io.quarkus.panache.common.deployment;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.deployment.bean.JavaBeanUtil;

public class EntityField {

    public final String name;
    public final String descriptor;
    public String signature;
    public final Set<EntityFieldAnnotation> annotations = new HashSet<>(2);

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

    public static class EntityFieldAnnotation {
        public final String descriptor;
        public String name;
        public Object value;

        public EntityFieldAnnotation(String desc) {
            this.descriptor = desc;
        }
    }

}
