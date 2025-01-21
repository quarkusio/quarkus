package io.quarkus.builder;

import org.wildfly.common.flags.Flags;

import io.smallrye.common.constraint.Assert;

/**
 * Flags which can be set on consume declarations.
 */
public final class ProduceFlags extends Flags<ProduceFlag, ProduceFlags> {

    @Override
    protected ProduceFlags value(final int bits) {
        return values[bits & enumValues.length - 1];
    }

    @Override
    protected ProduceFlags this_() {
        return this;
    }

    @Override
    protected ProduceFlag itemOf(final int index) {
        return enumValues[index];
    }

    @Override
    protected ProduceFlag castItemOrNull(final Object obj) {
        return obj instanceof ProduceFlag ? (ProduceFlag) obj : null;
    }

    @Override
    protected ProduceFlags castThis(final Object obj) {
        return (ProduceFlags) obj;
    }

    private ProduceFlags(int val) {
        super(val);
    }

    private static final ProduceFlag[] enumValues = ProduceFlag.values();
    private static final ProduceFlags[] values;

    static {
        final ProduceFlags[] flags = new ProduceFlags[1 << ProduceFlag.values().length];
        for (int i = 0; i < flags.length; i++) {
            flags[i] = new ProduceFlags(i);
        }
        values = flags;
    }

    public static ProduceFlags of(ProduceFlag flag) {
        Assert.checkNotNullParam("flag", flag);
        return values[1 << flag.ordinal()];
    }

    public static final ProduceFlags NONE = values[0];
}
