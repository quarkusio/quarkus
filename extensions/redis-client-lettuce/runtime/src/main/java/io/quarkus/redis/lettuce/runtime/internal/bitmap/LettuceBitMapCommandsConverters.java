package io.quarkus.redis.lettuce.runtime.internal.bitmap;

import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.bitmap.BitFieldArgs;
import io.quarkus.redis.lettuce.runtime.internal.ArgReplay;

final class LettuceBitMapCommandsConverters {

    private LettuceBitMapCommandsConverters() {
        // Utility class
    }

    static io.lettuce.core.BitFieldArgs toLettuceBitFieldArgs(BitFieldArgs quarkus) {
        return new io.lettuce.core.BitFieldArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(quarkus.toArgs(), args);
            }
        };
    }

}
