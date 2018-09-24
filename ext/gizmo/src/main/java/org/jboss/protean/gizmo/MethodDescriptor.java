package org.jboss.protean.gizmo;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import org.jboss.jandex.MethodInfo;


public class MethodDescriptor {

    private final String declaringClass;
    private final String name;
    private final String returnType;
    private final String[] parameterTypes;
    private final String descriptor;

    private MethodDescriptor(String declaringClass, String name, String returnType, String... parameterTypes) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.descriptor = DescriptorUtils.methodSignitureToDescriptor(returnType, parameterTypes);
        for (String p : parameterTypes) {
            if (p.length() != 1) {
                if (!(p.startsWith("L") && p.endsWith(";") || p.startsWith("["))) {
                    throw new IllegalArgumentException("Invalid parameter type " + p + " it must be in the JVM descriptor format");
                }
            }
        }
        if (returnType.length() != 1) {
            if (!(returnType.startsWith("L") || returnType.startsWith("[")) || !returnType.endsWith(";")) {
                throw new IllegalArgumentException("Invalid return type " + returnType + " it must be in the JVM descriptor format");
            }
        }
    }

    private MethodDescriptor(MethodInfo info) {
        this.name = info.name();
        this.returnType = DescriptorUtils.typeToString(info.returnType());
        String[] paramTypes = new String[info.parameters().size()];
        for (int i = 0; i < paramTypes.length; ++i) {
            paramTypes[i] = DescriptorUtils.typeToString(info.parameters().get(i));
        }
        this.parameterTypes = paramTypes;
        this.declaringClass = info.declaringClass().toString().replace(".", "/");
        this.descriptor = DescriptorUtils.methodSignitureToDescriptor(returnType, parameterTypes);
    }

    public static MethodDescriptor ofMethod(String declaringClass, String name, String returnType, String... parameterTypes) {
        return new MethodDescriptor(DescriptorUtils.objectToInternalClassName(declaringClass), name, DescriptorUtils.objectToDescriptor(returnType), DescriptorUtils.objectsToDescriptor(parameterTypes));
    }

    public static MethodDescriptor ofMethod(Class<?> declaringClass, String name, Class<?> returnType, Class<?>... parameterTypes) {
        String[] args = new String[parameterTypes.length];
        for (int i = 0; i < args.length; ++i) {
            args[i] = DescriptorUtils.classToStringRepresentation(parameterTypes[i]);
        }
        return new MethodDescriptor(DescriptorUtils.objectToInternalClassName(declaringClass), name, DescriptorUtils.classToStringRepresentation(returnType), args);
    }

    public static MethodDescriptor ofMethod(Method method) {
        return ofMethod(method.getDeclaringClass(), method.getName(), method.getReturnType(), method.getParameterTypes());
    }

    public static MethodDescriptor ofMethod(Object declaringClass, String name, Object returnType, Object... parameterTypes) {
        return new MethodDescriptor(DescriptorUtils.objectToInternalClassName(declaringClass), name, DescriptorUtils.objectToDescriptor(returnType), DescriptorUtils.objectsToDescriptor(parameterTypes));
    }

    public static MethodDescriptor ofConstructor(String declaringClass, String... parameterTypes) {
        return ofMethod(declaringClass, "<init>", void.class.getName(), parameterTypes);
    }

    public static MethodDescriptor ofConstructor(Class<?> declaringClass, Class<?>... parameterTypes) {
        return ofMethod(declaringClass, "<init>", void.class, (Object[]) parameterTypes);
    }

    public static MethodDescriptor ofConstructor(Object declaringClass, Object... parameterTypes) {
        return ofMethod(declaringClass, "<init>", void.class, (Object[]) parameterTypes);
    }

    public static MethodDescriptor of(MethodInfo methodInfo) {
        return new MethodDescriptor(methodInfo);
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodDescriptor that = (MethodDescriptor) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(returnType, that.returnType) &&
                Arrays.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(name, returnType);
        result = 31 * result + Arrays.hashCode(parameterTypes);
        return result;
    }

    @Override
    public String toString() {
        return "MethodDescriptor{" +
                "name='" + name + '\'' +
                ", returnType='" + returnType + '\'' +
                ", parameterTypes=" + Arrays.toString(parameterTypes) +
                '}';
    }

    public String getDescriptor() {
        return descriptor;

    }
}
