package io.quarkus.redis.lettuce.runtime.internal.sortedset;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.quarkus.redis.datasource.sortedset.ScoreRange;
import io.quarkus.redis.datasource.sortedset.ZRangeArgs;

class LettuceSortedSetCommandsConvertersTest {

    // -------------------------------------------------------------- ScoreRange
    // Lettuce Range is not a renderable command-args object, so these assert through
    // its public boundary accessors instead of wire tokens.

    @Test
    void inclusiveScoreRange() {
        Range<Number> range = LettuceSortedSetCommandsConverters.toLettuceScoreRange(ScoreRange.from(1.0, 3.0));
        assertThat(range.getLower().getValue()).isEqualTo(1.0);
        assertThat(range.getLower().isIncluding()).isTrue();
        assertThat(range.getUpper().getValue()).isEqualTo(3.0);
        assertThat(range.getUpper().isIncluding()).isTrue();
    }

    @Test
    void exclusiveScoreRange() {
        Range<Number> range = LettuceSortedSetCommandsConverters
                .toLettuceScoreRange(new ScoreRange<>(1.0, false, 3.0, false));
        assertThat(range.getLower().getValue()).isEqualTo(1.0);
        assertThat(range.getLower().isIncluding()).isFalse();
        assertThat(range.getUpper().getValue()).isEqualTo(3.0);
        assertThat(range.getUpper().isIncluding()).isFalse();
    }

    /**
     * An unbounded score range converts to inclusive infinite boundaries — Lettuce writes those as
     * the {@code -inf} / {@code +inf} tokens, matching what the Vert.x backend sends.
     */
    @Test
    void unboundedScoreRangeBecomesInfiniteBoundaries() {
        for (ScoreRange<Double> unbounded : List.of(ScoreRange.unbounded(),
                new ScoreRange<Double>(null, null),
                new ScoreRange<>(NEGATIVE_INFINITY, POSITIVE_INFINITY))) {
            Range<Number> range = LettuceSortedSetCommandsConverters.toLettuceScoreRange(unbounded);
            assertThat(range.getLower().getValue()).isEqualTo(NEGATIVE_INFINITY);
            assertThat(range.getLower().isIncluding()).isTrue();
            assertThat(range.getUpper().getValue()).isEqualTo(POSITIVE_INFINITY);
            assertThat(range.getUpper().isIncluding()).isTrue();
        }
    }

    /** An infinite boundary is always inclusive, even in an otherwise exclusive range. */
    @Test
    void halfInfiniteScoreRange() {
        Range<Number> range = LettuceSortedSetCommandsConverters
                .toLettuceScoreRange(new ScoreRange<>(NEGATIVE_INFINITY, false, 3.0, false));
        assertThat(range.getLower().getValue()).isEqualTo(NEGATIVE_INFINITY);
        assertThat(range.getLower().isIncluding()).isTrue();
        assertThat(range.getUpper().getValue()).isEqualTo(3.0);
        assertThat(range.getUpper().isIncluding()).isFalse();
    }

    // ------------------------------------------------------ lexicographical Range

    @Test
    void inclusiveLexRange() {
        Range<byte[]> range = LettuceSortedSetCommandsConverters
                .toLettuceLexRange(new io.quarkus.redis.datasource.sortedset.Range<>("b", "d"));
        assertThat(range.getLower().getValue()).isEqualTo("b".getBytes(UTF_8));
        assertThat(range.getLower().isIncluding()).isTrue();
        assertThat(range.getUpper().getValue()).isEqualTo("d".getBytes(UTF_8));
        assertThat(range.getUpper().isIncluding()).isTrue();
    }

    @Test
    void exclusiveLexRange() {
        Range<byte[]> range = LettuceSortedSetCommandsConverters
                .toLettuceLexRange(new io.quarkus.redis.datasource.sortedset.Range<>("b", false, "d", false));
        assertThat(range.getLower().isIncluding()).isFalse();
        assertThat(range.getUpper().isIncluding()).isFalse();
    }

    @Test
    void unboundedLexRange() {
        assertThat(LettuceSortedSetCommandsConverters
                .toLettuceLexRange(io.quarkus.redis.datasource.sortedset.Range.unbounded()).isUnbounded())
                .isTrue();
        // The `-` / `+` sentinels and a `null` bound are the two other spellings of "unbounded".
        assertThat(LettuceSortedSetCommandsConverters
                .toLettuceLexRange(new io.quarkus.redis.datasource.sortedset.Range<>("-", "+")).isUnbounded())
                .isTrue();
        Range<byte[]> halfOpen = LettuceSortedSetCommandsConverters
                .toLettuceLexRange(new io.quarkus.redis.datasource.sortedset.Range<>("value99", true, null, true));
        assertThat(halfOpen.getLower().getValue()).isEqualTo("value99".getBytes(UTF_8));
        assertThat(halfOpen.getUpper().isUnbounded()).isTrue();
    }

