package io.quarkus.mongodb.rest.data.panache.deployment;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo.MethodDescriptor;

public class EntityClassHelper {

    private static final DotName OBJECT_ID = DotName.createSimple(ObjectId.class.getName());

    private static final DotName BSON_ID_ANNOTATION = DotName.createSimple(BsonId.class.getName());

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
                if (field.type().name().equals(OBJECT_ID) || field.hasAnnotation(BSON_ID_ANNOTATION)) {
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
