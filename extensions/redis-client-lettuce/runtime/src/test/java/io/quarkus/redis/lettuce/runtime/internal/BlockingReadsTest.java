package io.quarkus.redis.lettuce.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

class BlockingReadsTest {

    @Test
    void aMarkSurvivesRepeatedLookups() {
        // a cluster redirect writes the same command twice, and each write asks for the timeout again
        CommandArgs<String, String> args = args();
        BlockingReads.mark(args);
        assertThat(BlockingReads.isMarked(args)).isTrue();
        assertThat(BlockingReads.isMarked(args)).isTrue();
    }

    @Test
    void marksArePerInstanceNotPerContent() {
        // two reads with identical arguments are two commands: marking one must not mark the other
        CommandArgs<String, String> first = args();
        CommandArgs<String, String> second = args();
        BlockingReads.mark(first);
        assertThat(BlockingReads.isMarked(first)).isTrue();
        assertThat(BlockingReads.isMarked(second)).isFalse();
        BlockingReads.mark(second);
        assertThat(BlockingReads.isMarked(second)).isTrue();
    }

    @Test
    void unmarkedAndNullArgumentsAreNotBlocking() {
        assertThat(BlockingReads.isMarked(args())).isFalse();
        assertThat(BlockingReads.isMarked(null)).isFalse();
    }

    private static CommandArgs<String, String> args() {
        return new CommandArgs<>(StringCodec.UTF8).add("BLOCK").add("1000").add("STREAMS").addKey("k").add("$");
    }

}
