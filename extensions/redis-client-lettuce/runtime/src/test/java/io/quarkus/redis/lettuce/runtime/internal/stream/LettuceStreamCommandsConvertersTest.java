package io.quarkus.redis.lettuce.runtime.internal.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.lettuce.core.Range;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.stream.StreamRange;
import io.quarkus.redis.datasource.stream.XAddArgs;
import io.quarkus.redis.datasource.stream.XClaimArgs;
import io.quarkus.redis.datasource.stream.XGroupCreateArgs;
import io.quarkus.redis.datasource.stream.XGroupSetIdArgs;
import io.quarkus.redis.datasource.stream.XPendingArgs;
import io.quarkus.redis.datasource.stream.XReadArgs;
import io.quarkus.redis.datasource.stream.XReadGroupArgs;
import io.quarkus.redis.datasource.stream.XTrimArgs;
import io.quarkus.redis.lettuce.runtime.internal.BlockingReads;

class LettuceStreamCommandsConvertersTest {

    @Test
    void xClaimArgsWithoutOptionsRenderNothing() {
        assertThat(render(new XClaimArgs(), Duration.ofSeconds(1))).isEmpty();
    }

    @Test
    void xClaimArgsRenderOptionsInVertxOrder() {
        XClaimArgs args = new XClaimArgs().idle(Duration.ofMillis(1)).retryCount(5).force().justId().lastId("0-0");
        assertThat(render(args, Duration.ofSeconds(1)))
                .containsExactly("IDLE", "1", "RETRYCOUNT", "5", "FORCE", "JUSTID", "LASTID", "0-0");
    }

    @Test
    void xClaimArgsRenderTime() {
        assertThat(render(new XClaimArgs().time(1_700_000_000_000L), Duration.ZERO))
                .containsExactly("TIME", "1700000000000");
    }

