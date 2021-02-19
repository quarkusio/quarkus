package io.quarkus.rest.data.panache.deployment.utils;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

public final class ReflectionImplementor {

    public ResultHandle getFieldsAsList(BytecodeCreator creator, ResultHandle object) {
        ResultHandle clazz = creator.invokeVirtualMethod(ofMethod(Object.class, "getClass", Class.class), object);
        ResultHandle fieldsArr = getFields(creator, clazz);
        return extractListFromFields(creator, fieldsArr);
    }

    public ResultHandle getSuperclassFieldsAsList(BytecodeCreator creator, ResultHandle object) {
        ResultHandle clazz = creator.invokeVirtualMethod(ofMethod(Object.class, "getClass", Class.class), object);
        ResultHandle fieldsArr = getFields(creator,
                creator.invokeVirtualMethod(ofMethod(Class.class, "getSuperclass", Class.class), clazz));
        return extractListFromFields(creator, fieldsArr);
    }

    public ResultHandle getFields(BytecodeCreator creator, ResultHandle object) {
        return creator.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredFields", Field[].class), object);
    }

    public void setFieldAccessible(BytecodeCreator creator, ResultHandle field, boolean bool) {
        ResultHandle booleanHandle = creator.load(bool);
        creator.invokeVirtualMethod(ofMethod(Field.class, "setAccessible", void.class, boolean.class),
                field, booleanHandle);
    }

    public ResultHandle getFieldValue(BytecodeCreator creator, ResultHandle field, ResultHandle object) {
        return creator.invokeVirtualMethod(ofMethod(Field.class, "get", Object.class, Object.class),
                field, object);
    }

    public void setFieldValue(BytecodeCreator creator, ResultHandle field, ResultHandle object, ResultHandle value) {
        creator.invokeVirtualMethod(ofMethod(Field.class, "set", void.class, Object.class, Object.class),
                field, object, value);
    }

    public ResultHandle getDeclaredFieldOfSuperClass(BytecodeCreator creator, ResultHandle object, ResultHandle fieldName) {
        ResultHandle clazz = creator.invokeVirtualMethod(ofMethod(Object.class, "getClass", Class.class), object);
        ResultHandle superClazz = creator.invokeVirtualMethod(ofMethod(Class.class, "getSuperclass", Class.class), clazz);
        return creator.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredField", Field.class, String.class), superClazz,
                fieldName);
    }

    public ResultHandle getDeclaredField(BytecodeCreator creator, ResultHandle object, ResultHandle fieldName) {
        ResultHandle clazz = creator.invokeVirtualMethod(ofMethod(Object.class, "getClass", Class.class), object);
        return creator.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredField", Field.class, String.class), clazz,
                fieldName);
    }

    public ResultHandle getFieldName(BytecodeCreator creator, ResultHandle field) {
        return creator.invokeVirtualMethod(ofMethod(Field.class, "getName", String.class), field);
    }

    private ResultHandle extractListFromFields(BytecodeCreator creator, ResultHandle fieldsArray) {
        return creator.invokeStaticMethod(ofMethod(Arrays.class, "asList", List.class, Object[].class), fieldsArray);
    }

}
