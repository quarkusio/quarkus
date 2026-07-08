package io.quarkus.core.deployment.action.impl;

import java.io.IOException;
import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;

import io.smallrye.serial.Serialized;
import io.smallrye.serial.spi.ObjectSerializer;

/**
 * A custom {@link ObjectSerializer} for {@code smallrye-serial} that intercepts
 * {@link ConstantDesc} and {@link Constable} values, producing a {@link SerializedConstant}
 * instead of attempting standard Java serialization.
 * <p>
 * This covers strings, enums, primitive wrappers, and any other type that
 * can describe itself as a JVM constant. {@link Class} objects are not intercepted
 * because this serializer runs at a priority below {@code ClassSerializer}, which
 * handles them first and produces the correct {@code SerializedClass} subtype
 * (needed by {@code SerializedRecord} for type descriptors).
 * <p>
 * For {@link Constable} objects whose {@code describeConstable()} returns empty,
 * serialization is delegated to the next serializer in the chain.
 * <p>
 * Registered at a priority between {@code PRIORITY_CLASS} and {@code PRIORITY_BASIC},
 * so that {@code ClassSerializer} handles {@code Class} objects first, but constants
 * are intercepted before the default string/enum/record serializers.
 */
final class ConstantDescSerializer implements ObjectSerializer {

    /**
     * Singleton instance.
     */
    static final ConstantDescSerializer INSTANCE = new ConstantDescSerializer();

    private ConstantDescSerializer() {
    }

    @Override
    public Serialized serialize(Context ctxt, Object object) throws IOException {
        // Boolean, Byte, Short, Character: store as int with the original box type
        // (their describeConstable() returns DynamicConstantDesc that loads a reference,
        // but primitive slots need an int value)
        if (object instanceof Boolean b) {
            return new SerializedConstant(b ? 1 : 0, ClassDesc.of("java.lang.Boolean"));
        }
        if (object instanceof Byte b) {
            return new SerializedConstant((int) b, ClassDesc.of("java.lang.Byte"));
        }
        if (object instanceof Short s) {
            return new SerializedConstant((int) s, ClassDesc.of("java.lang.Short"));
        }
        if (object instanceof Character c) {
            return new SerializedConstant((int) c, ClassDesc.of("java.lang.Character"));
        }
        if (object instanceof ConstantDesc cd) {
            return new SerializedConstant(cd);
        }
        if (object instanceof Constable c) {
            var opt = c.describeConstable();
            if (opt.isPresent()) {
                return new SerializedConstant(opt.get());
            }
            // Constable but describeConstable() returned empty — delegate to next
        }
        return ctxt.next();
    }

    @Override
    public int priority() {
        return PRIORITY_REPLACE + 1;
    }
}
