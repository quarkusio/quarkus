package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import jakarta.persistence.Id;

import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo.MethodDescriptor;

public class EntityClassHelper {

    private static final DotName JAVAX_PERSISTENCE_ID = DotName.createSimple(Id.class.getName());
    private final IndexView index;

    public EntityClassHelper(IndexView index) {
        this.index = index;
    }

    public FieldInfo getIdField(String className) {
        return getIdField(index.getClassByName(DotName.createSimple(className)));
    }

    private FieldInfo getIdField(ClassInfo classInfo) {
        ClassInfo tmpClassInfo = classInfo;
        while (tmpClassInfo != null) {
            for (FieldInfo field : tmpClassInfo.fields()) {
                if (field.hasAnnotation(JAVAX_PERSISTENCE_ID)) {
                    return field;
                }
            }
            if (tmpClassInfo.superName() != null) {
                tmpClassInfo = index.getClassByName(tmpClassInfo.superName());
            } else {
                tmpClassInfo = null;
            }
        }
        throw new IllegalArgumentException("Couldn't find id field of " + classInfo);
    }

    public MethodDescriptor getSetter(String className, FieldInfo field) {
        return getSetter(index.getClassByName(DotName.createSimple(className)), field);
    }

    private MethodDescriptor getSetter(ClassInfo entityClass, FieldInfo field) {
        MethodDescriptor setter = getMethod(entityClass, JavaBeanUtil.getSetterName(field.name()), field.type());
        if (setter != null) {
            return setter;
        }
        return MethodDescriptor.ofMethod(entityClass.toString(),
                EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + field.name(), void.class, field.type().name().toString());
    }

    private MethodDescriptor getMethod(ClassInfo entityClass, String name, Type... parameters) {
        if (entityClass == null) {
            return null;
        }
        MethodInfo methodInfo = entityClass.method(name, parameters);
        if (methodInfo != null) {
            return MethodDescriptor.of(methodInfo);
        } else if (entityClass.superName() != null) {
            return getMethod(index.getClassByName(entityClass.superName()), name, parameters);
        }
        return null;
    }
}
