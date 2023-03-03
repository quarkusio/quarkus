package io.quarkus.arc.processor.bcextensions;

import org.jboss.jandex.DotName;

class TypesReflection {
    static org.jboss.jandex.Type jandexType(Class<?> clazz) {
        if (clazz.isArray()) {
            int dimensions = 1;
            Class<?> componentType = clazz.getComponentType();
            while (componentType.isArray()) {
                dimensions++;
                componentType = componentType.getComponentType();
            }
            return org.jboss.jandex.ArrayType.create(jandexType(componentType), dimensions);
        }

        if (clazz.isPrimitive()) {
            if (clazz == Void.TYPE) {
                return org.jboss.jandex.Type.create(DotName.createSimple("void"), org.jboss.jandex.Type.Kind.VOID);
            } else if (clazz == Boolean.TYPE) {
                return org.jboss.jandex.PrimitiveType.BOOLEAN;
            } else if (clazz == Byte.TYPE) {
                return org.jboss.jandex.PrimitiveType.BYTE;
            } else if (clazz == Short.TYPE) {
                return org.jboss.jandex.PrimitiveType.SHORT;
            } else if (clazz == Integer.TYPE) {
                return org.jboss.jandex.PrimitiveType.INT;
            } else if (clazz == Long.TYPE) {
                return org.jboss.jandex.PrimitiveType.LONG;
            } else if (clazz == Float.TYPE) {
                return org.jboss.jandex.PrimitiveType.FLOAT;
            } else if (clazz == Double.TYPE) {
                return org.jboss.jandex.PrimitiveType.DOUBLE;
            } else if (clazz == Character.TYPE) {
                return org.jboss.jandex.PrimitiveType.CHAR;
            } else {
                throw new IllegalArgumentException("Unknown primitive type " + clazz);
            }
        }

        return org.jboss.jandex.Type.create(DotName.createSimple(clazz.getName()), org.jboss.jandex.Type.Kind.CLASS);
    }
}
