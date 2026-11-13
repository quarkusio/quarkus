package io.quarkus.redis.lettuce.runtime.internal.key;

import java.util.List;

import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.keys.CopyArgs;
import io.quarkus.redis.datasource.keys.ExpireArgs;
import io.quarkus.redis.datasource.keys.KeyScanArgs;
import io.quarkus.redis.lettuce.runtime.internal.ArgReplay;

final class LettuceKeyCommandsConverters {

    private LettuceKeyCommandsConverters() {
        // Utility class
    }

    static io.lettuce.core.ExpireArgs toLettuceExpireArgs(ExpireArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.ExpireArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

    static io.lettuce.core.CopyArgs toLettuceCopyArgs(CopyArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.CopyArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

    static io.lettuce.core.KeyScanArgs toLettuceKeyScanArgs(KeyScanArgs quarkus) {
        List<String> tokens = quarkus.toArgs();
        return new io.lettuce.core.KeyScanArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

}
