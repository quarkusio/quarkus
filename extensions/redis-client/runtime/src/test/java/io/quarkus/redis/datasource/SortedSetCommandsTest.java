package io.quarkus.redis.datasource;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.sortedset.Range;
import io.quarkus.redis.datasource.sortedset.ScoreRange;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ZAddArgs;
import io.quarkus.redis.datasource.sortedset.ZAggregateArgs;
import io.quarkus.redis.datasource.sortedset.ZRangeArgs;
import io.quarkus.redis.datasource.sortedset.ZScanCursor;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

@SuppressWarnings("unchecked")
public class SortedSetCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    static String key = "key-zz";
    private SortedSetCommands<String, Place> setOfPlaces;
    private SortedSetCommands<String, String> setOfStrings;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(2));

        setOfPlaces = ds.sortedSet(Place.class);
        setOfStrings = ds.sortedSet(String.class);
    }

    @AfterEach
    public void clear() {
        ds.flushall();
    }

    private void populate() {
        setOfPlaces.zadd(key, Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0));
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(setOfStrings.getDataSource());
        assertThat(ds).isEqualTo(setOfPlaces.getDataSource());
    }

    @Test
    void zadd() {
        assertThat(setOfPlaces.zadd(key, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(key, 1.0, Place.crussol)).isFalse();

        assertThat(setOfPlaces.zrange(key, 0, -1)).isEqualTo(List.of(Place.crussol));
        assertThat(setOfPlaces.zadd(key, new ScoredValue<>(Place.grignan, 2.0), new ScoredValue<>(Place.suze, 3.0)))
                .isEqualTo(2);
        assertThat(setOfPlaces.zrange(key, 0, -1)).isEqualTo(List.of(Place.crussol, Place.grignan, Place.suze));
    }

    @Test
    void zaddScoredValue() {
        assertThat(setOfPlaces.zadd(key, ScoredValue.of(Place.crussol, 1.0))).isEqualTo(1);
        assertThat(setOfPlaces.zadd(key, ScoredValue.of(Place.crussol, 1.0))).isEqualTo(0);

        assertThat(setOfPlaces.zrange(key, 0, -1)).isEqualTo(List.of(Place.crussol));
        assertThat(setOfPlaces.zadd(key, ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.suze, 3.0))).isEqualTo(2);
        assertThat(setOfPlaces.zrange(key, 0, -1)).isEqualTo(List.of(Place.crussol, Place.grignan, Place.suze));
    }

    @Test
    void zaddnx() {
        assertThat(setOfPlaces.zadd(key, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(key, new ZAddArgs().nx(), ScoredValue.of(Place.crussol, 2.0))).isEqualTo(0);
        assertThat(setOfPlaces.zadd(key, new ZAddArgs().nx(), ScoredValue.of(Place.grignan, 2.0))).isEqualTo(1);

        assertThat(setOfPlaces.zadd(key, new ZAddArgs().nx(), Map.of(Place.grignan, 2.0, Place.suze, 3.0))).isEqualTo(1);
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0),
                ScoredValue.of(Place.grignan, 2.0),
                ScoredValue.of(Place.suze, 3.0)));
    }

    @Test
    void zaddxx() {
        assertThat(setOfPlaces.zadd(key, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(key, new ZAddArgs().xx(), 2.0, Place.crussol)).isFalse();

        assertThat(setOfPlaces.zadd(key, new ZAddArgs().xx(), 2.0, Place.grignan)).isFalse();

        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0)));
    }

    @Test
    @RequiresRedis6OrHigher
    void zaddch() {
        assertThat(setOfPlaces.zadd(key, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(key, new ZAddArgs().ch().xx(), 2.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(key, new ZAddArgs().ch(), 2.0, Place.grignan)).isTrue();

        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0), ScoredValue.of(Place.grignan, 2.0)));
    }

    @Test
    @RequiresRedis6OrHigher
    void zaddincr() {
        assertThat(setOfPlaces.zadd(key, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zaddincr(key, 2.0, Place.crussol)).isEqualTo(3.0);
        assertThat(setOfPlaces.zaddincr(key, 2.0, Place.grignan)).isEqualTo(2.0);
        assertThat(setOfPlaces.zaddincr("missing", 2.0, Place.grignan)).isEqualTo(2.0);

        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1))
                .isEqualTo(List.of(ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.crussol, 3.0)));
    }

    @Test
    void zaddincrnx() {
        assertThat(setOfPlaces.zaddincr(key, new ZAddArgs().nx(), 2.0, Place.crussol)).hasValue(2.0);
        assertThat(setOfPlaces.zaddincr(key, new ZAddArgs().nx(), 2.0, Place.crussol)).isEmpty();
    }

    @Test
    void zaddincrxx() {
        assertThat(setOfPlaces.zaddincr(key, new ZAddArgs().xx(), 2.0, Place.crussol)).isEmpty();
        assertThat(setOfPlaces.zaddincr(key, new ZAddArgs().nx(), 2.0, Place.crussol)).hasValue(2.0);
        assertThat(setOfPlaces.zaddincr(key, new ZAddArgs().xx(), 2.0, Place.crussol)).hasValue(4.0);
    }

    @Test
    @RequiresRedis6OrHigher
    void zaddgt() {
        assertThat(setOfPlaces.zadd(key, 1.0, Place.crussol)).isTrue();
        // new score less than the current score
        assertThat(setOfPlaces.zadd(key, new ZAddArgs().gt(), 0.0, Place.crussol)).isFalse();
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0)));

        // new score greater than the current score
        assertThat(setOfPlaces.zadd(key, new ZAddArgs().gt(), 2.0, Place.crussol)).isFalse();
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0)));

        // add new element
        assertThat(setOfPlaces.zadd(key, new ZAddArgs().gt(), 0.0, Place.grignan)).isTrue();
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(
                List.of(ScoredValue.of(Place.grignan, 0.0), ScoredValue.of(Place.crussol, 2.0)));
    }

    @Test
    @RequiresRedis6OrHigher
    void zaddlt() {
        assertThat(setOfPlaces.zadd(key, 2.0, Place.crussol)).isTrue();
        // new score greater than the current score
        assertThat(setOfPlaces.zadd(key, new ZAddArgs().lt(), 3.0, Place.crussol)).isFalse();
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0)));

        // new score less than the current score
        assertThat(setOfPlaces.zadd(key, new ZAddArgs().lt(), 1.0, Place.crussol)).isFalse();
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0)));

        // add new element
        assertThat(setOfPlaces.zadd(key, new ZAddArgs().lt(), 0.0, Place.grignan)).isTrue();
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(
                List.of(ScoredValue.of(Place.grignan, 0.0), ScoredValue.of(Place.crussol, 1.0)));
    }

    @Test
    void zcard() {
        assertThat(setOfPlaces.zcard(key)).isEqualTo(0);
        assertThat(setOfPlaces.zadd(key, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zcard(key)).isEqualTo(1);
    }

    @Test
    void zcount() {
        assertThat(setOfPlaces.zcount(key, ScoreRange.from(0, 0))).isEqualTo(0);

        assertThat(setOfPlaces.zadd(key, new ScoredValue<>(Place.crussol, 1.0), new ScoredValue<>(Place.grignan, 2.0),
                new ScoredValue<>(Place.suze, 2.1)))
                .isEqualTo(3);

        assertThat(setOfPlaces.zcount(key, ScoreRange.from(1.0, 3.0))).isEqualTo(3);
        assertThat(setOfPlaces.zcount(key, ScoreRange.from(1.0, 2.0))).isEqualTo(2);
        assertThat(setOfPlaces.zcount(key, ScoreRange.from(NEGATIVE_INFINITY, POSITIVE_INFINITY))).isEqualTo(3);

        assertThat(setOfPlaces.zcount(key, new ScoreRange<>(1.0, false, 3.0, true))).isEqualTo(2);
        assertThat(setOfPlaces.zcount(key, ScoreRange.unbounded())).isEqualTo(3);
    }

    @Test
    @RequiresRedis6OrHigher
    void zdiff() {
        String zset1 = "zset1";
        String zset2 = "zset2";

        assertThat(setOfPlaces.zadd(zset1, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(zset1, 2.0, Place.grignan)).isTrue();
        assertThat(setOfPlaces.zadd(zset1, 3.0, Place.suze)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 2.0, Place.grignan)).isTrue();

        assertThat(setOfPlaces.zdiff(zset1, zset2)).isEqualTo(List.of(Place.suze));
        assertThat(setOfPlaces.zdiffWithScores(zset1, zset2)).isEqualTo(List.of(ScoredValue.of(Place.suze, 3.0)));

        assertThatThrownBy(() -> setOfPlaces.zdiff(zset2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> setOfPlaces.zdiff()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @RequiresRedis6OrHigher
    void zdiffstore() {
        String zset1 = "zset1";
        String zset2 = "zset2";

        assertThat(setOfPlaces.zadd(zset1, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(zset1, 2.0, Place.grignan)).isTrue();
        assertThat(setOfPlaces.zadd(zset1, 3.0, Place.suze)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 2.0, Place.grignan)).isTrue();

        assertThat(setOfPlaces.zdiffstore("out", zset1, zset2)).isEqualTo(1);
        assertThat(setOfPlaces.zrangeWithScores("out", 0, -1)).isEqualTo(List.of(ScoredValue.of(Place.suze, 3.0)));
    }

    @Test
    void zincrby() {
        assertThat(setOfPlaces.zincrby(key, 0.0, Place.crussol)).isEqualTo(0.0, offset(0.1));
        assertThat(setOfPlaces.zincrby(key, 1.1, Place.crussol)).isEqualTo(1.1, offset(0.1));
        assertThat(setOfPlaces.zscore(key, Place.crussol)).hasValueCloseTo(1.1, offset(0.1));
        assertThat(setOfPlaces.zincrby(key, -1.2, Place.crussol)).isEqualTo(-0.1, offset(0.1));
    }

    @Test
    @RequiresRedis7OrHigher
    void zintercard() {
        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        setOfPlaces.zadd("zset2", Map.of(Place.crussol, 2.0, Place.grignan, 1.0));
        assertThat(setOfPlaces.zintercard("zset1", "zset2")).isEqualTo(2);
        assertThat(setOfPlaces.zintercard(1, "zset1", "zset2")).isEqualTo(1);
    }

    @Test
    void zinterstore() {
        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        setOfPlaces.zadd("zset2", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));
        assertThat(setOfPlaces.zinterstore(key, "zset1", "zset2")).isEqualTo(2);
        assertThat(setOfPlaces.zrange(key, 0, -1)).isEqualTo(List.of(Place.crussol, Place.grignan));
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 3.0), ScoredValue.of(Place.grignan, 5.0)));
    }

    @Test
    @RequiresRedis6OrHigher
    void zinterstoreWithArgs() {
        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        setOfPlaces.zadd("zset2", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));
        assertThat(setOfPlaces.zinterstore(key, new ZAggregateArgs().max(), "zset1", "zset2")).isEqualTo(2);
        assertThat(setOfPlaces.zrange(key, 0, -1)).isEqualTo(List.of(Place.crussol, Place.grignan));
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1, new ZRangeArgs().rev()))
                .isEqualTo(List.of(ScoredValue.of(Place.grignan, 3.0), ScoredValue.of(Place.crussol, 2.0)));
    }

    @Test
    void bzpopmin() {
        setOfPlaces.zadd("zset", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));
        assertThat(setOfPlaces.bzpopmin(Duration.ofSeconds(1), "zset"))
                .isEqualTo(KeyValue.of("zset", ScoredValue.of(Place.crussol, 2.0)));
        assertThat(setOfPlaces.bzpopmin(Duration.ofMillis(1000), "zset2")).isNull();
    }

    @Test
    void bzpopmax() {
        setOfPlaces.zadd("zset", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));
        assertThat(setOfPlaces.bzpopmax(Duration.ofSeconds(1), "zset"))
                .isEqualTo(KeyValue.of("zset", ScoredValue.of(Place.suze, 4.0)));
        assertThat(setOfPlaces.bzpopmax(Duration.ofSeconds(1), "zset2")).isNull();
    }

    @Test
    void zpopmin() {
        setOfPlaces.zadd("zset", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));

        assertThat(setOfPlaces.zpopmin("zset")).isEqualTo(ScoredValue.of(Place.crussol, 2.0));
        assertThat(setOfPlaces.zpopmin("zset", 2)).containsExactly(ScoredValue.of(Place.grignan, 3.0),
                ScoredValue.of(Place.suze, 4.0));
        assertThat(setOfPlaces.zpopmin("foo")).isEqualTo(ScoredValue.empty());
    }

    @Test
    void zpopmax() {
        setOfPlaces.zadd("zset", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));

        assertThat(setOfPlaces.zpopmax("zset")).isEqualTo(ScoredValue.of(Place.suze, 4.0));
        assertThat(setOfPlaces.zpopmax("zset", 2)).containsExactlyInAnyOrder(ScoredValue.of(Place.crussol, 2.0),
                ScoredValue.of(Place.grignan, 3.0));
        assertThat(setOfPlaces.zpopmax("foo")).isEqualTo(ScoredValue.empty());
    }

    @Test
    @RequiresRedis6OrHigher
    void zrandmember() {
        setOfPlaces.zadd("zset", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));
        assertThat(setOfPlaces.zrandmember("zset")).isIn(Place.crussol, Place.grignan, Place.suze);
        assertThat(setOfPlaces.zrandmember("zset", 2)).hasSize(2).containsAnyOf(Place.crussol, Place.grignan, Place.suze);
        assertThat(setOfPlaces.zrandmemberWithScores("zset")).isIn(ScoredValue.of(Place.crussol, 2.0),
                ScoredValue.of(Place.grignan, 3.0), ScoredValue.of(Place.suze, 4.0));
        assertThat(setOfPlaces.zrandmemberWithScores("zset", 2)).hasSize(2).containsAnyOf(ScoredValue.of(Place.crussol, 2.0),
                ScoredValue.of(Place.grignan, 3.0), ScoredValue.of(Place.suze, 4.0));
    }

    @Test
    void zrange() {
        populate();
        assertThat(setOfPlaces.zrange(key, 0, -1)).isEqualTo(List.of(Place.crussol, Place.grignan, Place.suze));
    }

    @Test
    void zrangeWithScores() {
        populate();
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(
                List.of(ScoredValue.of(Place.crussol, 1.0), ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.suze, 3.0)));
    }

    @Test
    @RequiresRedis6OrHigher
    void zrangebyscore() {
        setOfPlaces.zadd(key, Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));

        assertThat(setOfPlaces.zrangebyscore(key, new ScoreRange<>(2.0, 3.0))).isEqualTo(List.of(Place.grignan, Place.suze));
        assertThat(setOfPlaces.zrangebyscore(key, new ScoreRange<>(1.0, false, 4.0, false)))
                .isEqualTo(List.of(Place.grignan, Place.suze));
        assertThat(setOfPlaces.zrangebyscore(key, new ScoreRange<>(NEGATIVE_INFINITY, POSITIVE_INFINITY)))
                .isEqualTo(List.of(Place.crussol, Place.grignan, Place.suze, Place.adhemar));
        assertThat(setOfPlaces.zrangebyscore(key, ScoreRange.unbounded()))
                .isEqualTo(List.of(Place.crussol, Place.grignan, Place.suze, Place.adhemar));
        assertThat(setOfPlaces.zrangebyscore(key, new ScoreRange<>(0.0, 4.0), new ZRangeArgs().limit(1, 3)))
                .isEqualTo(List.of(Place.grignan, Place.suze, Place.adhemar));
        assertThat(setOfPlaces.zrangebyscore(key, ScoreRange.unbounded(), new ZRangeArgs().limit(2, 2)))
                .isEqualTo(List.of(Place.suze, Place.adhemar));
    }

    @Test
    @RequiresRedis6OrHigher
    void zrangebyscoreWithScores() {
        setOfPlaces.zadd(key, Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));

        assertThat(setOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(2.0, 3.0)))
                .isEqualTo(List.of(ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.suze, 3.0)));
        assertThat(setOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(1.0, false, 3.0, true)))
                .isEqualTo(List.of(ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.suze, 3.0)));
        assertThat(setOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(NEGATIVE_INFINITY, POSITIVE_INFINITY)))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0), ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.suze, 3.0),
                        ScoredValue.of(Place.adhemar, 4.0)));
        assertThat(setOfPlaces.zrangebyscoreWithScores(key, ScoreRange.unbounded()))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0), ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.suze, 3.0),
                        ScoredValue.of(Place.adhemar, 4.0)));
        assertThat(setOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(0.0, 4.0), new ZRangeArgs().limit(1, 3)))
                .isEqualTo(List.of(ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.suze, 3.0),
                        ScoredValue.of(Place.adhemar, 4.0)));
        assertThat(setOfPlaces.zrangebyscoreWithScores(key, ScoreRange.unbounded(), new ZRangeArgs().limit(2, 2)))
                .isEqualTo(List.of(ScoredValue.of(Place.suze, 3.0), ScoredValue.of(Place.adhemar, 4.0)));
    }

    @Test
    @RequiresRedis6OrHigher
    void zrangebyscoreWithScoresInfinity() {
        setOfPlaces.zadd(key, Map.of(Place.crussol, Double.POSITIVE_INFINITY, Place.grignan, Double.NEGATIVE_INFINITY));
        assertThat(setOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(null, null))).hasSize(2);
        assertThat(setOfPlaces.zrangebyscoreWithScores(key, ScoreRange.unbounded())).hasSize(2);
    }

    @Test
    @RequiresRedis6OrHigher
    void zrangestorebylex() {
        setOfStrings.zadd(key, Map.of("a", 1.0, "b", 1.0, "c", 1.0, "d", 1.0));
        assertThat(setOfStrings.zrangestorebylex("key1", key, new Range<>("b", "d"), new ZRangeArgs().limit(0, 4)))
                .isEqualTo(3);
        assertThat(setOfStrings.zrange("key1", 0, 1)).isEqualTo(List.of("b", "c"));

        assertThat(setOfStrings.zrangestorebylex("key1", key, new Range<>("b", "d"))).isEqualTo(3);
        assertThat(setOfStrings.zrange("key1", 0, 1)).isEqualTo(List.of("b", "c"));
    }

    @Test
    @RequiresRedis6OrHigher
    void zrangestorebyscore() {
        setOfPlaces.zadd(key, Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));
        assertThat(setOfPlaces.zrangestorebyscore("key1", key, new ScoreRange<>(0.0, 2.0),
                new ZRangeArgs().limit(0, 2))).isEqualTo(2);
        assertThat(setOfPlaces.zrange("key1", 0, 2)).isEqualTo(List.of(Place.crussol, Place.grignan));

        assertThat(setOfPlaces.zrangestorebyscore("key1", key, new ScoreRange<>(0.0, 2.0))).isEqualTo(2);
        assertThat(setOfPlaces.zrange("key1", 0, 2)).isEqualTo(List.of(Place.crussol, Place.grignan));
    }

    @Test
    @RequiresRedis6OrHigher
    void zrangestore() {
        setOfPlaces.zadd(key, Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));
        assertThat(setOfPlaces.zrangestore("key1", key, 0, -1)).isEqualTo(4);
        assertThat(setOfPlaces.zrange("key1", 0, -1))
                .isEqualTo(List.of(Place.crussol, Place.grignan, Place.suze, Place.adhemar));

        assertThat(setOfPlaces.zrangestore("key1", key, 2, 4, new ZRangeArgs().rev())).isEqualTo(2);
        assertThat(setOfPlaces.zrange("key1", 0, -1)).isEqualTo(List.of(Place.crussol, Place.grignan));
    }

    @Test
    void zrank() {
        assertThat(setOfPlaces.zrank(key, Place.crussol)).isEmpty();
        populate();
        assertThat(setOfPlaces.zrank(key, Place.crussol)).hasValue(0);
        assertThat(setOfPlaces.zrank(key, Place.suze)).hasValue(2);
    }

    @Test
    void zrem() {
        assertThat(setOfPlaces.zrem(key, Place.crussol)).isEqualTo(0);
        populate();
        assertThat(setOfPlaces.zrem(key, Place.grignan)).isEqualTo(1);
        assertThat(setOfPlaces.zrange(key, 0, -1)).isEqualTo(List.of(Place.crussol, Place.suze));
        assertThat(setOfPlaces.zrem(key, Place.crussol, Place.suze)).isEqualTo(2);
        assertThat(setOfPlaces.zrange(key, 0, -1)).isEqualTo(List.of());
    }

    @Test
    void zremrangebyscore() {
        populate();
        assertThat(setOfPlaces.zremrangebyscore(key, new ScoreRange<>(1.0, 2.0))).isEqualTo(2);
        assertThat(setOfPlaces.zrange(key, 0, -1)).isEqualTo(List.of(Place.suze));

        populate();
        assertThat(setOfPlaces.zremrangebyscore(key, new ScoreRange<>(1.0, false, 3.0, false)))
                .isEqualTo(1);
        assertThat(setOfPlaces.zrange(key, 0, -1)).isEqualTo(List.of(Place.crussol, Place.suze));
    }

    @Test
    void zremrangebyrank() {
        setOfPlaces.zadd(key, Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));
        assertThat(setOfPlaces.zremrangebyrank(key, 1, 2)).isEqualTo(2);
        assertThat(setOfPlaces.zrange(key, 0, -1)).isEqualTo(List.of(Place.crussol, Place.adhemar));

        setOfPlaces.zadd(key, Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));
        assertThat(setOfPlaces.zremrangebyrank(key, 0, -1)).isEqualTo(4);
        assertThat(setOfPlaces.zcard(key)).isEqualTo(0);
    }

    @Test
    @RequiresRedis6OrHigher
    void zrevrange() {
        populate();
        assertThat(setOfPlaces.zrange(key, 0, -1, new ZRangeArgs().rev()))
                .isEqualTo(List.of(Place.suze, Place.grignan, Place.crussol));
    }

    @Test
    @RequiresRedis6OrHigher
    void zrevrangeWithScoreEmpty() {
        assertThat(ds.sortedSet(String.class).zrangeWithScores("top-products", 0, 2, new ZRangeArgs().rev())).isEmpty();
        assertThat(ds.sortedSet(String.class).zrangeWithScores("missing", 0, 2)).isEmpty();
    }

    @Test
    @RequiresRedis6OrHigher
    void zrevrangeWithScores() {
        populate();
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1, new ZRangeArgs().rev()))
                .isEqualTo(List.of(ScoredValue.of(Place.suze, 3.0), ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.crussol, 1.0)));
    }

    @Test
    @RequiresRedis6OrHigher
    void zrevrangebylex() {
        populateManyStringEntriesForLex();
        assertThat(setOfStrings.zrangebylex(key, Range.unbounded(), new ZRangeArgs().rev())).hasSize(100);
        assertThat(setOfStrings.zrangebylex(key, new Range<>("value", "zzz"), new ZRangeArgs().rev())).hasSize(100);
        assertThat(setOfStrings.zrangebylex(key, new Range<>("value98", true, "value99", true),
                new ZRangeArgs().rev())).containsSequence("value99", "value98");
        assertThat(setOfStrings.zrangebylex(key, new Range<>("value99", true, null, true), new ZRangeArgs().rev())).hasSize(1);
        assertThat(setOfStrings.zrangebylex(key, new Range<>("value99", false, null, false), new ZRangeArgs().rev()))
                .hasSize(0);
    }

    @Test
    @RequiresRedis6OrHigher
    void zrevrangebyscore() {
        setOfPlaces.zadd(key, Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));
        ZRangeArgs rev = new ZRangeArgs().rev();
        assertThat(setOfPlaces.zrangebyscore(key, new ScoreRange<>(3.0, 2.0), rev))
                .isEqualTo(List.of(Place.suze, Place.grignan));
        assertThat(setOfPlaces.zrangebyscore(key, new ScoreRange<>(4.0, false, 1.0, false), rev))
                .isEqualTo(List.of(Place.suze, Place.grignan));
        assertThat(setOfPlaces.zrangebyscore(key, new ScoreRange<>(POSITIVE_INFINITY, NEGATIVE_INFINITY), rev))
                .isEqualTo(List.of(Place.adhemar, Place.suze, Place.grignan, Place.crussol));
        assertThat(setOfPlaces.zrangebyscore(key, ScoreRange.unbounded(), rev))
                .isEqualTo(List.of(Place.adhemar, Place.suze, Place.grignan, Place.crussol));
        assertThat(setOfPlaces.zrangebyscore(key, new ScoreRange<>(4.0, 0.0), new ZRangeArgs().rev().limit(1, 3)))
                .isEqualTo(List.of(Place.suze, Place.grignan, Place.crussol));
        assertThat(setOfPlaces.zrangebyscore(key, ScoreRange.unbounded(), new ZRangeArgs().rev().limit(2, 2)))
                .isEqualTo(List.of(Place.grignan, Place.crussol));
    }

    @Test
    @RequiresRedis6OrHigher
    void zrevrangebyscoreWithScores() {
        setOfPlaces.zadd(key, Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));
        ZRangeArgs rev = new ZRangeArgs().rev();
        assertThat(setOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(3.0, 2.0), rev))
                .isEqualTo(List.of(ScoredValue.of(Place.suze, 3.0), ScoredValue.of(Place.grignan, 2.0)));
        assertThat(setOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(4.0, false, 1.0, false), rev))
                .isEqualTo(List.of(ScoredValue.of(Place.suze, 3.0), ScoredValue.of(Place.grignan, 2.0)));
        assertThat(setOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(POSITIVE_INFINITY, NEGATIVE_INFINITY), rev))
                .isEqualTo(List.of(ScoredValue.of(Place.adhemar, 4.0), ScoredValue.of(Place.suze, 3.0),
                        ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.crussol, 1.0)));
        assertThat(setOfPlaces.zrangebyscoreWithScores(key, ScoreRange.unbounded(), rev))
                .isEqualTo(List.of(ScoredValue.of(Place.adhemar, 4.0), ScoredValue.of(Place.suze, 3.0),
                        ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.crussol, 1.0)));
        assertThat(setOfPlaces.zrangebyscoreWithScores(key, new ScoreRange<>(4.0, 0.0), new ZRangeArgs().rev().limit(1, 3)))
                .isEqualTo(List.of(ScoredValue.of(Place.suze, 3.0), ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.crussol, 1.0)));
        assertThat(setOfPlaces.zrangebyscoreWithScores(key, ScoreRange.unbounded(), new ZRangeArgs().rev().limit(2, 2)))
                .isEqualTo(List.of(ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.crussol, 1.0)));
    }

    @Test
    void zrevrank() {
        assertThat(setOfPlaces.zrevrank(key, Place.crussol)).isEmpty();
        populate();
        assertThat(setOfPlaces.zrevrank(key, Place.suze)).hasValue(0);
        assertThat(setOfPlaces.zrevrank(key, Place.crussol)).hasValue(2);
    }

    @Test
    @RequiresRedis6OrHigher
    void zrevrangestorebylex() {
        setOfStrings.zadd(key, Map.of("a", 1.0, "b", 2.0, "c", 3.0, "d", 4.0));
        assertThat(setOfStrings.zrangestorebylex("key1", key, new Range<>("c", "-"),
                new ZRangeArgs().rev().limit(0, 4))).isEqualTo(3);
        assertThat(setOfStrings.zrange("key1", 0, 2)).isEqualTo(List.of("a", "b", "c"));
    }

    @Test
    @RequiresRedis6OrHigher
    void zrevrangestorebyscore() {
        setOfPlaces.zadd(key, Map.of(Place.crussol, 1.0, Place.grignan, 2.0, Place.suze, 3.0, Place.adhemar, 4.0));
        assertThat(
                setOfPlaces.zrangestorebyscore("key1", key, new ScoreRange<>(2.0, true, 1.0, false),
                        new ZRangeArgs().rev().limit(0, 2)))
                .isEqualTo(1);
        assertThat(setOfPlaces.zrange("key1", 0, 2)).isEqualTo(List.of(Place.grignan));
    }

    @Test
    void zscore() {
        assertThat(setOfPlaces.zscore(key, Place.crussol)).isEmpty();
        setOfPlaces.zadd(key, 1.0, Place.crussol);
        assertThat(setOfPlaces.zscore(key, Place.crussol)).hasValue(1.0);
    }

    @Test
    void zunionstore() {
        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        setOfPlaces.zadd("zset2", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));

        assertThat(setOfPlaces.zunionstore(key, "zset1", "zset2")).isEqualTo(3);
        assertThat(setOfPlaces.zunionstore(key + "2", new ZAggregateArgs().max(), "zset1", "zset2")).isEqualTo(3);

        assertThat(setOfPlaces.zrange(key, 0, -1)).isEqualTo(List.of(Place.crussol, Place.suze, Place.grignan));
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 3.0), new ScoredValue<>(Place.suze, 4.0),
                        ScoredValue.of(Place.grignan, 5.0)));

        assertThat(setOfPlaces.zunionstore(key, new ZAggregateArgs().weights(2.0, 3.0), "zset1", "zset2")).isEqualTo(3);
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(
                List.of(new ScoredValue<>(Place.crussol, 8.0), new ScoredValue<>(Place.suze, 12.0),
                        new ScoredValue<>(Place.grignan, 13.0)));

        assertThat(setOfPlaces.zunionstore(key, new ZAggregateArgs().weights(2.0, 3.0).sum(), "zset1", "zset2")).isEqualTo(3);
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(
                List.of(new ScoredValue<>(Place.crussol, 8.0), new ScoredValue<>(Place.suze, 12.0),
                        new ScoredValue<>(Place.grignan, 13.0)));

        assertThat(setOfPlaces.zunionstore(key, new ZAggregateArgs().min(), "zset1", "zset2")).isEqualTo(3);
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(
                List.of(ScoredValue.of(Place.crussol, 1.0), new ScoredValue<>(Place.grignan, 2.0),
                        new ScoredValue<>(Place.suze, 4.0)));

        assertThat(setOfPlaces.zunionstore(key, new ZAggregateArgs().weights(2.0, 3.0).min(), "zset1", "zset2")).isEqualTo(3);
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(
                List.of(ScoredValue.of(Place.crussol, 2.0), new ScoredValue<>(Place.grignan, 4.0),
                        new ScoredValue<>(Place.suze, 12.0)));

        assertThat(setOfPlaces.zunionstore(key, new ZAggregateArgs().weights(2.0, 3.0).max(), "zset1", "zset2")).isEqualTo(3);
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1)).isEqualTo(
                List.of(new ScoredValue<>(Place.crussol, 6.0), new ScoredValue<>(Place.grignan, 9.0),
                        new ScoredValue<>(Place.suze, 12.0)));
    }

    @Test
    void zinterstoreArgs() {
        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        setOfPlaces.zadd("zset2", Map.of(Place.crussol, 2.0, Place.grignan, 3.0, Place.suze, 4.0));

        assertThat(setOfPlaces.zinterstore(key, new ZAggregateArgs().sum(), "zset1", "zset2")).isEqualTo(2);
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 3.0), ScoredValue.of(Place.grignan, 5.0)));

        assertThat(setOfPlaces.zinterstore(key, new ZAggregateArgs().min(), "zset1", "zset2")).isEqualTo(2);
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0), ScoredValue.of(Place.grignan, 2.0)));

        assertThat(setOfPlaces.zinterstore(key, new ZAggregateArgs().max(), "zset1", "zset2")).isEqualTo(2);
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0), new ScoredValue<>(Place.grignan, 3.0)));

        assertThat(setOfPlaces.zinterstore(key, new ZAggregateArgs().weights(2, 3), "zset1", "zset2")).isEqualTo(2);
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1))
                .isEqualTo(List.of(new ScoredValue<>(Place.crussol, 8.0), new ScoredValue<>(Place.grignan, 13.0)));

        assertThat(setOfPlaces.zinterstore(key, new ZAggregateArgs().weights(2, 3).sum(), "zset1", "zset2")).isEqualTo(2);
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1))
                .isEqualTo(List.of(new ScoredValue<>(Place.crussol, 8.0), new ScoredValue<>(Place.grignan, 13.0)));

        assertThat(setOfPlaces.zinterstore(key, new ZAggregateArgs().weights(2, 3).min(), "zset1", "zset2")).isEqualTo(2);
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0), new ScoredValue<>(Place.grignan, 4.0)));

        assertThat(setOfPlaces.zinterstore(key, new ZAggregateArgs().weights(2, 3).max(), "zset1", "zset2")).isEqualTo(2);
        assertThat(setOfPlaces.zrangeWithScores(key, 0, -1))
                .isEqualTo(List.of(new ScoredValue<>(Place.crussol, 6.0), new ScoredValue<>(Place.grignan, 9.0)));
    }

    @Test
    void zsscan() {
        setOfPlaces.zadd(key, 1.0, Place.crussol);
        ZScanCursor<Place> cursor = setOfPlaces.zscan(key);
        assertThat(cursor.cursorId()).isEqualTo(Cursor.INITIAL_CURSOR_ID);
        assertThat(cursor.hasNext()).isTrue();
        List<ScoredValue<Place>> values = cursor.next();
        assertThat(cursor.cursorId()).isEqualTo(0);
        assertThat(cursor.hasNext()).isFalse();
        assertThat(values.get(0)).isEqualTo(new ScoredValue<>(Place.crussol, 1.0));
    }

    @Test
    void zsscanEmpty() {
        ZScanCursor<Place> cursor = setOfPlaces.zscan(key);
        assertThat(cursor.cursorId()).isEqualTo(Cursor.INITIAL_CURSOR_ID);
        assertThat(cursor.hasNext()).isTrue();
        List<ScoredValue<Place>> values = cursor.next();
        assertThat(cursor.cursorId()).isEqualTo(0);
        assertThat(cursor.hasNext()).isFalse();
        assertThat(values).isEmpty();
    }

    @Test
    void zsscanEmptyAsIterable() {
        ZScanCursor<Place> cursor = setOfPlaces.zscan(key);
        assertThat(cursor.cursorId()).isEqualTo(Cursor.INITIAL_CURSOR_ID);
        assertThat(cursor.hasNext()).isTrue();
        Iterable<ScoredValue<Place>> iterable = cursor.toIterable();
        assertThat(iterable).isEmpty();
        assertThat(cursor.cursorId()).isEqualTo(0);
        assertThat(cursor.hasNext()).isFalse();
    }

    @Test
    void zsscanWithCursorAndArgs() {
        setOfPlaces.zadd(key, 1.0, Place.crussol);
        setOfPlaces.zadd(key, 2.0, Place.grignan);
        setOfPlaces.zadd(key, 3.0, Place.adhemar);
        ZScanCursor<Place> cursor = setOfPlaces.zscan(key, new ScanArgs().count(2));
        assertThat(cursor.cursorId()).isEqualTo(Cursor.INITIAL_CURSOR_ID);
        assertThat(cursor.hasNext()).isTrue();
        List<ScoredValue<Place>> values = cursor.next();
        assertThat(cursor.cursorId()).isEqualTo(0);
        assertThat(cursor.hasNext()).isFalse();
        assertThat(values.get(0)).isEqualTo(new ScoredValue<>(Place.crussol, 1.0));
    }

    @Test
    void zscanMultiple() {
        populateManyStringEntries();

        ZScanCursor<String> cursor = setOfStrings.zscan(key, new ScanArgs().count(5));
        assertThat(cursor).isNotNull();
        assertThat(cursor.hasNext()).isTrue();

        List<ScoredValue<String>> values = new ArrayList<>();
        while (cursor.hasNext()) {
            values.addAll(cursor.next());
        }
        assertThat(cursor.cursorId()).isEqualTo(0L);
        assertThat(cursor.hasNext()).isFalse();
        assertThat(values).hasSize(100);
    }

    @Test
    void zscanMultipleAsITerable() {
        populateManyStringEntries();

        ZScanCursor<String> cursor = setOfStrings.zscan(key, new ScanArgs().count(5));
        assertThat(cursor).isNotNull();
        assertThat(cursor.hasNext()).isTrue();

        List<ScoredValue<String>> values = new ArrayList<>();
        for (ScoredValue<String> scoredValue : cursor.toIterable()) {
            values.add(scoredValue);
        }
        assertThat(cursor.cursorId()).isEqualTo(0L);
        assertThat(cursor.hasNext()).isFalse();
        assertThat(values).hasSize(100);
    }

    @Test
    void zscanMatch() {
        populateManyStringEntries();

        ZScanCursor<String> cursor = setOfStrings.zscan(key, new ScanArgs().count(10).match("val*"));
        List<ScoredValue<String>> values = new ArrayList<>();
        while (cursor.hasNext()) {
            values.addAll(cursor.next());
        }
        assertThat(cursor.cursorId()).isEqualTo(0L);
        assertThat(cursor.hasNext()).isFalse();
        assertThat(values).hasSize(100);
    }

    @Test
    void zlexcount() {
        populateManyStringEntriesForLex();
        assertThat(setOfStrings.zlexcount(key, new Range<>("-", "+"))).isEqualTo(100);
        assertThat(setOfStrings.zlexcount(key, new Range<>("value", "zzz"))).isEqualTo(100);

        assertThat(setOfStrings.zlexcount(key, Range.unbounded())).isEqualTo(100);
        assertThat(setOfStrings.zlexcount(key, new Range<>("value99", true, null, false))).isEqualTo(1);
        assertThat(setOfStrings.zlexcount(key, new Range<>("value99", false, null, false))).isEqualTo(0);
    }

    @Test
    @RequiresRedis6OrHigher
    public void zmscore() {
        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(setOfPlaces.zmscore("zset1", Place.crussol, Place.suze, Place.grignan))
                .isEqualTo(List.of(OptionalDouble.of(1.0), OptionalDouble.empty(), OptionalDouble.of(2.0)));
    }

    @Test
    @RequiresRedis7OrHigher
    public void zmpopMin() {
        assertThat(setOfPlaces.zmpopMin("zset1")).isEqualTo(null);

        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(setOfPlaces.zmpopMin("zset1")).isEqualTo(ScoredValue.of(Place.crussol, 1.0));
        assertThat(setOfPlaces.zmpopMin("zset1")).isEqualTo(ScoredValue.of(Place.grignan, 2.0));
        assertThat(setOfPlaces.zmpopMin("zset1")).isNull();

        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(setOfPlaces.zmpopMin(2, "zset1")).containsExactly(ScoredValue.of(Place.crussol, 1.0),
                ScoredValue.of(Place.grignan, 2.0));

        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(setOfPlaces.zmpopMin(3, "zset1")).containsExactly(ScoredValue.of(Place.crussol, 1.0),
                ScoredValue.of(Place.grignan, 2.0));

        assertThat(setOfPlaces.bzmpopMax(Duration.ofSeconds(1), 3, "zset1")).isEmpty();
    }

    @Test
    @RequiresRedis7OrHigher
    public void zmpopMax() {
        assertThat(setOfPlaces.zmpopMax("zset1")).isEqualTo(null);

        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(setOfPlaces.zmpopMax("zset1")).isEqualTo(ScoredValue.of(Place.grignan, 2.0));
        assertThat(setOfPlaces.zmpopMax("zset1")).isEqualTo(ScoredValue.of(Place.crussol, 1.0));
        assertThat(setOfPlaces.zmpopMax("zset1")).isNull();

        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(setOfPlaces.zmpopMax(2, "zset1")).containsExactly(
                ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.crussol, 1.0));

        setOfPlaces.zadd("zset1", Map.of(Place.grignan, 2.0, Place.crussol, 1.0));
        assertThat(setOfPlaces.zmpopMax(3, "zset1")).containsExactly(
                ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.crussol, 1.0));

        assertThat(setOfPlaces.bzmpopMax(Duration.ofSeconds(1), 3, "zset1")).isEmpty();
    }

    @Test
    @RequiresRedis7OrHigher
    public void bzmpopMin() {
        assertThat(setOfPlaces.bzmpopMin(Duration.ofSeconds(1), "zset1")).isEqualTo(null);

        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(setOfPlaces.bzmpopMin(Duration.ofSeconds(10), "zset1")).isEqualTo(ScoredValue.of(Place.crussol, 1.0));
        assertThat(setOfPlaces.bzmpopMin(Duration.ofSeconds(10), "zset1")).isEqualTo(ScoredValue.of(Place.grignan, 2.0));
        assertThat(setOfPlaces.bzmpopMin(Duration.ofSeconds(1), "zset1")).isNull();

        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(setOfPlaces.bzmpopMin(Duration.ofSeconds(10), 2, "zset1")).containsExactly(
                ScoredValue.of(Place.crussol, 1.0),
                ScoredValue.of(Place.grignan, 2.0));

        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(setOfPlaces.bzmpopMin(Duration.ofSeconds(10), 3, "zset1")).containsExactly(
                ScoredValue.of(Place.crussol, 1.0),
                ScoredValue.of(Place.grignan, 2.0));

        assertThat(setOfPlaces.bzmpopMax(Duration.ofSeconds(1), 3, "zset1")).isEmpty();
    }

    @Test
    @RequiresRedis7OrHigher
    public void bzmpopMax() {
        assertThat(setOfPlaces.bzmpopMax(Duration.ofSeconds(1), "zset1")).isEqualTo(null);

        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(setOfPlaces.bzmpopMax(Duration.ofSeconds(10), "zset1")).isEqualTo(ScoredValue.of(Place.grignan, 2.0));
        assertThat(setOfPlaces.bzmpopMax(Duration.ofSeconds(10), "zset1")).isEqualTo(ScoredValue.of(Place.crussol, 1.0));
        assertThat(setOfPlaces.bzmpopMax(Duration.ofSeconds(1), "zset1")).isNull();

        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(setOfPlaces.bzmpopMax(Duration.ofSeconds(10), 2, "zset1")).containsExactly(
                ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.crussol, 1.0));

        setOfPlaces.zadd("zset1", Map.of(Place.crussol, 1.0, Place.grignan, 2.0));
        assertThat(setOfPlaces.bzmpopMax(Duration.ofSeconds(10), 3, "zset1")).containsExactly(
                ScoredValue.of(Place.grignan, 2.0), ScoredValue.of(Place.crussol, 1.0));

        assertThat(setOfPlaces.bzmpopMax(Duration.ofSeconds(1), 3, "zset1")).isEmpty();
    }

    @Test
    @RequiresRedis6OrHigher
    void zrangebylex() {
        populateManyStringEntriesForLex();

        assertThat(setOfStrings.zrangebylex(key, new Range<>("-", "+"))).hasSize(100);
        assertThat(setOfStrings.zrangebylex(key, new Range<>("-", "+"), new ZRangeArgs().limit(10, 10))).hasSize(10);

        assertThat(setOfStrings.zrangebylex(key, Range.unbounded())).hasSize(100);
        assertThat(setOfStrings.zrangebylex(key, new Range<>("value", "zzz"))).hasSize(100);
        assertThat(setOfStrings.zrangebylex(key, new Range<>("value98", "value99"))).containsSequence("value98",
                "value99");
        assertThat(setOfStrings.zrangebylex(key, new Range<>("value99", true, null, false))).hasSize(1);
        assertThat(setOfStrings.zrangebylex(key, new Range<>("value99", false, null, false))).hasSize(0);
    }

    @Test
    @RequiresRedis6OrHigher
    void zremrangebylex() {
        populateManyStringEntriesForLex();
        assertThat(setOfStrings.zremrangebylex(key, new Range<>("aaa", false, "zzz", true))).isEqualTo(100);

        populateManyStringEntriesForLex();
        assertThat(setOfStrings.zremrangebylex(key, new Range<>("aaa", "zzz"))).isEqualTo(100);
    }

    @Test
    @RequiresRedis6OrHigher
    void zunion() {
        String zset1 = "zset1";
        String zset2 = "zset2";

        assertThat(setOfPlaces.zadd(zset1, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(zset1, 2.0, Place.grignan)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 2.0, Place.grignan)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 3.0, Place.suze)).isTrue();

        assertThat(setOfPlaces.zunion(zset1, zset2)).isEqualTo(List.of(Place.crussol, Place.suze, Place.grignan));
        assertThat(setOfPlaces.zunionWithScores(zset1, zset2)).isEqualTo(
                List.of(ScoredValue.of(Place.crussol, 2.0), ScoredValue.of(Place.suze, 3.0),
                        ScoredValue.of(Place.grignan, 4.0)));

        assertThat(setOfPlaces.zunion(new ZAggregateArgs().max(), zset1, zset2))
                .isEqualTo(List.of(Place.crussol, Place.grignan, Place.suze));
        assertThat(setOfPlaces.zunionWithScores(new ZAggregateArgs().max(), zset1, zset2)).isEqualTo(
                List.of(ScoredValue.of(Place.crussol, 1.0), ScoredValue.of(Place.grignan, 2.0),
                        ScoredValue.of(Place.suze, 3.0)));
    }

    @Test
    @RequiresRedis6OrHigher
    void zinter() {
        String zset1 = "zset1";
        String zset2 = "zset2";

        assertThat(setOfPlaces.zadd(zset1, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(zset1, 2.0, Place.grignan)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 2.0, Place.grignan)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 3.0, Place.suze)).isTrue();

        assertThat(setOfPlaces.zinter(zset1, zset2)).isEqualTo(List.of(Place.crussol, Place.grignan));
        assertThat(setOfPlaces.zinterWithScores(zset1, zset2))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0), ScoredValue.of(Place.grignan, 4.0)));

        assertThatThrownBy(() -> setOfPlaces.zinter(zset2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> setOfPlaces.zinter()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @RequiresRedis6OrHigher
    void zinterWithScores() {
        String zset1 = "zset1";
        String zset2 = "zset2";

        assertThat(setOfPlaces.zadd(zset1, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(zset1, 2.0, Place.grignan)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 2.0, Place.grignan)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 3.0, Place.suze)).isTrue();

        assertThat(setOfPlaces.zinterWithScores(zset1, zset2)).isEqualTo(List.of(ScoredValue.of(Place.crussol, 2.0),
                ScoredValue.of(Place.grignan, 4.0)));
        assertThat(setOfPlaces.zinterWithScores(new ZAggregateArgs().max(), zset1, zset2))
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0), ScoredValue.of(Place.grignan, 2.0)));
    }

    @Test
    @RequiresRedis6OrHigher
    void zinterWithArgs() {
        String zset1 = "zset1";
        String zset2 = "zset2";

        assertThat(setOfPlaces.zadd(zset1, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(zset1, 2.0, Place.grignan)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 1.0, Place.crussol)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 2.0, Place.grignan)).isTrue();
        assertThat(setOfPlaces.zadd(zset2, 3.0, Place.suze)).isTrue();

        assertThat(setOfPlaces.zinter(new ZAggregateArgs().min(), zset1, zset2))
                .isEqualTo(List.of(Place.crussol, Place.grignan));
        List<ScoredValue<Place>> actual = setOfPlaces.zinterWithScores(new ZAggregateArgs().max(), zset1, zset2);
        assertThat(actual)
                .isEqualTo(List.of(ScoredValue.of(Place.crussol, 1.0), ScoredValue.of(Place.grignan, 2.0)));
    }

    String value = "value";

    private void populateManyStringEntries() {
        for (int i = 0; i < 100; i++) {
            setOfStrings.zadd(key + 1, i, value + i);
            setOfStrings.zadd(key, i, value + i);
        }
    }

    private void populateManyStringEntriesForLex() {
        for (int i = 0; i < 100; i++) {
            setOfStrings.zadd(key + 1, 1.0, value + i);
            setOfStrings.zadd(key, 1.0, value + i);
        }
    }

    @Test
    @RequiresRedis6OrHigher
    void sort() {
        SortedSetCommands<String, String> commands = ds.sortedSet(String.class, String.class);
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

        ListCommands<String, String> listCommands = ds.list(String.class, String.class);
        assertThat(listCommands.lrange("dest1", 0, -1)).containsExactly("a", "b", "e", "f");
        assertThat(listCommands.lpop("dest2", 100)).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    void zaddWithTypeReference() {
        var set = ds.sortedSet(new TypeReference<List<Place>>() {
            // Empty on purpose
        });
        assertThat(set.zadd(key, 1.0, List.of(Place.crussol, Place.suze))).isTrue();
        assertThat(set.zadd(key, 1.0, List.of(Place.crussol, Place.suze))).isFalse();

        assertThat(set.zrange(key, 0, -1)).isEqualTo(List.of(List.of(Place.crussol, Place.suze)));
        assertThat(set.zadd(key, new ScoredValue<>(List.of(Place.grignan), 2.0), new ScoredValue<>(List.of(Place.suze), 3.0)))
                .isEqualTo(2);
        assertThat(set.zrange(key, 0, -1)).hasSize(3);
    }
}
