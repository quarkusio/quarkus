package io.quarkus.redis.lettuce.runtime.internal.autosuggest;

import java.util.List;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.search.arguments.SugGetArgs;
import io.quarkus.redis.datasource.autosuggest.GetArgs;
import io.quarkus.redis.lettuce.runtime.internal.ArgReplay;

final class LettuceAutoSuggestCommandsConverters {

    private LettuceAutoSuggestCommandsConverters() {
        // Utility class
    }

    static SugGetArgs toLettuceSugGetArgs(GetArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        SugGetArgs lettuceArgs = new SugGetArgs() {
            @Override
            public void build(CommandArgs<?, ?> args) {
                ArgReplay.replay(tokens, args);
            }
        };
        if (quarkus.hasScores()) {
            lettuceArgs.withScores();
        }
        return lettuceArgs;
    }

}
