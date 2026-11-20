package io.quarkus.redis.lettuce.runtime.internal.bloom;

import java.util.List;

import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.bloom.BfInsertArgs;
import io.quarkus.redis.datasource.bloom.BfReserveArgs;
import io.quarkus.redis.lettuce.runtime.internal.ArgReplay;

final class LettuceBloomCommandsConverters {

    private LettuceBloomCommandsConverters() {
        // Utility class
    }

    static io.lettuce.core.probabilistic.arguments.BfReserveArgs toLettuceBfReserveArgs(BfReserveArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.probabilistic.arguments.BfReserveArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

    static io.lettuce.core.probabilistic.arguments.BfInsertArgs toLettuceBfInsertArgs(BfInsertArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.probabilistic.arguments.BfInsertArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

}
