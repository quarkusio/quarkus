package io.quarkus.redis.lettuce.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

class ArgReplayTest {

    @Test
    void replayPreservesTokenOrderAndTypes() {
        CommandArgs<String, String> args = newArgs();
        ArgReplay.replay(List.of("LIMIT", 0L, 3L, "ALPHA"), args);
        assertThat(tokens(args)).containsExactly("LIMIT", "0", "3", "ALPHA");
    }

    @Test
    void replayOfEmptyArgumentsAddsNothing() {
        CommandArgs<String, String> args = newArgs();
        ArgReplay.replay(List.of(), args);
        assertThat(tokens(args)).isEmpty();
    }

    @Test
    void replayAppendsAfterExistingTokens() {
        CommandArgs<String, String> args = newArgs().addKey("k");
        ArgReplay.replay(List.of("EX", 10L), args);
        assertThat(tokens(args)).containsExactly("key<k>", "EX", "10");
    }

    @Test
    void replaySkipsNullTokensLikeVertxRedisCommandPut() {
        CommandArgs<String, String> args = newArgs();
        ArgReplay.replay(Arrays.asList("COUNT", 3L, null, "ANY"), args);
        assertThat(tokens(args)).containsExactly("COUNT", "3", "ANY");
    }

    @Test
    void replayExceptSkipsNullTokens() {
        CommandArgs<String, String> args = newArgs();
        ArgReplay.replayExcept(Arrays.asList(null, "GET", "EX", 10L), args, Set.of("GET"));
        assertThat(tokens(args)).containsExactly("EX", "10");
    }

    @Test
    void replayExceptSkipsEveryOccurrenceOfTheListedKeywords() {
        CommandArgs<String, String> args = newArgs();
        ArgReplay.replayExcept(List.of("GET", "EX", 10L, "GET", "NX"), args, Set.of("GET"));
        assertThat(tokens(args)).containsExactly("EX", "10", "NX");
    }

    @Test
    void replayExceptOnlySkipsExactStringMatches() {
        CommandArgs<String, String> args = newArgs();
        ArgReplay.replayExcept(List.of("get", "COUNT", 10L), args, Set.of("GET", "10"));
        assertThat(tokens(args)).containsExactly("get", "COUNT", "10");
    }

    // ------------------------------------------------------------------ helpers

    private static CommandArgs<String, String> newArgs() {
        return new CommandArgs<>(StringCodec.UTF8);
    }

    private static String[] tokens(CommandArgs<String, String> args) {
        // CommandArgs.toCommandString() renders tokens space-separated, unquoted
        String rendered = args.toCommandString();
        if (rendered == null || rendered.isEmpty()) {
            return new String[0];
        }
        return rendered.split(" ");
    }
}
