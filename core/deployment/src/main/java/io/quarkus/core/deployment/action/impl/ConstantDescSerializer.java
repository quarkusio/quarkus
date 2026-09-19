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
        return switch (object) {
            case Boolean b ->
                new SerializedConstant(Integer.valueOf(b.booleanValue() ? 1 : 0), ClassDesc.of("java.lang.Boolean"));
            case Byte b -> new SerializedConstant(Integer.valueOf(b.intValue()), ClassDesc.of("java.lang.Byte"));
            case Short s -> new SerializedConstant(Integer.valueOf(s.intValue()), ClassDesc.of("java.lang.Short"));
            case Character c -> new SerializedConstant(Integer.valueOf(c.charValue()), ClassDesc.of("java.lang.Character"));
            case ConstantDesc cd -> new SerializedConstant(cd);
            case Constable c -> {
                var opt = c.describeConstable();
                if (opt.isPresent()) {
                    yield new SerializedConstant(opt.get());
                }
                // Constable but describeConstable() returned empty — delegate to next
                yield ctxt.next();
            }
            case null, default -> ctxt.next();
        };
    }

    @Override
    public int priority() {
        return PRIORITY_REPLACE + 1;
    }
}
