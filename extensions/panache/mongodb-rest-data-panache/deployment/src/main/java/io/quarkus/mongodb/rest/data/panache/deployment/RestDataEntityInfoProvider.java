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
import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.rest.data.panache.deployment.RestDataEntityInfo;

final class RestDataEntityInfoProvider {

    private static final DotName PANACHE_MONGO_ENTITY_BASE = DotName.createSimple(PanacheMongoEntityBase.class.getName());

    private static final DotName OBJECT_ID = DotName.createSimple(ObjectId.class.getName());

    private static final DotName BSON_ID_ANNOTATION = DotName.createSimple(BsonId.class.getName());

    private final IndexView index;

    RestDataEntityInfoProvider(IndexView index) {
        this.index = index;
    }

    RestDataEntityInfo get(String entityType, String idType) {
        ClassInfo classInfo = index.getClassByName(DotName.createSimple(entityType));
        FieldInfo idField = getIdField(classInfo);
        return new RestDataEntityInfo(classInfo.toString(), idType, idField, getSetter(classInfo, idField, idType));
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

    private MethodDescriptor getSetter(ClassInfo entityClass, FieldInfo field, String fieldType) {
        if (isPanacheEntity(entityClass)) {
            return getPanacheEntitySetter(entityClass, field.name(), fieldType);
        }
        return getGenericEntitySetter(entityClass, field);
    }

    private boolean isPanacheEntity(ClassInfo entityClass) {
        if (entityClass == null || entityClass.superName() == null) {
            return false;
        }
        if (PANACHE_MONGO_ENTITY_BASE.equals(entityClass.superName())) {
            return true;
        }
        return isPanacheEntity(index.getClassByName(entityClass.superName()));
    }

    private MethodDescriptor getPanacheEntitySetter(ClassInfo entityClass, String fieldName, String fieldType) {
        return MethodDescriptor.ofMethod(entityClass.toString(), JavaBeanUtil.getSetterName(fieldName), void.class, fieldType);
    }

    private MethodDescriptor getGenericEntitySetter(ClassInfo entityClass, FieldInfo field) {
        if (entityClass == null) {
            return null;
        }
        MethodInfo methodInfo = entityClass.method(JavaBeanUtil.getSetterName(field.name()), field.type());
        if (methodInfo != null) {
            return MethodDescriptor.of(methodInfo);
        } else if (entityClass.superName() != null) {
            return getGenericEntitySetter(index.getClassByName(entityClass.superName()), field);
        }
        return null;
    }
}
