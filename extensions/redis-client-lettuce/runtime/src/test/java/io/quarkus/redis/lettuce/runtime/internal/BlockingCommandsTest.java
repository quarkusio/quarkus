package io.quarkus.redis.lettuce.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;

class BlockingCommandsTest {

    @Test
    void theBlockingPopsAlwaysBlock() {
        assertThat(BlockingCommands.blocks(CommandType.BLPOP, "k", "0")).isTrue();
        assertThat(BlockingCommands.blocks(CommandType.BRPOP, "k", "5")).isTrue();
        assertThat(BlockingCommands.blocks(CommandType.BRPOPLPUSH, "a", "b", "0")).isTrue();
        assertThat(BlockingCommands.blocks(CommandType.BLMOVE, "a", "b", "LEFT", "RIGHT", "0")).isTrue();
        assertThat(BlockingCommands.blocks(CommandType.BLMPOP, "0", "1", "k", "LEFT")).isTrue();
        assertThat(BlockingCommands.blocks(CommandType.BZPOPMIN, "k", "0")).isTrue();
        assertThat(BlockingCommands.blocks(CommandType.BZPOPMAX, "k", "0")).isTrue();
        assertThat(BlockingCommands.blocks(CommandType.BZMPOP, "0", "1", "k", "MIN")).isTrue();
        // a command execute(...) sends under its name, without a Lettuce command type
        assertThat(BlockingCommands.blocks(keyword("BLPOP"), "k", "0")).isTrue();
    }

    @Test
    void ordinaryCommandsDoNotBlock() {
        assertThat(BlockingCommands.blocks(CommandType.GET, "k")).isFalse();
        assertThat(BlockingCommands.blocks(CommandType.LPOP, "k")).isFalse();
        assertThat(BlockingCommands.blocks(CommandType.PING)).isFalse();
        assertThat(BlockingCommands.blocks(CommandType.PING, (String[]) null)).isFalse();
        assertThat(BlockingCommands.blocks(keyword("FT.SEARCH"), "idx", "BLOCK")).isFalse();
    }

    @Test
    void waitingForTheWritesOfTheConnectionStaysOnThatConnection() {
        // WAIT and WAITAOF are about the writes of their own connection: not to be moved to a pooled one
        assertThat(BlockingCommands.blocks(CommandType.WAIT, "1", "0")).isFalse();
        assertThat(BlockingCommands.blocks(keyword("WAITAOF"), "1", "0", "0")).isFalse();
    }

    @Test
    void aStreamReadBlocksOnlyWithTheBlockOption() {
        assertThat(BlockingCommands.blocks(CommandType.XREAD, "STREAMS", "s", "0-0")).isFalse();
        assertThat(BlockingCommands.blocks(CommandType.XREAD, "COUNT", "10", "STREAMS", "s", "0-0")).isFalse();
        assertThat(BlockingCommands.blocks(CommandType.XREAD, "BLOCK", "0", "STREAMS", "s", "$")).isTrue();
        assertThat(BlockingCommands.blocks(CommandType.XREAD, "COUNT", "10", "block", "100", "streams", "s", "$")).isTrue();
        assertThat(BlockingCommands.blocks(CommandType.XREADGROUP, "GROUP", "g", "c", "STREAMS", "s", ">")).isFalse();
        assertThat(BlockingCommands.blocks(CommandType.XREADGROUP, "GROUP", "g", "c", "COUNT", "1", "BLOCK", "0", "NOACK",
                "STREAMS", "s", ">")).isTrue();
        assertThat(BlockingCommands.blocks(CommandType.XREAD, null, "BLOCK", "0", "STREAMS", "s", "$")).isTrue();
        assertThat(BlockingCommands.blocks(CommandType.XREAD)).isFalse();
        assertThat(BlockingCommands.blocks(CommandType.XREAD, (String[]) null)).isFalse();
    }

    @Test
    void aStreamGroupConsumerOrKeyNamedBlockIsNotTheOption() {
        assertThat(BlockingCommands.blocks(CommandType.XREADGROUP, "GROUP", "block", "c", "STREAMS", "s", ">")).isFalse();
        assertThat(BlockingCommands.blocks(CommandType.XREADGROUP, "GROUP", "g", "BLOCK", "STREAMS", "s", ">")).isFalse();
        assertThat(BlockingCommands.blocks(CommandType.XREAD, "STREAMS", "block", "0-0")).isFalse();
        assertThat(BlockingCommands.blocks(CommandType.XREADGROUP, "GROUP", "block", "block", "BLOCK", "5", "STREAMS", "s",
                ">")).isTrue();
    }

    private static ProtocolKeyword keyword(String name) {
        return new ProtocolKeyword() {
            @Override
            public byte[] getBytes() {
                return name.getBytes(StandardCharsets.US_ASCII);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

}
