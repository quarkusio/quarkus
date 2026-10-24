package io.quarkus.redis.lettuce.runtime.internal.sortedset;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.SortArgs;
import io.quarkus.redis.datasource.codecs.Codec;
import io.quarkus.redis.datasource.codecs.Codecs;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.sortedset.Range;
import io.quarkus.redis.datasource.sortedset.ReactiveSortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ScoreRange;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ZAddArgs;
import io.quarkus.redis.datasource.sortedset.ZAggregateArgs;
import io.quarkus.redis.datasource.sortedset.ZRangeArgs;
import io.quarkus.redis.datasource.sortedset.ZScanCursor;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Place;
import tools.jackson.core.type.TypeReference;

@SuppressWarnings("unchecked")
class LettuceSortedSetCommandsTest extends CommandsTestBase {

    static final String value = "value";

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveSortedSetCommands<String, Place> reactiveSortedSetOfPlaces;
    ReactiveSortedSetCommands<String, String> reactiveSortedSetOfStrings;
    SortedSetCommands<String, Place> blockingSortedSetOfPlaces;
    SortedSetCommands<String, String> blockingSortedSetOfStrings;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveSortedSetOfPlaces = reactiveDs.sortedSet(Place.class);
        reactiveSortedSetOfStrings = reactiveDs.sortedSet(String.class);
        blockingSortedSetOfPlaces = blockingDs.sortedSet(Place.class);
        blockingSortedSetOfStrings = blockingDs.sortedSet(String.class);
    }

    private void populate() {
        blockingSortedSetOfPlaces.zadd(key, Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0));
    }

    private static final Codec PLACE_CODEC = Codecs.getDefaultCodecFor(Place.class);

    private static Place decodePlace(byte[] bytes) {
        return (Place) PLACE_CODEC.decode(bytes);
    }

    private static List<Place> rawZrange(String key) {
        List<Place> values = new ArrayList<>();
        for (byte[] value : connection.sync().zrange(key.getBytes(StandardCharsets.UTF_8), 0, -1)) {
            values.add(decodePlace(value));
        }
        return values;
    }

    private static List<ScoredValue<Place>> rawZrangeWithScores(String key) {
        return toScoredValues(connection.sync().zrangeWithScores(key.getBytes(StandardCharsets.UTF_8), 0, -1));
    }

    private static List<ScoredValue<Place>> rawZrevrangeWithScores(String key) {
        return toScoredValues(connection.sync().zrevrangeWithScores(key.getBytes(StandardCharsets.UTF_8), 0, -1));
    }

    private static List<ScoredValue<Place>> toScoredValues(List<io.lettuce.core.ScoredValue<byte[]>> raw) {
        List<ScoredValue<Place>> values = new ArrayList<>();
        for (io.lettuce.core.ScoredValue<byte[]> value : raw) {
            values.add(ScoredValue.of(decodePlace(value.getValue()), value.getScore()));
        }
        return values;
    }

    @Test
    void getDataSource() {
        assertThat(reactiveDs).isEqualTo(reactiveSortedSetOfPlaces.getDataSource());
        assertThat(reactiveDs).isEqualTo(reactiveSortedSetOfStrings.getDataSource());
        assertThat(blockingDs).isEqualTo(blockingSortedSetOfStrings.getDataSource());
        assertThat(blockingDs).isEqualTo(blockingSortedSetOfPlaces.getDataSource());
    }

    @Test
    void zadd() {
        assertThat(blockingSortedSetOfPlaces.zadd(key, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(key, 1.0, Place.crussol)).isFalse();

        assertThat(rawZrange(key)).isEqualTo(List.of(Place.crussol));
        assertThat(
                blockingSortedSetOfPlaces.zadd(key, new ScoredValue<>(Place.grignan, 2.0), new ScoredValue<>(Place.suze, 3.0)))
                .isEqualTo(2);
        assertThat(rawZrange(key)).isEqualTo(List.of(Place.crussol, Place.grignan, Place.suze));
    }

    @Test
    void zaddScoredValue() {
        assertThat(blockingSortedSetOfPlaces.zadd(key, ScoredValue.of(Place.crussol, 1.0))).isEqualTo(1);
        assertThat(blockingSortedSetOfPlaces.zadd(key, ScoredValue.of(Place.crussol, 1.0))).isEqualTo(0);

        assertThat(rawZrange(key)).isEqualTo(List.of(Place.crussol));
        assertThat(blockingSortedSetOfPlaces.zadd(key, ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.suze, 3.0)))
                .isEqualTo(2);
        assertThat(rawZrange(key)).isEqualTo(List.of(Place.crussol, Place.grignan, Place.suze));
    }

    @Test
    void zaddnx() {
        assertThat(blockingSortedSetOfPlaces.zadd(key, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(key, new ZAddArgs().nx(), ScoredValue.of(Place.crussol, 2.0))).isEqualTo(0);
        assertThat(blockingSortedSetOfPlaces.zadd(key, new ZAddArgs().nx(), ScoredValue.of(Place.grignan, 2.0))).isEqualTo(1);

        assertThat(blockingSortedSetOfPlaces.zadd(key, new ZAddArgs().nx(), Map.of(Place.grignan, 2.0, Place.suze, 3.0)))
                .isEqualTo(1);
        assertThat(rawZrangeWithScores(key)).isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0),
                ScoredValue.of(Place.grignan, 2.0),
                ScoredValue.of(Place.suze, 3.0)));
    }

    @Test
    void zaddxx() {
        assertThat(blockingSortedSetOfPlaces.zadd(key, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(key, new ZAddArgs().xx(), 2.0, Place.crussol)).isFalse();

        assertThat(blockingSortedSetOfPlaces.zadd(key, new ZAddArgs().xx(), 2.0, Place.grignan)).isFalse();

        assertThat(rawZrangeWithScores(key)).isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0)));
    }

    @Test
    void zaddch() {
        assertThat(blockingSortedSetOfPlaces.zadd(key, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(key, new ZAddArgs().ch().xx(), 2.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(key, new ZAddArgs().ch(), 2.0, Place.grignan)).isTrue();

        assertThat(rawZrangeWithScores(key))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0), ScoredValue.of(Place.grignan, 2.0)));
    }

    @Test
    void zaddincr() {
        assertThat(blockingSortedSetOfPlaces.zadd(key, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zaddincr(key, 2.0, Place.crussol)).isEqualTo(3.0);
        assertThat(blockingSortedSetOfPlaces.zaddincr(key, 2.0, Place.grignan)).isEqualTo(2.0);
        assertThat(blockingSortedSetOfPlaces.zaddincr("missing", 2.0, Place.grignan)).isEqualTo(2.0);

        assertThat(rawZrangeWithScores(key))
                .isEqualTo(List.of(ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.crussol, 3.0)));
    }

    @Test
    void zaddincrnx() {
        assertThat(blockingSortedSetOfPlaces.zaddincr(key, new ZAddArgs().nx(), 2.0, Place.crussol)).hasValue(2.0);
        assertThat(blockingSortedSetOfPlaces.zaddincr(key, new ZAddArgs().nx(), 2.0, Place.crussol)).isEmpty();
    }

    @Test
    void zaddincrxx() {
        assertThat(blockingSortedSetOfPlaces.zaddincr(key, new ZAddArgs().xx(), 2.0, Place.crussol)).isEmpty();
        assertThat(blockingSortedSetOfPlaces.zaddincr(key, new ZAddArgs().nx(), 2.0, Place.crussol)).hasValue(2.0);
        assertThat(blockingSortedSetOfPlaces.zaddincr(key, new ZAddArgs().xx(), 2.0, Place.crussol)).hasValue(4.0);
    }

    @Test
    void zaddgt() {
        assertThat(blockingSortedSetOfPlaces.zadd(key, 1.0, Place.crussol)).isTrue();
        // new score less than the current score
        assertThat(blockingSortedSetOfPlaces.zadd(key, new ZAddArgs().gt(), 0.0, Place.crussol)).isFalse();
        assertThat(rawZrangeWithScores(key)).isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0)));

        // new score greater than the current score
        assertThat(blockingSortedSetOfPlaces.zadd(key, new ZAddArgs().gt(), 2.0, Place.crussol)).isFalse();
        assertThat(rawZrangeWithScores(key)).isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0)));

        // add new element
        assertThat(blockingSortedSetOfPlaces.zadd(key, new ZAddArgs().gt(), 0.0, Place.grignan)).isTrue();
        assertThat(rawZrangeWithScores(key)).isEqualTo(
                List.of(ScoredValue.of(Place.grignan, 0.0), ScoredValue.of(Place.crussol, 2.0)));
    }

    @Test
    void zaddlt() {
        assertThat(blockingSortedSetOfPlaces.zadd(key, 2.0, Place.crussol)).isTrue();
        // new score greater than the current score
        assertThat(blockingSortedSetOfPlaces.zadd(key, new ZAddArgs().lt(), 3.0, Place.crussol)).isFalse();
        assertThat(rawZrangeWithScores(key)).isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0)));

        // new score less than the current score
        assertThat(blockingSortedSetOfPlaces.zadd(key, new ZAddArgs().lt(), 1.0, Place.crussol)).isFalse();
        assertThat(rawZrangeWithScores(key)).isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0)));

        // add new element
        assertThat(blockingSortedSetOfPlaces.zadd(key, new ZAddArgs().lt(), 0.0, Place.grignan)).isTrue();
        assertThat(rawZrangeWithScores(key)).isEqualTo(
                List.of(ScoredValue.of(Place.grignan, 0.0), ScoredValue.of(Place.crussol, 1.0)));
    }

    @Test
    void zcard() {
        assertThat(blockingSortedSetOfPlaces.zcard(key)).isEqualTo(0);
        assertThat(blockingSortedSetOfPlaces.zadd(key, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zcard(key)).isEqualTo(1);
    }

    @Test
    void zcount() {
        assertThat(blockingSortedSetOfPlaces.zcount(key, ScoreRange.from(0, 0))).isEqualTo(0);

        assertThat(blockingSortedSetOfPlaces.zadd(key, new ScoredValue<>(Place.crussol, 1.0),
                new ScoredValue<>(Place.grignan, 2.0),
                new ScoredValue<>(Place.suze, 2.1)))
                .isEqualTo(3);

        assertThat(blockingSortedSetOfPlaces.zcount(key, ScoreRange.from(1.0, 3.0))).isEqualTo(3);
        assertThat(blockingSortedSetOfPlaces.zcount(key, ScoreRange.from(1.0, 2.0))).isEqualTo(2);
        assertThat(blockingSortedSetOfPlaces.zcount(key, ScoreRange.from(NEGATIVE_INFINITY, POSITIVE_INFINITY))).isEqualTo(3);

        assertThat(blockingSortedSetOfPlaces.zcount(key, new ScoreRange<>(1.0, false, 3.0, true))).isEqualTo(2);
        assertThat(blockingSortedSetOfPlaces.zcount(key, ScoreRange.unbounded())).isEqualTo(3);
    }

    @Test
    void zdiff() {
        String zset1 = "zset1";
        String zset2 = "zset2";

        assertThat(blockingSortedSetOfPlaces.zadd(zset1, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset1, 2.0, Place.grignan)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset1, 3.0, Place.suze)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 2.0, Place.grignan)).isTrue();

        assertThat(blockingSortedSetOfPlaces.zdiff(zset1, zset2)).isEqualTo(List.of(Place.suze));
        assertThat(blockingSortedSetOfPlaces.zdiffWithScores(zset1, zset2)).isEqualTo(List.of(ScoredValue.of(Place.suze, 3.0)));

        assertThatThrownBy(() -> blockingSortedSetOfPlaces.zdiff(zset2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> blockingSortedSetOfPlaces.zdiff()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zdiffstore() {
        String zset1 = "zset1";
        String zset2 = "zset2";

        assertThat(blockingSortedSetOfPlaces.zadd(zset1, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset1, 2.0, Place.grignan)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset1, 3.0, Place.suze)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 2.0, Place.grignan)).isTrue();

        assertThat(blockingSortedSetOfPlaces.zdiffstore("out", zset1, zset2)).isEqualTo(1);
        assertThat(rawZrangeWithScores("out")).isEqualTo(List.of(ScoredValue.of(Place.suze, 3.0)));
    }

    @Test
    void zincrby() {
        assertThat(blockingSortedSetOfPlaces.zincrby(key, 0.0, Place.crussol)).isEqualTo(0.0, offset(0.1));
        assertThat(blockingSortedSetOfPlaces.zincrby(key, 1.1, Place.crussol)).isEqualTo(1.1, offset(0.1));
        assertThat(blockingSortedSetOfPlaces.zscore(key, Place.crussol)).hasValueCloseTo(1.1, offset(0.1));
        assertThat(blockingSortedSetOfPlaces.zincrby(key, -1.2, Place.crussol)).isEqualTo(-0.1, offset(0.1));
    }

    @Test
    void zintercard() {
        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        blockingSortedSetOfPlaces.zadd("zset2", Map.of(Place.crussol, 2.0, Place.grignan, 1.0));
        assertThat(blockingSortedSetOfPlaces.zintercard("zset1", "zset2")).isEqualTo(2);
        assertThat(blockingSortedSetOfPlaces.zintercard(1, "zset1", "zset2")).isEqualTo(1);
    }

    @Test
    void zinterstore() {
        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        blockingSortedSetOfPlaces.zadd("zset2", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));
        assertThat(blockingSortedSetOfPlaces.zinterstore(key, "zset1", "zset2")).isEqualTo(2);
        assertThat(rawZrange(key)).isEqualTo(List.of(Place.crussol, Place.grignan));
        assertThat(rawZrangeWithScores(key))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 3.0), ScoredValue.of(Place.grignan, 5.0)));
    }

    @Test
    void zinterstoreWithArgs() {
        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        blockingSortedSetOfPlaces.zadd("zset2", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));
        assertThat(blockingSortedSetOfPlaces.zinterstore(key, new ZAggregateArgs().max(), "zset1", "zset2")).isEqualTo(2);
        assertThat(rawZrange(key)).isEqualTo(List.of(Place.crussol, Place.grignan));
        assertThat(rawZrevrangeWithScores(key))
                .isEqualTo(List.of(ScoredValue.of(Place.grignan, 3.0), ScoredValue.of(Place.crussol, 2.0)));
    }

    @Test
    void bzpopmin() {
        blockingSortedSetOfPlaces.zadd("zset", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));
        assertThat(blockingSortedSetOfPlaces.bzpopmin(Duration.ofSeconds(1), "zset"))
                .isEqualTo(KeyValue.of("zset", ScoredValue.of(Place.crussol, 2.0)));
        assertThat(blockingSortedSetOfPlaces.bzpopmin(Duration.ofMillis(1000), "zset2")).isNull();
    }

    @Test
    void bzpopmax() {
        blockingSortedSetOfPlaces.zadd("zset", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));
        assertThat(blockingSortedSetOfPlaces.bzpopmax(Duration.ofSeconds(1), "zset"))
                .isEqualTo(KeyValue.of("zset", ScoredValue.of(Place.suze, 4.0)));
        assertThat(blockingSortedSetOfPlaces.bzpopmax(Duration.ofSeconds(1), "zset2")).isNull();
    }

    @Test
    void blockingCommandsSupportSubSecondTimeouts() {
        Duration timeout = Duration.ofMillis(250);
        assertThat(blockingSortedSetOfPlaces.bzpopmin(timeout, "missing")).isNull();
        assertThat(blockingSortedSetOfPlaces.bzpopmax(timeout, "missing")).isNull();
        assertThat(blockingSortedSetOfPlaces.bzmpopMin(timeout, "missing")).isNull();
        assertThat(blockingSortedSetOfPlaces.bzmpopMax(timeout, "missing")).isNull();
        assertThat(blockingSortedSetOfPlaces.bzmpopMin(timeout, 2, "missing")).isEmpty();
        assertThat(blockingSortedSetOfPlaces.bzmpopMax(timeout, 2, "missing")).isEmpty();
    }

    @Test
    void zpopmin() {
        blockingSortedSetOfPlaces.zadd("zset", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));

        assertThat(blockingSortedSetOfPlaces.zpopmin("zset")).isEqualTo(ScoredValue.of(Place.crussol, 2.0));
        assertThat(blockingSortedSetOfPlaces.zpopmin("zset", 2)).containsExactly(ScoredValue.of(Place.grignan, 3.0),
                ScoredValue.of(Place.suze, 4.0));
        assertThat(blockingSortedSetOfPlaces.zpopmin("foo")).isEqualTo(ScoredValue.empty());
    }

    @Test
    void zpopmax() {
        blockingSortedSetOfPlaces.zadd("zset", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));

        assertThat(blockingSortedSetOfPlaces.zpopmax("zset")).isEqualTo(ScoredValue.of(Place.suze, 4.0));
        assertThat(blockingSortedSetOfPlaces.zpopmax("zset", 2)).containsExactlyInAnyOrder(ScoredValue.of(Place.crussol, 2.0),
                ScoredValue.of(Place.grignan, 3.0));
        assertThat(blockingSortedSetOfPlaces.zpopmax("foo")).isEqualTo(ScoredValue.empty());
    }

    @Test
    void zrandmember() {
        blockingSortedSetOfPlaces.zadd("zset", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));
        assertThat(blockingSortedSetOfPlaces.zrandmember("zset")).isIn(Place.crussol, Place.grignan, Place.suze);
        assertThat(blockingSortedSetOfPlaces.zrandmember("zset", 2)).hasSize(2).containsAnyOf(Place.crussol, Place.grignan,
                Place.suze);
        assertThat(blockingSortedSetOfPlaces.zrandmemberWithScores("zset")).isIn(ScoredValue.of(Place.crussol, 2.0),
                ScoredValue.of(Place.grignan, 3.0), ScoredValue.of(Place.suze, 4.0));
        assertThat(blockingSortedSetOfPlaces.zrandmemberWithScores("zset", 2)).hasSize(2).containsAnyOf(
                ScoredValue.of(Place.crussol, 2.0),
                ScoredValue.of(Place.grignan, 3.0), ScoredValue.of(Place.suze, 4.0));
    }

    @Test
    void zrange() {
        populate();
        assertThat(blockingSortedSetOfPlaces.zrange(key, 0, -1)).isEqualTo(List.of(Place.crussol, Place.grignan, Place.suze));
    }

    @Test
    void zrangeWithScores() {
        populate();
        assertThat(blockingSortedSetOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(
                List.of(ScoredValue.of(Place.crussol, 1.0), ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.suze, 3.0)));
    }

    @Test
    void zrangebyscore() {
        blockingSortedSetOfPlaces.zadd(key,
                Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));

        assertThat(blockingSortedSetOfPlaces.zrangebyscore(key, new ScoreRange<>(2.0, 3.0)))
                .isEqualTo(List.of(Place.grignan, Place.suze));
        assertThat(blockingSortedSetOfPlaces.zrangebyscore(key, new ScoreRange<>(1.0, false, 4.0, false)))
                .isEqualTo(List.of(Place.grignan, Place.suze));
        assertThat(blockingSortedSetOfPlaces.zrangebyscore(key, new ScoreRange<>(NEGATIVE_INFINITY, POSITIVE_INFINITY)))
                .isEqualTo(List.of(Place.crussol, Place.grignan, Place.suze, Place.adhemar));
        assertThat(blockingSortedSetOfPlaces.zrangebyscore(key, ScoreRange.unbounded()))
                .isEqualTo(List.of(Place.crussol, Place.grignan, Place.suze, Place.adhemar));
        assertThat(blockingSortedSetOfPlaces.zrangebyscore(key, new ScoreRange<>(0.0, 4.0), new ZRangeArgs().limit(1, 3)))
                .isEqualTo(List.of(Place.grignan, Place.suze, Place.adhemar));
        assertThat(blockingSortedSetOfPlaces.zrangebyscore(key, ScoreRange.unbounded(), new ZRangeArgs().limit(2, 2)))
                .isEqualTo(List.of(Place.suze, Place.adhemar));
    }

    @Test
    void zrangebyscoreWithScores() {
        blockingSortedSetOfPlaces.zadd(key,
                Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));

        assertThat(blockingSortedSetOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(2.0, 3.0)))
                .isEqualTo(List.of(ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.suze, 3.0)));
        assertThat(blockingSortedSetOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(1.0, false, 3.0, true)))
                .isEqualTo(List.of(ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.suze, 3.0)));
        assertThat(
                blockingSortedSetOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(NEGATIVE_INFINITY, POSITIVE_INFINITY)))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0), ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.suze, 3.0),
                        ScoredValue.of(Place.adhemar, 4.0)));
        assertThat(blockingSortedSetOfPlaces.zrangebyscoreWithScores(key, ScoreRange.unbounded()))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0), ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.suze, 3.0),
                        ScoredValue.of(Place.adhemar, 4.0)));
        assertThat(blockingSortedSetOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(0.0, 4.0),
                new ZRangeArgs().limit(1, 3)))
                .isEqualTo(List.of(ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.suze, 3.0),
                        ScoredValue.of(Place.adhemar, 4.0)));
        assertThat(blockingSortedSetOfPlaces.zrangebyscoreWithScores(key, ScoreRange.unbounded(), new ZRangeArgs().limit(2, 2)))
                .isEqualTo(List.of(ScoredValue.of(Place.suze, 3.0), ScoredValue.of(Place.adhemar, 4.0)));
    }

    @Test
    void zrangebyscoreWithScoresInfinity() {
        blockingSortedSetOfPlaces.zadd(key,
                Map.of(Place.crussol, Double.POSITIVE_INFINITY, Place.grignan, Double.NEGATIVE_INFINITY));
        assertThat(blockingSortedSetOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(null, null))).hasSize(2);
        assertThat(blockingSortedSetOfPlaces.zrangebyscoreWithScores(key, ScoreRange.unbounded())).hasSize(2);
    }

    @Test
    void zrangestorebylex() {
        blockingSortedSetOfStrings.zadd(key, Map.of("a", 1.0, "b", 1.0, "c", 1.0, "d", 1.0));
        assertThat(
                blockingSortedSetOfStrings.zrangestorebylex("key1", key, new Range<>("b", "d"), new ZRangeArgs().limit(0, 4)))
                .isEqualTo(3);
        assertThat(blockingSortedSetOfStrings.zrange("key1", 0, 1)).isEqualTo(List.of("b", "c"));

        assertThat(blockingSortedSetOfStrings.zrangestorebylex("key1", key, new Range<>("b", "d"))).isEqualTo(3);
        assertThat(blockingSortedSetOfStrings.zrange("key1", 0, 1)).isEqualTo(List.of("b", "c"));
    }

    @Test
    void zrangestorebyscore() {
        blockingSortedSetOfPlaces.zadd(key,
                Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));
        assertThat(blockingSortedSetOfPlaces.zrangestorebyscore("key1", key, new ScoreRange<>(0.0, 2.0),
                new ZRangeArgs().limit(0, 2))).isEqualTo(2);
        assertThat(blockingSortedSetOfPlaces.zrange("key1", 0, 2)).isEqualTo(List.of(Place.crussol, Place.grignan));

        assertThat(blockingSortedSetOfPlaces.zrangestorebyscore("key1", key, new ScoreRange<>(0.0, 2.0))).isEqualTo(2);
        assertThat(blockingSortedSetOfPlaces.zrange("key1", 0, 2)).isEqualTo(List.of(Place.crussol, Place.grignan));
    }

    @Test
    void zrangestore() {
        blockingSortedSetOfPlaces.zadd(key,
                Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));
        assertThat(blockingSortedSetOfPlaces.zrangestore("key1", key, 0, -1)).isEqualTo(4);
        assertThat(blockingSortedSetOfPlaces.zrange("key1", 0, -1))
                .isEqualTo(List.of(Place.crussol, Place.grignan, Place.suze, Place.adhemar));

        assertThat(blockingSortedSetOfPlaces.zrangestore("key1", key, 2, 4, new ZRangeArgs().rev())).isEqualTo(2);
        assertThat(blockingSortedSetOfPlaces.zrange("key1", 0, -1)).isEqualTo(List.of(Place.crussol, Place.grignan));
    }

    @Test
    void zrank() {
        assertThat(blockingSortedSetOfPlaces.zrank(key, Place.crussol)).isEmpty();
        populate();
        assertThat(blockingSortedSetOfPlaces.zrank(key, Place.crussol)).hasValue(0);
        assertThat(blockingSortedSetOfPlaces.zrank(key, Place.suze)).hasValue(2);
    }

    @Test
    void zrem() {
        assertThat(blockingSortedSetOfPlaces.zrem(key, Place.crussol)).isEqualTo(0);
        populate();
        assertThat(blockingSortedSetOfPlaces.zrem(key, Place.grignan)).isEqualTo(1);
        assertThat(rawZrange(key)).isEqualTo(List.of(Place.crussol, Place.suze));
        assertThat(blockingSortedSetOfPlaces.zrem(key, Place.crussol, Place.suze)).isEqualTo(2);
        assertThat(rawZrange(key)).isEqualTo(List.of());
    }

    @Test
    void zremrangebyscore() {
        populate();
        assertThat(blockingSortedSetOfPlaces.zremrangebyscore(key, new ScoreRange<>(1.0, 2.0))).isEqualTo(2);
        assertThat(rawZrange(key)).isEqualTo(List.of(Place.suze));

        populate();
        assertThat(blockingSortedSetOfPlaces.zremrangebyscore(key, new ScoreRange<>(1.0, false, 3.0, false)))
                .isEqualTo(1);
        assertThat(rawZrange(key)).isEqualTo(List.of(Place.crussol, Place.suze));
    }

    @Test
    void zremrangebyrank() {
        blockingSortedSetOfPlaces.zadd(key,
                Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));
        assertThat(blockingSortedSetOfPlaces.zremrangebyrank(key, 1, 2)).isEqualTo(2);
        assertThat(rawZrange(key)).isEqualTo(List.of(Place.crussol, Place.adhemar));

        blockingSortedSetOfPlaces.zadd(key,
                Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));
        assertThat(blockingSortedSetOfPlaces.zremrangebyrank(key, 0, -1)).isEqualTo(4);
        assertThat(blockingSortedSetOfPlaces.zcard(key)).isEqualTo(0);
    }

    @Test
    void zrevrange() {
        populate();
        assertThat(blockingSortedSetOfPlaces.zrange(key, 0, -1, new ZRangeArgs().rev()))
                .isEqualTo(List.of(Place.suze, Place.grignan, Place.crussol));
    }

    @Test
    void zrevrangeWithScoreEmpty() {
        assertThat(blockingDs.sortedSet(String.class).zrangeWithScores("top-products", 0, 2, new ZRangeArgs().rev())).isEmpty();
        assertThat(blockingDs.sortedSet(String.class).zrangeWithScores("missing", 0, 2)).isEmpty();
    }

    @Test
    void zrevrangeWithScores() {
        populate();
        assertThat(blockingSortedSetOfPlaces.zrangeWithScores(key, 0, -1, new ZRangeArgs().rev()))
                .isEqualTo(List.of(ScoredValue.of(Place.suze, 3.0), ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.crussol, 1.0)));
    }

    @Test
    void zrevrangebylex() {
        populateManyStringEntriesForLex();
        assertThat(blockingSortedSetOfStrings.zrangebylex(key, Range.unbounded(), new ZRangeArgs().rev())).hasSize(100);
        assertThat(blockingSortedSetOfStrings.zrangebylex(key, new Range<>("value", "zzz"), new ZRangeArgs().rev()))
                .hasSize(100);
        assertThat(blockingSortedSetOfStrings.zrangebylex(key, new Range<>("value98", true, "value99", true),
                new ZRangeArgs().rev())).containsSequence("value99", "value98");
        assertThat(
                blockingSortedSetOfStrings.zrangebylex(key, new Range<>("value99", true, null, true), new ZRangeArgs().rev()))
                .hasSize(1);
        assertThat(
                blockingSortedSetOfStrings.zrangebylex(key, new Range<>("value99", false, null, false), new ZRangeArgs().rev()))
                .hasSize(0);
    }

    @Test
    void zrevrangebyscore() {
        blockingSortedSetOfPlaces.zadd(key,
                Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));
        ZRangeArgs rev = new ZRangeArgs().rev();
        assertThat(blockingSortedSetOfPlaces.zrangebyscore(key, new ScoreRange<>(3.0, 2.0), rev))
                .isEqualTo(List.of(Place.suze, Place.grignan));
        assertThat(blockingSortedSetOfPlaces.zrangebyscore(key, new ScoreRange<>(4.0, false, 1.0, false), rev))
                .isEqualTo(List.of(Place.suze, Place.grignan));
        assertThat(blockingSortedSetOfPlaces.zrangebyscore(key, new ScoreRange<>(POSITIVE_INFINITY, NEGATIVE_INFINITY), rev))
                .isEqualTo(List.of(Place.adhemar, Place.suze, Place.grignan, Place.crussol));
        assertThat(blockingSortedSetOfPlaces.zrangebyscore(key, ScoreRange.unbounded(), rev))
                .isEqualTo(List.of(Place.adhemar, Place.suze, Place.grignan, Place.crussol));
        assertThat(blockingSortedSetOfPlaces.zrangebyscore(key, new ScoreRange<>(4.0, 0.0), new ZRangeArgs().rev().limit(1, 3)))
                .isEqualTo(List.of(Place.suze, Place.grignan, Place.crussol));
        assertThat(blockingSortedSetOfPlaces.zrangebyscore(key, ScoreRange.unbounded(), new ZRangeArgs().rev().limit(2, 2)))
                .isEqualTo(List.of(Place.grignan, Place.crussol));
    }

    @Test
    void zrevrangebyscoreWithScores() {
        blockingSortedSetOfPlaces.zadd(key,
                Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));
        ZRangeArgs rev = new ZRangeArgs().rev();
        assertThat(blockingSortedSetOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(3.0, 2.0), rev))
                .isEqualTo(List.of(ScoredValue.of(Place.suze, 3.0), ScoredValue.of(Place.grignan, 2.0)));
        assertThat(blockingSortedSetOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(4.0, false, 1.0, false), rev))
                .isEqualTo(List.of(ScoredValue.of(Place.suze, 3.0), ScoredValue.of(Place.grignan, 2.0)));
        assertThat(blockingSortedSetOfPlaces.zrangebyscoreWithScores(key,
                new ScoreRange<>(POSITIVE_INFINITY, NEGATIVE_INFINITY), rev))
                .isEqualTo(List.of(ScoredValue.of(Place.adhemar, 4.0), ScoredValue.of(Place.suze, 3.0),
                        ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.crussol, 1.0)));
        assertThat(blockingSortedSetOfPlaces.zrangebyscoreWithScores(key, ScoreRange.unbounded(), rev))
                .isEqualTo(List.of(ScoredValue.of(Place.adhemar, 4.0), ScoredValue.of(Place.suze, 3.0),
                        ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.crussol, 1.0)));
        assertThat(blockingSortedSetOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(4.0, 0.0),
                new ZRangeArgs().rev().limit(1, 3)))
                .isEqualTo(List.of(ScoredValue.of(Place.suze, 3.0), ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.crussol, 1.0)));
        assertThat(blockingSortedSetOfPlaces.zrangebyscoreWithScores(key, ScoreRange.unbounded(),
                new ZRangeArgs().rev().limit(2, 2)))
                .isEqualTo(List.of(ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.crussol, 1.0)));
    }

    @Test
    void zrevrank() {
        assertThat(blockingSortedSetOfPlaces.zrevrank(key, Place.crussol)).isEmpty();
        populate();
        assertThat(blockingSortedSetOfPlaces.zrevrank(key, Place.suze)).hasValue(0);
        assertThat(blockingSortedSetOfPlaces.zrevrank(key, Place.crussol)).hasValue(2);
    }

    @Test
    void zrevrangestorebylex() {
        blockingSortedSetOfStrings.zadd(key, Map.of("a", 1.0, "b", 2.0, "c", 3.0, "d", 4.0));
        assertThat(blockingSortedSetOfStrings.zrangestorebylex("key1", key, new Range<>("c", "-"),
                new ZRangeArgs().rev().limit(0, 4))).isEqualTo(3);
        assertThat(blockingSortedSetOfStrings.zrange("key1", 0, 2)).isEqualTo(List.of("a", "b", "c"));
    }

    @Test
    void zrevrangestorebyscore() {
        blockingSortedSetOfPlaces.zadd(key,
                Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));
        assertThat(
                blockingSortedSetOfPlaces.zrangestorebyscore("key1", key, new ScoreRange<>(2.0, true, 1.0, false),
                        new ZRangeArgs().rev().limit(0, 2)))
                .isEqualTo(1);
        assertThat(blockingSortedSetOfPlaces.zrange("key1", 0, 2)).isEqualTo(List.of(Place.grignan));
    }

    @Test
    void zscore() {
        assertThat(blockingSortedSetOfPlaces.zscore(key, Place.crussol)).isEmpty();
        blockingSortedSetOfPlaces.zadd(key, 1.0, Place.crussol);
        assertThat(blockingSortedSetOfPlaces.zscore(key, Place.crussol)).hasValue(1.0);
    }

    @Test
    void zunionstore() {
        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        blockingSortedSetOfPlaces.zadd("zset2", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));

        assertThat(blockingSortedSetOfPlaces.zunionstore(key, "zset1", "zset2")).isEqualTo(3);
        assertThat(blockingSortedSetOfPlaces.zunionstore(key + "2", new ZAggregateArgs().max(), "zset1", "zset2")).isEqualTo(3);

        assertThat(rawZrange(key)).isEqualTo(List.of(Place.crussol, Place.suze, Place.grignan));
        assertThat(rawZrangeWithScores(key))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 3.0), new ScoredValue<>(Place.suze, 4.0),
                        ScoredValue.of(Place.grignan, 5.0)));

        assertThat(blockingSortedSetOfPlaces.zunionstore(key, new ZAggregateArgs().weights(2.0, 3.0), "zset1", "zset2"))
                .isEqualTo(3);
        assertThat(rawZrangeWithScores(key)).isEqualTo(
                List.of(new ScoredValue<>(Place.crussol, 8.0), new ScoredValue<>(Place.suze, 12.0),
                        new ScoredValue<>(Place.grignan, 13.0)));

        assertThat(blockingSortedSetOfPlaces.zunionstore(key, new ZAggregateArgs().weights(2.0, 3.0).sum(), "zset1", "zset2"))
                .isEqualTo(3);
        assertThat(rawZrangeWithScores(key)).isEqualTo(
                List.of(new ScoredValue<>(Place.crussol, 8.0), new ScoredValue<>(Place.suze, 12.0),
                        new ScoredValue<>(Place.grignan, 13.0)));

        assertThat(blockingSortedSetOfPlaces.zunionstore(key, new ZAggregateArgs().min(), "zset1", "zset2")).isEqualTo(3);
        assertThat(rawZrangeWithScores(key)).isEqualTo(
                List.of(ScoredValue.of(Place.crussol, 1.0), new ScoredValue<>(Place.grignan, 2.0),
                        new ScoredValue<>(Place.suze, 4.0)));

        assertThat(blockingSortedSetOfPlaces.zunionstore(key, new ZAggregateArgs().weights(2.0, 3.0).min(), "zset1", "zset2"))
                .isEqualTo(3);
        assertThat(rawZrangeWithScores(key)).isEqualTo(
                List.of(ScoredValue.of(Place.crussol, 2.0), new ScoredValue<>(Place.grignan, 4.0),
                        new ScoredValue<>(Place.suze, 12.0)));

        assertThat(blockingSortedSetOfPlaces.zunionstore(key, new ZAggregateArgs().weights(2.0, 3.0).max(), "zset1", "zset2"))
                .isEqualTo(3);
        assertThat(rawZrangeWithScores(key)).isEqualTo(
                List.of(new ScoredValue<>(Place.crussol, 6.0), new ScoredValue<>(Place.grignan, 9.0),
                        new ScoredValue<>(Place.suze, 12.0)));
    }

    @Test
    void zinterstoreArgs() {
        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        blockingSortedSetOfPlaces.zadd("zset2", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));

        assertThat(blockingSortedSetOfPlaces.zinterstore(key, new ZAggregateArgs().sum(), "zset1", "zset2")).isEqualTo(2);
        assertThat(rawZrangeWithScores(key))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 3.0), ScoredValue.of(Place.grignan, 5.0)));

        assertThat(blockingSortedSetOfPlaces.zinterstore(key, new ZAggregateArgs().min(), "zset1", "zset2")).isEqualTo(2);
        assertThat(rawZrangeWithScores(key))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0), ScoredValue.of(Place.grignan, 2.0)));

        assertThat(blockingSortedSetOfPlaces.zinterstore(key, new ZAggregateArgs().max(), "zset1", "zset2")).isEqualTo(2);
        assertThat(rawZrangeWithScores(key))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0), new ScoredValue<>(Place.grignan, 3.0)));

        assertThat(blockingSortedSetOfPlaces.zinterstore(key, new ZAggregateArgs().weights(2, 3), "zset1", "zset2"))
                .isEqualTo(2);
        assertThat(rawZrangeWithScores(key))
                .isEqualTo(List.of(new ScoredValue<>(Place.crussol, 8.0), new ScoredValue<>(Place.grignan, 13.0)));

        assertThat(blockingSortedSetOfPlaces.zinterstore(key, new ZAggregateArgs().weights(2, 3).sum(), "zset1", "zset2"))
                .isEqualTo(2);
        assertThat(rawZrangeWithScores(key))
                .isEqualTo(List.of(new ScoredValue<>(Place.crussol, 8.0), new ScoredValue<>(Place.grignan, 13.0)));

        assertThat(blockingSortedSetOfPlaces.zinterstore(key, new ZAggregateArgs().weights(2, 3).min(), "zset1", "zset2"))
                .isEqualTo(2);
        assertThat(rawZrangeWithScores(key))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0), new ScoredValue<>(Place.grignan, 4.0)));

        assertThat(blockingSortedSetOfPlaces.zinterstore(key, new ZAggregateArgs().weights(2, 3).max(), "zset1", "zset2"))
                .isEqualTo(2);
        assertThat(rawZrangeWithScores(key))
                .isEqualTo(List.of(new ScoredValue<>(Place.crussol, 6.0), new ScoredValue<>(Place.grignan, 9.0)));
    }

    @Test
    void zscan() {
        blockingSortedSetOfPlaces.zadd(key, 1.0, Place.crussol);
        ZScanCursor<Place> cursor = blockingSortedSetOfPlaces.zscan(key);
        assertThat(cursor.hasNext()).isTrue();
        List<ScoredValue<Place>> values = cursor.next();
        assertThat(cursor.hasNext()).isFalse();
        assertThat(values.get(0)).isEqualTo(new ScoredValue<>(Place.crussol, 1.0));
    }

    @Test
    void zscanEmpty() {
        ZScanCursor<Place> cursor = blockingSortedSetOfPlaces.zscan(key);
        assertThat(cursor.hasNext()).isTrue();
        List<ScoredValue<Place>> values = cursor.next();
        assertThat(cursor.hasNext()).isFalse();
        assertThat(values).isEmpty();
    }

    @Test
    void zscanEmptyAsIterable() {
        ZScanCursor<Place> cursor = blockingSortedSetOfPlaces.zscan(key);
        assertThat(cursor.hasNext()).isTrue();
        Iterable<ScoredValue<Place>> iterable = cursor.toIterable();
        assertThat(iterable).isEmpty();
        assertThat(cursor.hasNext()).isFalse();
    }

    @Test
    void zscanWithCursorAndArgs() {
        blockingSortedSetOfPlaces.zadd(key, 1.0, Place.crussol);
        blockingSortedSetOfPlaces.zadd(key, 2.0, Place.grignan);
        blockingSortedSetOfPlaces.zadd(key, 3.0, Place.adhemar);
        ZScanCursor<Place> cursor = blockingSortedSetOfPlaces.zscan(key, new ScanArgs().count(2));
        assertThat(cursor.hasNext()).isTrue();
        List<ScoredValue<Place>> values = cursor.next();
        assertThat(cursor.hasNext()).isFalse();
        assertThat(values.get(0)).isEqualTo(new ScoredValue<>(Place.crussol, 1.0));
    }

    @Test
    void zscanMultiple() {
        populateManyStringEntries();

        ZScanCursor<String> cursor = blockingSortedSetOfStrings.zscan(key, new ScanArgs().count(5));
        assertThat(cursor).isNotNull();
        assertThat(cursor.hasNext()).isTrue();

        List<ScoredValue<String>> values = new ArrayList<>();
        while (cursor.hasNext()) {
            values.addAll(cursor.next());
        }
        assertThat(cursor.hasNext()).isFalse();
        assertThat(values).hasSize(100);
    }

    @Test
    void zscanMultipleAsIterable() {
        populateManyStringEntries();

        ZScanCursor<String> cursor = blockingSortedSetOfStrings.zscan(key, new ScanArgs().count(5));
        assertThat(cursor).isNotNull();
        assertThat(cursor.hasNext()).isTrue();

        List<ScoredValue<String>> values = new ArrayList<>();
        for (ScoredValue<String> scoredValue : cursor.toIterable()) {
            values.add(scoredValue);
        }
        assertThat(cursor.hasNext()).isFalse();
        assertThat(values).hasSize(100);
    }

    @Test
    void zscanMatch() {
        populateManyStringEntries();

        ZScanCursor<String> cursor = blockingSortedSetOfStrings.zscan(key, new ScanArgs().count(10).match("val*"));
        List<ScoredValue<String>> values = new ArrayList<>();
        while (cursor.hasNext()) {
            values.addAll(cursor.next());
        }
        assertThat(cursor.hasNext()).isFalse();
        assertThat(values).hasSize(100);
    }

    @Test
    void zlexcount() {
        populateManyStringEntriesForLex();
        assertThat(blockingSortedSetOfStrings.zlexcount(key, new Range<>("-", "+"))).isEqualTo(100);
        assertThat(blockingSortedSetOfStrings.zlexcount(key, new Range<>("value", "zzz"))).isEqualTo(100);

        assertThat(blockingSortedSetOfStrings.zlexcount(key, Range.unbounded())).isEqualTo(100);
        assertThat(blockingSortedSetOfStrings.zlexcount(key, new Range<>("value99", true, null, false))).isEqualTo(1);
        assertThat(blockingSortedSetOfStrings.zlexcount(key, new Range<>("value99", false, null, false))).isEqualTo(0);
    }

    @Test
    void zmscore() {
        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.zmscore("zset1", Place.crussol, Place.suze, Place.grignan))
                .isEqualTo(List.of(OptionalDouble.of(1.0), OptionalDouble.empty(), OptionalDouble.of(2.0)));
    }

    @Test
    void zmpopMin() {
        assertThat(blockingSortedSetOfPlaces.zmpopMin("zset1")).isEqualTo(null);

        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.zmpopMin("zset1")).isEqualTo(ScoredValue.of(Place.crussol, 1.0));
        assertThat(blockingSortedSetOfPlaces.zmpopMin("zset1")).isEqualTo(ScoredValue.of(Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.zmpopMin("zset1")).isNull();

        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.zmpopMin(2, "zset1")).containsExactly(ScoredValue.of(Place.crussol, 1.0),
                ScoredValue.of(Place.grignan, 2.0));

        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.zmpopMin(3, "zset1")).containsExactly(ScoredValue.of(Place.crussol, 1.0),
                ScoredValue.of(Place.grignan, 2.0));

        assertThat(blockingSortedSetOfPlaces.bzmpopMax(Duration.ofSeconds(1), 3, "zset1")).isEmpty();
    }

    @Test
    void zmpopMax() {
        assertThat(blockingSortedSetOfPlaces.zmpopMax("zset1")).isEqualTo(null);

        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.zmpopMax("zset1")).isEqualTo(ScoredValue.of(Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.zmpopMax("zset1")).isEqualTo(ScoredValue.of(Place.crussol, 1.0));
        assertThat(blockingSortedSetOfPlaces.zmpopMax("zset1")).isNull();

        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.zmpopMax(2, "zset1")).containsExactly(
                ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.crussol, 1.0));

        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.grignan, 2.0, Place.crussol, 1.0));
        assertThat(blockingSortedSetOfPlaces.zmpopMax(3, "zset1")).containsExactly(
                ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.crussol, 1.0));

        assertThat(blockingSortedSetOfPlaces.bzmpopMax(Duration.ofSeconds(1), 3, "zset1")).isEmpty();
    }

    @Test
    void bzmpopMin() {
        assertThat(blockingSortedSetOfPlaces.bzmpopMin(Duration.ofSeconds(1), "zset1")).isEqualTo(null);

        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.bzmpopMin(Duration.ofSeconds(10), "zset1"))
                .isEqualTo(ScoredValue.of(Place.crussol, 1.0));
        assertThat(blockingSortedSetOfPlaces.bzmpopMin(Duration.ofSeconds(10), "zset1"))
                .isEqualTo(ScoredValue.of(Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.bzmpopMin(Duration.ofSeconds(1), "zset1")).isNull();

        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.bzmpopMin(Duration.ofSeconds(10), 2, "zset1")).containsExactly(
                ScoredValue.of(Place.crussol, 1.0),
                ScoredValue.of(Place.grignan, 2.0));

        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.bzmpopMin(Duration.ofSeconds(10), 3, "zset1")).containsExactly(
                ScoredValue.of(Place.crussol, 1.0),
                ScoredValue.of(Place.grignan, 2.0));

        assertThat(blockingSortedSetOfPlaces.bzmpopMax(Duration.ofSeconds(1), 3, "zset1")).isEmpty();
    }

    @Test
    void bzmpopMax() {
        assertThat(blockingSortedSetOfPlaces.bzmpopMax(Duration.ofSeconds(1), "zset1")).isEqualTo(null);

        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.bzmpopMax(Duration.ofSeconds(10), "zset1"))
                .isEqualTo(ScoredValue.of(Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.bzmpopMax(Duration.ofSeconds(10), "zset1"))
                .isEqualTo(ScoredValue.of(Place.crussol, 1.0));
        assertThat(blockingSortedSetOfPlaces.bzmpopMax(Duration.ofSeconds(1), "zset1")).isNull();

        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.bzmpopMax(Duration.ofSeconds(10), 2, "zset1")).containsExactly(
                ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.crussol, 1.0));

        blockingSortedSetOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(blockingSortedSetOfPlaces.bzmpopMax(Duration.ofSeconds(10), 3, "zset1")).containsExactly(
                ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.crussol, 1.0));

        assertThat(blockingSortedSetOfPlaces.bzmpopMax(Duration.ofSeconds(1), 3, "zset1")).isEmpty();
    }

    @Test
    void zrangebylex() {
        populateManyStringEntriesForLex();

        assertThat(blockingSortedSetOfStrings.zrangebylex(key, new Range<>("-", "+"))).hasSize(100);
        assertThat(blockingSortedSetOfStrings.zrangebylex(key, new Range<>("-", "+"), new ZRangeArgs().limit(10, 10)))
                .hasSize(10);

        assertThat(blockingSortedSetOfStrings.zrangebylex(key, Range.unbounded())).hasSize(100);
        assertThat(blockingSortedSetOfStrings.zrangebylex(key, new Range<>("value", "zzz"))).hasSize(100);
        assertThat(blockingSortedSetOfStrings.zrangebylex(key, new Range<>("value98", "value99"))).containsSequence("value98",
                "value99");
        assertThat(blockingSortedSetOfStrings.zrangebylex(key, new Range<>("value99", true, null, false))).hasSize(1);
        assertThat(blockingSortedSetOfStrings.zrangebylex(key, new Range<>("value99", false, null, false))).hasSize(0);
    }

    @Test
    void zremrangebylex() {
        populateManyStringEntriesForLex();
        assertThat(blockingSortedSetOfStrings.zremrangebylex(key, new Range<>("aaa", false, "zzz", true))).isEqualTo(100);

        populateManyStringEntriesForLex();
        assertThat(blockingSortedSetOfStrings.zremrangebylex(key, new Range<>("aaa", "zzz"))).isEqualTo(100);
    }

    @Test
    void zunion() {
        String zset1 = "zset1";
        String zset2 = "zset2";

        assertThat(blockingSortedSetOfPlaces.zadd(zset1, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset1, 2.0, Place.grignan)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 2.0, Place.grignan)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 3.0, Place.suze)).isTrue();

        assertThat(blockingSortedSetOfPlaces.zunion(zset1, zset2)).isEqualTo(List.of(Place.crussol, Place.suze, Place.grignan));
        assertThat(blockingSortedSetOfPlaces.zunionWithScores(zset1, zset2)).isEqualTo(
                List.of(ScoredValue.of(Place.crussol, 2.0), ScoredValue.of(Place.suze, 3.0),
                        ScoredValue.of(Place.grignan, 4.0)));

        assertThat(blockingSortedSetOfPlaces.zunion(new ZAggregateArgs().max(), zset1, zset2))
                .isEqualTo(List.of(Place.crussol, Place.grignan, Place.suze));
        assertThat(blockingSortedSetOfPlaces.zunionWithScores(new ZAggregateArgs().max(), zset1, zset2)).isEqualTo(
                List.of(ScoredValue.of(Place.crussol, 1.0), ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.suze, 3.0)));
    }

    @Test
    void zinter() {
        String zset1 = "zset1";
        String zset2 = "zset2";

        assertThat(blockingSortedSetOfPlaces.zadd(zset1, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset1, 2.0, Place.grignan)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 2.0, Place.grignan)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 3.0, Place.suze)).isTrue();

        assertThat(blockingSortedSetOfPlaces.zinter(zset1, zset2)).isEqualTo(List.of(Place.crussol, Place.grignan));
        assertThat(blockingSortedSetOfPlaces.zinterWithScores(zset1, zset2))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0), ScoredValue.of(Place.grignan, 4.0)));

        assertThatThrownBy(() -> blockingSortedSetOfPlaces.zinter(zset2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> blockingSortedSetOfPlaces.zinter()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zinterWithScores() {
        String zset1 = "zset1";
        String zset2 = "zset2";

        assertThat(blockingSortedSetOfPlaces.zadd(zset1, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset1, 2.0, Place.grignan)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 2.0, Place.grignan)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 3.0, Place.suze)).isTrue();

        assertThat(blockingSortedSetOfPlaces.zinterWithScores(zset1, zset2))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0),
                        ScoredValue.of(Place.grignan, 4.0)));
        assertThat(blockingSortedSetOfPlaces.zinterWithScores(new ZAggregateArgs().max(), zset1, zset2))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0), ScoredValue.of(Place.grignan, 2.0)));
    }

    @Test
    void zinterWithArgs() {
        String zset1 = "zset1";
        String zset2 = "zset2";

        assertThat(blockingSortedSetOfPlaces.zadd(zset1, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset1, 2.0, Place.grignan)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 1.0, Place.crussol)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 2.0, Place.grignan)).isTrue();
        assertThat(blockingSortedSetOfPlaces.zadd(zset2, 3.0, Place.suze)).isTrue();

        assertThat(blockingSortedSetOfPlaces.zinter(new ZAggregateArgs().min(), zset1, zset2))
                .isEqualTo(List.of(Place.crussol, Place.grignan));
        List<ScoredValue<Place>> actual = blockingSortedSetOfPlaces.zinterWithScores(new ZAggregateArgs().max(), zset1,
                zset2);
        assertThat(actual)
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0), ScoredValue.of(Place.grignan, 2.0)));
    }

    private void populateManyStringEntries() {
        for (int i = 0; i < 100; i++) {
            blockingSortedSetOfStrings.zadd(key + 1, i, value + i);
            blockingSortedSetOfStrings.zadd(key, i, value + i);
        }
    }

    private void populateManyStringEntriesForLex() {
        for (int i = 0; i < 100; i++) {
            blockingSortedSetOfStrings.zadd(key + 1, 1.0, value + i);
            blockingSortedSetOfStrings.zadd(key, 1.0, value + i);
        }
    }

    @Test
    void sort() {
        SortedSetCommands<String, String> commands = blockingDs.sortedSet(String.class, String.class);
        commands.zadd(key, Map.of("9", 9.0, "1", 1.0, "3", 3.0, "5", 5.0,
                "8", 8.0, "7", 7.0, "6", 6.0, "2", 2.0, "4", 4.0));

        assertThat(commands.sort(key)).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");

        assertThat(commands.sort(key, new SortArgs().descending())).containsExactly("9", "8", "7", "6", "5", "4", "3", "2",
                "1");

        String k = key + "-alpha";
        Map<String, Double> items = Map.of("a", 1.0, "e", 5.0, "f", 6.0, "b", 2.0);
        commands.zadd(k, items);

        assertThat(commands.sort(k, new SortArgs().alpha())).containsExactly("a", "b", "e", "f");

        commands.sortAndStore(k, "dest1", new SortArgs().alpha());
        commands.sortAndStore(key, "dest2");

        ListCommands<String, String> lists = blockingDs.list(String.class);
        assertThat(lists.lrange("dest1", 0, -1)).containsExactly("a", "b", "e", "f");
        assertThat(lists.lpop("dest2", 100)).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    void zaddWithTypeReference() {

        var set = blockingDs.sortedSet(new TypeReference<Place>() {
            // Empty on purpose
        });
        assertThat(set.zadd(key, 1.0, Place.crussol)).isTrue();
        assertThat(set.zadd(key, 1.0, Place.crussol)).isFalse();

        assertThat(rawZrange(key)).isEqualTo(List.of(Place.crussol));
        assertThat(set.zadd(key, new ScoredValue<>(Place.grignan, 2.0), new ScoredValue<>(Place.suze, 3.0)))
                .isEqualTo(2);
        assertThat(rawZrange(key)).hasSize(3);
    }

    @Test
    void apiReturnsImmutableCollections() {
        var zset = blockingSortedSetOfStrings;
        zset.zadd(key, Map.of("a", 1.0, "b", 2.0, "c", 3.0));
        zset.zadd(key + "-2", Map.of("a", 1.0));
        assertImmutable(zset.zrange(key, 0, -1));
        assertImmutable(zset.zrangeWithScores(key, 0, -1));
        assertImmutable(zset.zrange("missing", 0, -1));
        assertImmutable(zset.zrangebyscore(key, ScoreRange.from(0, 10)));
        assertImmutable(zset.zrangebyscoreWithScores(key, ScoreRange.from(0, 10)));
        assertImmutable(zset.zrangebylex(key, Range.unbounded()));
        // The blocking wrapper of the redis-client module re-collects the scores into a mutable list
        assertImmutable(reactiveSortedSetOfStrings.zmscore(key, "a", "zzz").await().atMost(TIMEOUT));
        assertImmutable(zset.zrandmember(key, 2));
        assertImmutable(zset.zrandmemberWithScores(key, 2));
        assertImmutable(zset.zdiff(key, key + "-2"));
        assertImmutable(zset.zdiffWithScores(key, key + "-2"));
        assertImmutable(zset.zinter(key, key + "-2"));
        assertImmutable(zset.zinterWithScores(key, key + "-2"));
        assertImmutable(zset.zunion(key, key + "-2"));
        assertImmutable(zset.zunionWithScores(key, key + "-2"));
        assertImmutable(zset.sort(key, new SortArgs().alpha()));
        assertImmutable(zset.zscan(key).next());
        assertImmutable(zset.zpopmin(key, 1));
        assertImmutable(zset.zpopmax(key, 1));
        assertImmutable(zset.zmpopMin(1, key));
        assertImmutable(zset.zmpopMax(1, key + "-2"));
        assertImmutable(zset.bzmpopMin(Duration.ofSeconds(1), 1, key));
        assertImmutable(zset.zpopmin("missing", 1));
    }

}
