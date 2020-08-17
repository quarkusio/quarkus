package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import javax.persistence.Id;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.rest.data.panache.deployment.RestDataEntityInfo;

final class RestDataEntityInfoProvider {

    private final IndexView index;

    RestDataEntityInfoProvider(IndexView index) {
        this.index = index;
    }

    RestDataEntityInfo get(String entityType, String idType) {
        ClassInfo classInfo = index.getClassByName(DotName.createSimple(entityType));
        FieldInfo idField = getIdField(classInfo);
        return new RestDataEntityInfo(classInfo.toString(), idType, idField, getSetter(classInfo, idField));
    }

    private FieldInfo getIdField(ClassInfo classInfo) {
        ClassInfo tmpClassInfo = classInfo;
        while (tmpClassInfo != null) {
            for (FieldInfo field : tmpClassInfo.fields()) {
                if (field.hasAnnotation(DotName.createSimple(Id.class.getName()))) {
                    return field;
                }
            }
            if (classInfo.superName() != null) {
                tmpClassInfo = index.getClassByName(classInfo.superName());
            } else {
                tmpClassInfo = null;
            }
        }
        throw new IllegalArgumentException("Couldn't find id field of " + classInfo);
    }

    private MethodDescriptor getSetter(ClassInfo entityClass, FieldInfo field) {
        if (entityClass == null) {
            return null;
        }
        MethodInfo methodInfo = entityClass.method(JavaBeanUtil.getSetterName(field.name()), field.type());
        if (methodInfo != null) {
            return MethodDescriptor.of(methodInfo);
        } else if (entityClass.superName() != null) {
            return getSetter(index.getClassByName(entityClass.superName()), field);
        }
        return null;
    }
}
