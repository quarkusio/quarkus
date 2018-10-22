package org.jboss.protean.gizmo;

import org.jboss.jandex.FieldInfo;

public class FieldDescriptor {

    private final String declaringClass;
    private final String name;
    private final String type;

    private FieldDescriptor(String declaringClass, String name, String type) {
        this.declaringClass = declaringClass.replace('.', '/');
        this.name = name;
        this.type = type;
    }

    private FieldDescriptor(FieldInfo fieldInfo) {
        this.name = fieldInfo.name();
        this.type = DescriptorUtils.typeToString(fieldInfo.type());
        this.declaringClass = fieldInfo.declaringClass().toString().replace('.', '/');
    }

    public static FieldDescriptor of(String declaringClass, String name, String type) {
        return new FieldDescriptor(DescriptorUtils.objectToInternalClassName(declaringClass), name, DescriptorUtils.objectToDescriptor(type));
    }

    public static FieldDescriptor of(String declaringClass, String name, Class<?> type) {
        return new FieldDescriptor(DescriptorUtils.objectToInternalClassName(declaringClass), name, DescriptorUtils.objectToDescriptor(type));
    }

    public static FieldDescriptor of(Class<?> declaringClass, String name, String type) {
        return new FieldDescriptor(DescriptorUtils.objectToInternalClassName(declaringClass), name, DescriptorUtils.objectToDescriptor(type));
    }

    public static FieldDescriptor of(Class<?> declaringClass, String name, Class<?> type) {
        return new FieldDescriptor(DescriptorUtils.objectToInternalClassName(declaringClass), name, DescriptorUtils.objectToDescriptor(type));
    }

    public static FieldDescriptor of(FieldInfo fieldInfo) {
        return new FieldDescriptor(fieldInfo);
    }

    public String getName() {
        return name;
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public String getType() {
        return type;
    }
}
