package io.quarkus.rest.data.panache.deployment.utils;

import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

public final class FieldAccessImplementor {

    private final IndexView index;

    private final Predicate<FieldInfo> idFieldPredicate;

    public FieldAccessImplementor(IndexView index, Predicate<FieldInfo> idFieldPredicate) {
        this.index = index;
        this.idFieldPredicate = idFieldPredicate;
    }

    public ResultHandle getId(BytecodeCreator creator, String className, ResultHandle instance) {
        ClassInfo classInfo = index.getClassByName(DotName.createSimple(className));
        FieldInfo field = getIdField(classInfo);
        if (field == null) {
            throw new RuntimeException(className + " does not have a field annotated with @Id");
        }

        MethodInfo getter = getGetter(classInfo, field);
        if (getter != null) {
            return creator.invokeVirtualMethod(getter, instance);
        }

        return creator.readInstanceField(field, instance);
    }

    public void setId(BytecodeCreator creator, String className, ResultHandle instance, ResultHandle value) {
        ClassInfo classInfo = index.getClassByName(DotName.createSimple(className));
        FieldInfo field = getIdField(classInfo);
        if (field == null) {
            throw new RuntimeException(className + " does not have a field annotated with @Id");
        }

        MethodInfo setter = getSetter(classInfo, field);

        if (setter != null) {
            creator.invokeVirtualMethod(setter, instance, value);
        } else {
            creator.writeInstanceField(field, instance, value);
        }
    }

    private FieldInfo getIdField(ClassInfo entityClass) {
        for (FieldInfo field : entityClass.fields()) {
            if (idFieldPredicate.test(field)) {
                return field;
            }
        }

        if (entityClass.superName() != null) {
            ClassInfo superClass = index.getClassByName(entityClass.superName());
            if (superClass != null) {
                return getIdField(superClass);
            }
        }

        return null;
    }

    private MethodInfo getGetter(ClassInfo entityClass, FieldInfo field) {
        MethodInfo getter = entityClass.method(JavaBeanUtil.getGetterName(field.name(), field.type().name().toString()));
        if (getter != null) {
            return getter;
        }

        if (entityClass.superName() != null) {
            ClassInfo superClass = index.getClassByName(entityClass.superName());
            if (superClass != null) {
                getGetter(superClass, field);
            }
        }

        return null;
    }

    private MethodInfo getSetter(ClassInfo entityClass, FieldInfo field) {
        MethodInfo setter = entityClass.method(JavaBeanUtil.getSetterName(field.name()), field.type());
        if (setter != null) {
            return setter;
        }

        if (entityClass.superName() != null) {
            ClassInfo superClass = index.getClassByName(entityClass.superName());
            if (superClass != null) {
                getSetter(superClass, field);
            }
        }

        return null;
    }
}
