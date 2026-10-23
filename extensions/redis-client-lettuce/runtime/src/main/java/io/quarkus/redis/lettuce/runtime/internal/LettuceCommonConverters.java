package io.quarkus.redis.lettuce.runtime.internal;

import java.util.List;

import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.SortArgs;

public final class LettuceCommonConverters {

    private LettuceCommonConverters() {
        // Utility class
    }

    public static io.lettuce.core.ScanArgs toLettuceScanArgs(ScanArgs quarkus) {
        List<String> tokens = quarkus.toArgs();
        return new io.lettuce.core.ScanArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

    public static io.lettuce.core.SortArgs toLettuceSortArgs(SortArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.SortArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

}
