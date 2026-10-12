package io.quarkus.redis.lettuce.runtime.internal.json;

import java.util.List;

import io.lettuce.core.json.arguments.JsonRangeArgs;
import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.json.JsonSetArgs;
import io.quarkus.redis.lettuce.runtime.internal.ArgReplay;

public final class LettuceJsonCommandsConverter {

    private LettuceJsonCommandsConverter() {
        // Utility class
    }

    public static io.lettuce.core.json.arguments.JsonSetArgs toLettuceJsonSetArgs(JsonSetArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.json.arguments.JsonSetArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

    public static JsonRangeArgs toLettuceJsonRangeArgs(long start, long stop) {
        return new JsonRangeArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                args.add(start);
                args.add(stop);
            }
        };
    }

}
