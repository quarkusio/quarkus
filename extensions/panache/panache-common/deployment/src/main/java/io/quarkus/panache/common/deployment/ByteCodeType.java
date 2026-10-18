package io.quarkus.panache.common.deployment;

import java.util.Map;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.PrimitiveType;

public class ByteCodeType {
    private static final Map<String, org.jboss.jandex.Type> WRAPPER_TO_PRIMITIVE = Map.of(
            Boolean.class.getName(), PrimitiveType.BOOLEAN,
            Byte.class.getName(), PrimitiveType.BYTE,
            Short.class.getName(), PrimitiveType.SHORT,
            Integer.class.getName(), PrimitiveType.INT,
            Long.class.getName(), PrimitiveType.LONG,
            Float.class.getName(), PrimitiveType.FLOAT,
            Double.class.getName(), PrimitiveType.DOUBLE,
            Character.class.getName(), PrimitiveType.CHAR);

    private final org.jboss.jandex.Type type;

    public ByteCodeType(Class<?> type) {
        this.type = ClassType.create(DotName.createSimple(type.getName()));
    }

    public ByteCodeType(org.jboss.jandex.Type type) {
        this.type = type;
    }

    public org.jboss.jandex.Type get() {
        return type;
    }

    public String descriptor() {
        return type.descriptor();
    }

    public DotName dotName() {
        return type.name();
    }

    public String internalName() {
        return type.name().toString('/');
    }

    public boolean isPrimitive() {
        return type.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE;
    }

    @Override
    public String toString() {
        return ByteCodeType.class.getSimpleName() + "[" + type + "]";
    }

    public org.objectweb.asm.Type type() {
        return org.objectweb.asm.Type.getType(type.descriptor());
    }

    public ByteCodeType unbox() {
        return new ByteCodeType(WRAPPER_TO_PRIMITIVE.getOrDefault(type.name().toString(), type));
    }
}
