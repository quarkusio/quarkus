package io.quarkus.redis.lettuce.runtime.internal.value;

import java.util.List;
import java.util.Set;

import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.value.GetExArgs;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.lettuce.runtime.internal.ArgReplay;

final class LettuceValueCommandsConverters {

    private LettuceValueCommandsConverters() {
        // Utility class
    }

    static io.lettuce.core.SetArgs toLettuceSetArgs(SetArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.SetArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replayExcept(tokens, args, Set.of("GET"));
            }
        };
    }

    static io.lettuce.core.GetExArgs toLettuceGetExArgs(GetExArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.GetExArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

}
