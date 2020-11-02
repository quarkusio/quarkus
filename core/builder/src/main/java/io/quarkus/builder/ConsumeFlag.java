package io.quarkus.builder;

import io.smallrye.common.constraint.Assert;

/**
 */
public enum ConsumeFlag {
    /**
     * Do not exclude the build step even if the given resource is not produced by any other build step.
     */
    OPTIONAL(io.quarkus.qlue.ConsumeFlag.OPTIONAL),
    ;

    private final io.quarkus.qlue.ConsumeFlag realFlag;

    ConsumeFlag(final io.quarkus.qlue.ConsumeFlag realFlag) {
        this.realFlag = realFlag;
    }

    public io.quarkus.qlue.ConsumeFlag getRealFlag() {
        return realFlag;
    }

    public static ConsumeFlag fromRealFlag(io.quarkus.qlue.ConsumeFlag realFlag) {
        Assert.checkNotNullParam("realFlag", realFlag);
        assert realFlag == io.quarkus.qlue.ConsumeFlag.OPTIONAL;
        return OPTIONAL;
    }
}