    /** {@code -} is only a sentinel in the lower bound; as an upper bound it stays a plain member. */
    @Test
    void minusAsAnUpperBoundIsAPlainMember() {
        Range<byte[]> range = LettuceSortedSetCommandsConverters
                .toLettuceLexRange(new io.quarkus.redis.datasource.sortedset.Range<>("c", "-"));
        assertThat(range.getLower().getValue()).isEqualTo("c".getBytes(UTF_8));
        assertThat(range.getUpper().getValue()).isEqualTo("-".getBytes(UTF_8));
        assertThat(range.getUpper().isIncluding()).isTrue();
    }
    // ------------------------------------------------------------- ZRangeArgs

    @Test
    void noLimitIsUnlimited() {
        assertThat(LettuceSortedSetCommandsConverters.toLettuceLimit(new ZRangeArgs()).isLimited()).isFalse();
        assertThat(LettuceSortedSetCommandsConverters.toLettuceLimit(new ZRangeArgs().rev()).isLimited()).isFalse();
    }

    @Test
    void limitIsReadBackFromTokens() {
        Limit limit = LettuceSortedSetCommandsConverters.toLettuceLimit(new ZRangeArgs().rev().limit(2, 5));
        assertThat(limit.isLimited()).isTrue();
        assertThat(limit.getOffset()).isEqualTo(2);
        assertThat(limit.getCount()).isEqualTo(5);
    }

    /** A negative count means "everything from the offset" in Redis and must reach the wire unchanged. */
    @Test
    void negativeCountIsPassedThrough() {
        Limit limit = LettuceSortedSetCommandsConverters.toLettuceLimit(new ZRangeArgs().limit(3, -1));
        assertThat(limit.isLimited()).isTrue();
        assertThat(limit.getOffset()).isEqualTo(3);
        assertThat(limit.getCount()).isEqualTo(-1);
    }

    /** Redis rejects LIMIT on an index range, and Lettuce's ByIndex cannot carry it, so it is refused up front. */
    @Test
    void limitIsRejectedOnIndexRanges() {
        assertThatThrownBy(() -> LettuceSortedSetCommandsConverters.toLettuceByIndex(0, -1, new ZRangeArgs().limit(0, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LIMIT");
        assertThatThrownBy(
                () -> LettuceSortedSetCommandsConverters.toLettuceIndexRange(0, -1, new ZRangeArgs().limit(0, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LIMIT");
    }

    @Test
    void indexRangeKeepsBoundsInclusive() {
        Range<Long> range = LettuceSortedSetCommandsConverters.toLettuceIndexRange(2, 4, new ZRangeArgs().rev());
        assertThat(range.getLower().getValue()).isEqualTo(2L);
        assertThat(range.getLower().isIncluding()).isTrue();
        assertThat(range.getUpper().getValue()).isEqualTo(4L);
        assertThat(range.getUpper().isIncluding()).isTrue();
    }

    /**
     * Lettuce writes a reversed range as {@code max min}; the Vert.x backend sends store bounds in the caller's
     * order. Swapping on conversion means Lettuce's swap restores the order the caller gave.
     */
    @Test
    void reversedStoreRangesSwapBounds() {
        Range<Number> scores = LettuceSortedSetCommandsConverters
                .toLettuceStoreScoreRange(new ScoreRange<>(2.0, true, 1.0, false), new ZRangeArgs().rev());
        assertThat(scores.getLower().getValue()).isEqualTo(1.0);
        assertThat(scores.getLower().isIncluding()).isFalse();
        assertThat(scores.getUpper().getValue()).isEqualTo(2.0);
        assertThat(scores.getUpper().isIncluding()).isTrue();

        Range<byte[]> lex = LettuceSortedSetCommandsConverters
                .toLettuceStoreLexRange(new io.quarkus.redis.datasource.sortedset.Range<>("c", "-"), new ZRangeArgs().rev());
        assertThat(lex.getLower().getValue()).isEqualTo("-".getBytes(UTF_8));
        assertThat(lex.getUpper().getValue()).isEqualTo("c".getBytes(UTF_8));
    }

    @Test
    void forwardStoreRangesKeepBounds() {
        Range<Number> scores = LettuceSortedSetCommandsConverters
                .toLettuceStoreScoreRange(new ScoreRange<>(0.0, 2.0), new ZRangeArgs());
        assertThat(scores.getLower().getValue()).isEqualTo(0.0);
        assertThat(scores.getUpper().getValue()).isEqualTo(2.0);

        Range<byte[]> lex = LettuceSortedSetCommandsConverters
                .toLettuceStoreLexRange(new io.quarkus.redis.datasource.sortedset.Range<>("b", "d"), new ZRangeArgs());
        assertThat(lex.getLower().getValue()).isEqualTo("b".getBytes(UTF_8));
        assertThat(lex.getUpper().getValue()).isEqualTo("d".getBytes(UTF_8));
    }
}
