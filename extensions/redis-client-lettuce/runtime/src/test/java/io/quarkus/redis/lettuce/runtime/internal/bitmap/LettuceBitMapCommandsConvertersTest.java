package io.quarkus.redis.lettuce.runtime.internal.bitmap;

import static io.quarkus.redis.datasource.bitmap.BitFieldArgs.offset;
import static io.quarkus.redis.datasource.bitmap.BitFieldArgs.signed;
import static io.quarkus.redis.datasource.bitmap.BitFieldArgs.typeWidthBasedOffset;
import static io.quarkus.redis.datasource.bitmap.BitFieldArgs.unsigned;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.bitmap.BitFieldArgs;
import io.quarkus.redis.datasource.bitmap.BitFieldArgs.OverflowType;

class LettuceBitMapCommandsConvertersTest {

    @Test
    void bitFieldArgsRenderEverySubcommandInOrder() {
        BitFieldArgs args = new BitFieldArgs()
                .set(signed(8), 0, 1)
                .get(signed(8), typeWidthBasedOffset(1))
                .incrBy(2, 3)
                .overflow(OverflowType.WRAP);

        assertThat(renderBitFieldArgs(args)).containsExactly(
                "SET", "i8", "0", "1",
                "GET", "i8", "#1",
                "INCRBY", "i8", "2", "3",
                "OVERFLOW", "WRAP");
    }

    @Test
    void bitFieldArgsRenderTypeWidthBasedOffsetWithHashPrefix() {
        assertThat(renderBitFieldArgs(new BitFieldArgs().get(unsigned(4), typeWidthBasedOffset(3))))
                .containsExactly("GET", "u4", "#3");
        assertThat(renderBitFieldArgs(new BitFieldArgs().set(unsigned(4), offset(3), 7)))
                .containsExactly("SET", "u4", "3", "7");
    }

    @Test
    void bitFieldArgsRenderEveryOverflowType() {
        assertThat(renderBitFieldArgs(new BitFieldArgs().overflow(OverflowType.WRAP)))
                .containsExactly("OVERFLOW", "WRAP");
        assertThat(renderBitFieldArgs(new BitFieldArgs().overflow(OverflowType.SAT)))
                .containsExactly("OVERFLOW", "SAT");
        assertThat(renderBitFieldArgs(new BitFieldArgs().overflow(OverflowType.FAIL)))
                .containsExactly("OVERFLOW", "FAIL");
    }

    @Test
    void bitFieldArgsRenderNegativeIncrementVerbatim() {
        assertThat(renderBitFieldArgs(new BitFieldArgs().incrBy(signed(16), 8, -5)))
                .containsExactly("INCRBY", "i16", "8", "-5");
    }

    @Test
    void emptyBitFieldArgsRenderNoTokens() {
        assertThat(renderBitFieldArgs(new BitFieldArgs())).isEmpty();
    }

    private static String[] renderBitFieldArgs(BitFieldArgs quarkus) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        LettuceBitMapCommandsConverters.toLettuceBitFieldArgs(quarkus).build(args);
        // CommandArgs.toCommandString() renders tokens space-separated, unquoted
        String rendered = args.toCommandString();
        if (rendered == null || rendered.isEmpty()) {
            return new String[0];
        }
        return rendered.split(" ");
    }

}
