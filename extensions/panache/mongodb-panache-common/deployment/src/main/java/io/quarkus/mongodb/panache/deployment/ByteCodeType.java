package io.quarkus.mongodb.panache.deployment;

import static org.jboss.jandex.DotName.createSimple;
import static org.objectweb.asm.Type.getType;

import java.util.StringJoiner;

import org.jboss.jandex.DotName;
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
        String typeDescriptor = type.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE
                ? type.toString()
                : "L" + type.name().toString().replace('.', '/') + ";";
        this.type = getType(typeDescriptor);
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
