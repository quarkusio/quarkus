package io.quarkus.builder;

import org.wildfly.common.flags.Flags;

import io.smallrye.common.constraint.Assert;

/**
 * Flags which can be set on consume declarations.
 *
 * @deprecated For raw steps, prefer {@link io.quarkus.qlue.ConsumeFlags} instead.
 */
@Deprecated
public final class ConsumeFlags extends Flags<ConsumeFlag, ConsumeFlags> {
    final io.quarkus.qlue.ConsumeFlags realFlags;

    @Override
    protected ConsumeFlags value(final int bits) {
        return values[bits & enumValues.length - 1];
    }

    @Override
    protected ConsumeFlags this_() {
        return this;
    }

    @Override
    protected ConsumeFlag itemOf(final int index) {
        return enumValues[index];
    }

    @Override
    protected ConsumeFlag castItemOrNull(final Object obj) {
        return obj instanceof ConsumeFlag ? (ConsumeFlag) obj : null;
    }

    @Override
    protected ConsumeFlags castThis(final Object obj) {
        return (ConsumeFlags) obj;
    }

    public io.quarkus.qlue.ConsumeFlags getRealFlags() {
        return realFlags;
    }

    private ConsumeFlags(int val) {
        super(val);
        realFlags = io.quarkus.qlue.ConsumeFlags.value(val);
    }

    private static final ConsumeFlag[] enumValues = ConsumeFlag.values();
    private static final ConsumeFlags[] values;

    static {
        final ConsumeFlags[] flags = new ConsumeFlags[1 << ConsumeFlag.values().length];
        for (int i = 0; i < flags.length; i++) {
            flags[i] = new ConsumeFlags(i);
        }
        values = flags;
    }

    public static ConsumeFlags of(ConsumeFlag flag) {
        Assert.checkNotNullParam("flag", flag);
        return values[1 << flag.ordinal()];
    }

    public static final ConsumeFlags NONE = values[0];
}
