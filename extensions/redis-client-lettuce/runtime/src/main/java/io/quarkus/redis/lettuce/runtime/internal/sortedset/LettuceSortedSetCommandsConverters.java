package io.quarkus.redis.lettuce.runtime.internal.sortedset;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.lettuce.core.Limit;
import io.lettuce.core.ZRange;
import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.sortedset.Range;
import io.quarkus.redis.datasource.sortedset.ScoreRange;
import io.quarkus.redis.datasource.sortedset.ZAddArgs;
import io.quarkus.redis.datasource.sortedset.ZAggregateArgs;
import io.quarkus.redis.datasource.sortedset.ZRangeArgs;
import io.quarkus.redis.lettuce.runtime.internal.ArgReplay;

final class LettuceSortedSetCommandsConverters {

    private LettuceSortedSetCommandsConverters() {
        // Utility class
    }

    static io.lettuce.core.ZAddArgs toLettuceZAddArgs(ZAddArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.ZAddArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

    static io.lettuce.core.Range<Number> toLettuceScoreRange(ScoreRange<Double> range) {
        return toLettuceScoreRange(range, false);
    }

    /**
     * Converts a score range, optionally swapping its bounds.
     * <p>
     * Lettuce writes {@code REV} ranges as {@code max min} on the wire, swapping the bounds itself. The Vert.x
     * backend swaps unconditionally for {@code ZRANGE ... BYLEX} but for {@code ZRANGE ... BYSCORE} only on an
     * unbounded range; everywhere else it sends the bounds exactly as the caller ordered them, so callers pass
     * {@code (max, min)} when reversing. Swapping here lets Lettuce's own swap restore the caller's order, keeping
     * both backends byte-identical.
     */
    private static io.lettuce.core.Range<Number> toLettuceScoreRange(ScoreRange<Double> range, boolean swapBounds) {
        String lowerBound = swapBounds ? range.getUpperBound() : range.getLowerBound();
        String upperBound = swapBounds ? range.getLowerBound() : range.getUpperBound();
        return io.lettuce.core.Range.from(toScoreBoundary(lowerBound), toScoreBoundary(upperBound));
    }

    private static io.lettuce.core.Range.Boundary<Number> toScoreBoundary(String bound) {
        if ("-inf".equals(bound)) {
            return io.lettuce.core.Range.Boundary.including(Double.NEGATIVE_INFINITY);
        }
        if ("+inf".equals(bound)) {
            return io.lettuce.core.Range.Boundary.including(Double.POSITIVE_INFINITY);
        }
        if (bound.startsWith("(")) {
            return io.lettuce.core.Range.Boundary.excluding(Double.parseDouble(bound.substring(1)));
        }
        return io.lettuce.core.Range.Boundary.including(Double.parseDouble(bound));
    }

    static io.lettuce.core.ZAggregateArgs toLettuceZAggregateArgs(ZAggregateArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.ZAggregateArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

    static io.lettuce.core.ZStoreArgs toLettuceZStoreArgs(ZAggregateArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.ZStoreArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

    static ZRange.ByIndex toLettuceByIndex(long start, long stop, ZRangeArgs quarkus) {
        if (quarkus.toArgs().contains("LIMIT")) {
            throw new IllegalArgumentException("LIMIT is only supported in combination with either BYSCORE or BYLEX");
        }
        ZRange.ByIndex range = ZRange.byIndex(start, stop);
        if (quarkus.isReverse()) {
            range.rev();
        }
        return range;
    }

    static io.lettuce.core.Range<byte[]> toLettuceLexRange(Range<String> range) {
        return toLettuceLexRange(range, false);
    }

    private static io.lettuce.core.Range<byte[]> toLettuceLexRange(Range<String> range, boolean swapBounds) {
        String lowerBound = swapBounds ? range.getUpperBound() : range.getLowerBound();
        String upperBound = swapBounds ? range.getLowerBound() : range.getUpperBound();
        return io.lettuce.core.Range.from(toLexBoundary(lowerBound), toLexBoundary(upperBound));
    }

    static ZRange.ByScore toLettuceByScore(ScoreRange<Double> range, ZRangeArgs quarkus) {
        boolean swapBounds = quarkus.isReverse() && !range.isUnbounded();
        ZRange.ByScore byScore = ZRange.byScore(toLettuceScoreRange(range, swapBounds));
        if (quarkus.isReverse()) {
            byScore.rev();
        }
        return byScore.limit(toLettuceLimit(quarkus));
    }

    static ZRange.ByLex<byte[]> toLettuceByLex(Range<String> range, ZRangeArgs quarkus) {
        // No pre-swap: Vert.x swaps BYLEX REV bounds unconditionally, exactly as Lettuce does on its own.
        ZRange.ByLex<byte[]> byLex = ZRange.byLex(toLettuceLexRange(range));
        if (quarkus.isReverse()) {
            byLex.rev();
        }
        return byLex.limit(toLettuceLimit(quarkus));
    }

    static io.lettuce.core.Range<Long> toLettuceIndexRange(long min, long max, ZRangeArgs quarkus) {
        if (quarkus.toArgs().contains("LIMIT")) {
            throw new IllegalArgumentException("LIMIT is only supported in combination with either BYSCORE or BYLEX");
        }
        return io.lettuce.core.Range.create(min, max);
    }

    static io.lettuce.core.Range<Number> toLettuceStoreScoreRange(ScoreRange<Double> range, ZRangeArgs quarkus) {
        return toLettuceScoreRange(range, quarkus.isReverse());
    }

    static io.lettuce.core.Range<byte[]> toLettuceStoreLexRange(Range<String> range, ZRangeArgs quarkus) {
        return toLettuceLexRange(range, quarkus.isReverse());
    }

    static Limit toLettuceLimit(ZRangeArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        int index = tokens.indexOf("LIMIT");
        if (index < 0) {
            return Limit.unlimited();
        }
        long offset = Long.parseLong(tokens.get(index + 1).toString());
        long count = Long.parseLong(tokens.get(index + 2).toString());
        return Limit.create(offset, count);
    }

    private static io.lettuce.core.Range.Boundary<byte[]> toLexBoundary(String bound) {
        if ("-".equals(bound) || "+".equals(bound)) {
            return io.lettuce.core.Range.Boundary.unbounded();
        }
        if (bound.startsWith("(")) {
            return io.lettuce.core.Range.Boundary.excluding(bound.substring(1).getBytes(StandardCharsets.UTF_8));
        }
        if (bound.startsWith("[")) {
            return io.lettuce.core.Range.Boundary.including(bound.substring(1).getBytes(StandardCharsets.UTF_8));
        }
        throw new IllegalStateException("Unexpected lexicographical boundary: " + bound);
    }

}