    @Test
    void xClaimArgsRejectIdleCombinedWithTime() {
        assertThatThrownBy(() -> render(new XClaimArgs().idle(Duration.ofMillis(1)).time(1), Duration.ZERO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void xAddArgsRenderTokens() {
        assertThat(renderToTokens(LettuceStreamCommandsConverters.toXAddArgs(new XAddArgs().maxlen(5L))::build))
                .containsExactly("MAXLEN", "=", "5", "*"); // the Quarkus args always render the id
    }

    @Test
    void xGroupCreateArgsRenderTokens() {
        assertThat(renderToTokens(LettuceStreamCommandsConverters
                .toXGroupCreateArgs(new XGroupCreateArgs().mkstream().entriesRead("3"))::build))
                .containsExactly("MKSTREAM", "ENTRIESREAD", "3");
    }

    @Test
    void xGroupSetIdCommandArgsRenderSubcommandKeyGroupIdAndOptions() {
        CommandArgs<byte[], byte[]> args = LettuceStreamCommandsConverters.toXGroupSetIdCommandArgs(bytes("k"),
                bytes("g"), "0-0", new XGroupSetIdArgs().entriesRead(7));
        assertThat(args.toCommandString().split(" ")).containsExactly("SETID", "key<k>", "key<g>", "0-0", "ENTRIESREAD", "7");
    }

    @Test
    void xReadArgsRenderTokens() {
        XReadArgs plain = new XReadArgs().count(2);
        assertThat(renderToTokens(LettuceStreamCommandsConverters.toXReadArgs(plain)::build)).containsExactly("COUNT", "2");

        XReadArgs blocking = new XReadArgs().count(2).block(Duration.ofSeconds(1));
        assertThat(renderToTokens(LettuceStreamCommandsConverters.toXReadArgs(blocking)::build))
                .containsExactly("COUNT", "2", "BLOCK", "1000");
    }

    @Test
    void xReadArgsMarkTheCommandArgsOnlyWithBlock() {
        CommandArgs<String, String> plain = new CommandArgs<>(StringCodec.UTF8);
        LettuceStreamCommandsConverters.toXReadArgs(new XReadArgs().count(2)).build(plain);
        assertThat(BlockingReads.isMarked(plain)).isFalse();

        CommandArgs<String, String> blocking = new CommandArgs<>(StringCodec.UTF8);
        LettuceStreamCommandsConverters.toXReadArgs(new XReadArgs().block(Duration.ofSeconds(1))).build(blocking);
        assertThat(BlockingReads.isMarked(blocking)).isTrue();

        CommandArgs<String, String> group = new CommandArgs<>(StringCodec.UTF8);
        LettuceStreamCommandsConverters.toXReadArgs(new XReadGroupArgs().block(Duration.ZERO)).build(group);
        assertThat(BlockingReads.isMarked(group)).isTrue();
    }

    @Test
    void xReadGroupArgsRenderTokens() {
        XReadGroupArgs plain = new XReadGroupArgs().count(2).noack().claim(Duration.ofMillis(5));
        assertThat(renderToTokens(LettuceStreamCommandsConverters.toXReadArgs(plain)::build))
                .containsExactly("COUNT", "2", "CLAIM", "5", "NOACK");
        assertThat(
                renderToTokens(LettuceStreamCommandsConverters.toXReadArgs(new XReadGroupArgs().block(Duration.ZERO))::build))
                .containsExactly("BLOCK", "0");
    }

    @Test
    void xTrimArgsRenderTokens() {
        assertThat(renderToTokens(LettuceStreamCommandsConverters
                .toXTrimArgs(new XTrimArgs().minid("5-0").nearlyExactTrimming().limit(10))::build))
                .containsExactly("MINID", "~", "5-0", "LIMIT", "10");
    }

    @Test
    void rangesKeepBoundsVerbatimExceptTheUnboundedMarkers() {
        Range<String> full = LettuceStreamCommandsConverters.toRange(StreamRange.of("-", "+"));
        assertThat(full.getLower().isUnbounded()).isTrue();
        assertThat(full.getUpper().isUnbounded()).isTrue();

        Range<String> ids = LettuceStreamCommandsConverters.toRange(StreamRange.of("(1-0", "2-0"));
        assertThat(ids.getLower().getValue()).isEqualTo("(1-0");
        assertThat(ids.getLower().isIncluding()).isTrue(); // sent verbatim, Redis reads the exclusive prefix
        assertThat(ids.getUpper().getValue()).isEqualTo("2-0");

        // XREVRANGE takes end then start; Lettuce writes the upper boundary first
        Range<String> reverse = LettuceStreamCommandsConverters.toReverseRange(StreamRange.of("+", "1-0"));
        assertThat(reverse.getUpper().isUnbounded()).isTrue();
        assertThat(reverse.getLower().getValue()).isEqualTo("1-0");
    }

    @Test
    void xPendingArgsRenderGroupIdleRangeCountAndConsumerInOrder() {
        assertThat(renderBytesToTokens(LettuceStreamCommandsConverters.toXPendingArgs(bytes("g"), StreamRange.of("-", "+"),
                10, new XPendingArgs().idle(Duration.ofSeconds(1)).consumer("c1"))::build))
                .containsExactly("key<g>", "IDLE", "1000", "-", "+", "10", "c1");
        assertThat(renderBytesToTokens(LettuceStreamCommandsConverters.toXPendingArgs(bytes("g"), StreamRange.of("-", "+"),
                10, null)::build)).containsExactly("key<g>", "-", "+", "10");
    }

    // ------------------------------------------------------------------ helpers

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String[] render(XClaimArgs args, Duration minIdleTime) {
        return renderToTokens(LettuceStreamCommandsConverters.toXClaimArgs(args, minIdleTime)::build);
    }

    private static String[] renderBytesToTokens(Consumer<CommandArgs<byte[], byte[]>> builder) {
        CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE);
        builder.accept(args);
        return split(args.toCommandString());
    }

    private static String[] renderToTokens(Consumer<CommandArgs<String, String>> builder) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        builder.accept(args);
        return split(args.toCommandString());
    }

    private static String[] split(String rendered) {
        // CommandArgs.toCommandString() renders tokens space-separated, unquoted
        if (rendered == null || rendered.isEmpty()) {
            return new String[0];
        }
        return rendered.split(" ");
    }
}
