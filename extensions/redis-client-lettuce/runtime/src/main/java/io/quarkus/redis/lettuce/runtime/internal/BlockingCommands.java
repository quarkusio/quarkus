package io.quarkus.redis.lettuce.runtime.internal;

import java.util.Set;

import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * The commands that hold their connection until an element arrives or their own timeout elapses: the blocking list
 * and sorted set pops, and {@code XREAD} and {@code XREADGROUP} when given the {@code BLOCK} option.
 * <p>
 * Such a command must not run on the shared connection of a data source, where every other command would wait behind
 * it: the typed commands borrow a pooled connection through {@link AbstractLettuceCommands#blocking}, and a raw
 * {@code execute(...)} does the same for the commands this class recognizes. It is not subject to the command timeout
 * either (see {@code LettuceConnectionFactory.NonBlockingCommandTimeoutSource}): its own timeout bounds it.
 * <p>
 * {@code WAIT} and {@code WAITAOF} block too, but are not listed: they wait for the writes of their own connection to
 * be replicated or persisted, so on another connection they would wait for nothing.
 */
public final class BlockingCommands {

    private static final Set<String> ALWAYS_BLOCKING = Set.of("BLPOP", "BRPOP", "BRPOPLPUSH", "BLMOVE", "BLMPOP",
            "BZPOPMIN", "BZPOPMAX", "BZMPOP");

    private static final Set<String> BLOCKING_WITH_BLOCK = Set.of("XREAD", "XREADGROUP");

    private BlockingCommands() {
        // Utility class
    }

    /**
     * Whether the command blocks whatever its arguments.
     */
    public static boolean alwaysBlocks(ProtocolKeyword type) {
        return ALWAYS_BLOCKING.contains(type.toString());
    }

    /**
     * Whether the command blocks when it is given the {@code BLOCK} option.
     */
    public static boolean blocksWithBlockOption(ProtocolKeyword type) {
        return BLOCKING_WITH_BLOCK.contains(type.toString());
    }

    /**
     * Whether a raw command blocks: one that always does, or a stream read whose arguments carry the {@code BLOCK}
     * option.
     *
     * @param type the command, its name in upper case
     * @param args the arguments as given to {@code execute(...)}; may be {@code null} and may contain {@code null}s
     */
    public static boolean blocks(ProtocolKeyword type, String... args) {
        return alwaysBlocks(type) || blocksWithBlockOption(type) && hasBlockOption(args);
    }

    /**
     * Looks for {@code BLOCK} among the options of {@code XREAD} or {@code XREADGROUP}, which precede
     * {@code STREAMS}; the group and the consumer following {@code GROUP} are names, not options.
     */
    private static boolean hasBlockOption(String[] args) {
        if (args == null) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null) {
                continue;
            }
            if ("STREAMS".equalsIgnoreCase(arg)) {
                return false;
            }
            if ("GROUP".equalsIgnoreCase(arg)) {
                i += 2;
            } else if ("BLOCK".equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

}
