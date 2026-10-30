package io.quarkus.redis.lettuce.runtime.internal.cuckoo;

import java.util.List;

import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.cuckoo.CfInsertArgs;
import io.quarkus.redis.datasource.cuckoo.CfReserveArgs;
import io.quarkus.redis.lettuce.runtime.internal.ArgReplay;

final class LettuceCuckooCommandsConverters {

    private LettuceCuckooCommandsConverters() {
        // Utility class
    }

    static io.lettuce.core.probabilistic.arguments.CfInsertArgs toLettuceCfInsertArgs(CfInsertArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.probabilistic.arguments.CfInsertArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

    static io.lettuce.core.probabilistic.arguments.CfReserveArgs toLettuceCfReserveArgs(CfReserveArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.probabilistic.arguments.CfReserveArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

}
