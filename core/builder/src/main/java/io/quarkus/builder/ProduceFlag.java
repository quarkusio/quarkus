package io.quarkus.builder;

import java.util.EnumMap;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 */
public enum ProduceFlag {
    /**
     * Only produce this item weakly: if only weak items produced by a build step are consumed, the step will not be included.
     */
    WEAK(io.quarkus.qlue.ProduceFlag.WEAK),
    /**
     * Only produce this {@link SimpleBuildItem} if no other build steps produce it.
     */
    OVERRIDABLE(io.quarkus.qlue.ProduceFlag.OVERRIDABLE),
    ;

    private static final EnumMap<io.quarkus.qlue.ProduceFlag, ProduceFlag> MAP;

    private final io.quarkus.qlue.ProduceFlag realFlag;

    ProduceFlag(final io.quarkus.qlue.ProduceFlag realFlag) {
        this.realFlag = realFlag;
    }

    public io.quarkus.qlue.ProduceFlag getRealFlag() {
        return realFlag;
    }

    public static ProduceFlag fromRealFlag(io.quarkus.qlue.ProduceFlag realFlag) {
        return MAP.get(realFlag);
    }

    static {
        final EnumMap<io.quarkus.qlue.ProduceFlag, ProduceFlag> map = new EnumMap<>(io.quarkus.qlue.ProduceFlag.class);
        map.put(io.quarkus.qlue.ProduceFlag.WEAK, WEAK);
        map.put(io.quarkus.qlue.ProduceFlag.OVERRIDABLE, OVERRIDABLE);
        MAP = map;
    }
}
