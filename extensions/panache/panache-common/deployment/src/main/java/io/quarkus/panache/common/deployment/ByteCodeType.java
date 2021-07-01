package io.quarkus.panache.common.deployment;

import static org.jboss.jandex.DotName.createSimple;
import static org.objectweb.asm.Type.BOOLEAN_TYPE;
import static org.objectweb.asm.Type.BYTE_TYPE;
import static org.objectweb.asm.Type.CHAR_TYPE;
import static org.objectweb.asm.Type.DOUBLE_TYPE;
import static org.objectweb.asm.Type.FLOAT_TYPE;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.LONG_TYPE;
import static org.objectweb.asm.Type.SHORT_TYPE;
import static org.objectweb.asm.Type.getType;

import java.util.StringJoiner;

import org.jboss.jandex.DotName;
import org.jboss.jandex.PrimitiveType;
import org.objectweb.asm.Type;

import io.quarkus.deployment.util.AsmUtil;

public class ByteCodeType {
    private final Type type;

    public ByteCodeType(Type type) {
        this.type = type;
    }

    public ByteCodeType(Class<?> type) {
        this.type = getType(type);
    }

    public ByteCodeType(org.jboss.jandex.Type type) {
        if (type.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE) {
            this.type = toAsm(type.asPrimitiveType());
        } else {
            String typeDescriptor = type.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE
                    ? type.toString()
                    : "L" + type.name().toString().replace('.', '/') + ";";
            this.type = getType(typeDescriptor);
        }
    }

    public String descriptor() {
        return type.getDescriptor();
    }

    public DotName dotName() {
        return createSimple(type.getClassName());
    }

    public String internalName() {
        return type.getInternalName();
    }

    public boolean isPrimitive() {
        return type().getSort() <= Type.DOUBLE;
    }

    private Type toAsm(PrimitiveType primitive) {
        switch (primitive.name().toString()) {
            case "byte":
                return BYTE_TYPE;
            case "char":
                return CHAR_TYPE;
            case "double":
                return DOUBLE_TYPE;
            case "float":
                return FLOAT_TYPE;
            case "int":
                return INT_TYPE;
            case "long":
                return LONG_TYPE;
            case "short":
                return SHORT_TYPE;
            case "boolean":
                return BOOLEAN_TYPE;

        }
        return null;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ByteCodeType.class.getSimpleName() + "[", "]")
                .add(type.toString())
                .toString();
    }

    public Type type() {
        return this.type;
    }

    public ByteCodeType unbox() {
        return new ByteCodeType(AsmUtil.WRAPPER_TO_PRIMITIVE.getOrDefault(type, type));
    }
}
