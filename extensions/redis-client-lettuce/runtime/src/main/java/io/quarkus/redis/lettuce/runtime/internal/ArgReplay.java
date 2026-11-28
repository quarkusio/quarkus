package io.quarkus.redis.lettuce.runtime.internal;

import java.util.List;
import java.util.Set;

import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Replays Quarkus command argument tokens verbatim into a Lettuce {@link CommandArgs}.
 * <p>
 * The Quarkus argument classes ({@code SetArgs}, {@code ScanArgs}, {@code SortArgs}, ...) expose their state only
 * through {@code toArgs()} (see {@link RedisCommandExtraArguments#toArgs()}), which returns the wire-format token
 * list the Vert.x backend sends unchanged. Appending those same tokens to the Lettuce command makes both backends
 * byte-identical on the wire and keeps the Lettuce backend independent of the keywords each Quarkus class emits.
 */
public final class ArgReplay {

    private ArgReplay() {
        // Utility class
    }

    /**
     * Appends every token of {@code tokens} to {@code args}, unchanged and in order.
     *
     * @param tokens the wire-format tokens, from an already-called {@code toArgs()}
     * @param args the Lettuce command arguments being built
     */
    public static <K, V> void replay(List<?> tokens, CommandArgs<K, V> args) {
        for (Object token : tokens) {
            if (token != null) {
                args.add(token);
            }
        }
    }

    /**
     * Appends every token of {@code tokens} to {@code args} except the {@code String} tokens contained in
     * {@code skip}, compared case-sensitively.
     * <p>
     * This exists for keywords that Lettuce appends itself, such as {@code GET} in {@code setGet}, which would
     * otherwise be sent twice. Only use it with argument classes whose tokens are keywords and numbers, never user
     * values, since any matching token is dropped regardless of position.
     *
     * @param tokens the wire-format tokens, from an already-called {@code toArgs()}
     * @param args the Lettuce command arguments being built
     * @param skip the keyword tokens to leave out
     */
    public static <K, V> void replayExcept(List<?> tokens, CommandArgs<K, V> args, Set<String> skip) {
        for (Object token : tokens) {
            if (token != null && !(token instanceof String s && skip.contains(s))) {
                args.add(token);
            }
        }
    }

}
