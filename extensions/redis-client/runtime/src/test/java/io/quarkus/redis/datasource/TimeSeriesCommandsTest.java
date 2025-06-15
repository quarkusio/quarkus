package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.timeseries.AddArgs;
import io.quarkus.redis.datasource.timeseries.Aggregation;
import io.quarkus.redis.datasource.timeseries.AlterArgs;
import io.quarkus.redis.datasource.timeseries.CreateArgs;
import io.quarkus.redis.datasource.timeseries.DuplicatePolicy;
import io.quarkus.redis.datasource.timeseries.Filter;
import io.quarkus.redis.datasource.timeseries.IncrementArgs;
import io.quarkus.redis.datasource.timeseries.MGetArgs;
import io.quarkus.redis.datasource.timeseries.MRangeArgs;
import io.quarkus.redis.datasource.timeseries.RangeArgs;
import io.quarkus.redis.datasource.timeseries.Reducer;
import io.quarkus.redis.datasource.timeseries.Sample;
import io.quarkus.redis.datasource.timeseries.SeriesSample;
import io.quarkus.redis.datasource.timeseries.TimeSeriesCommands;
import io.quarkus.redis.datasource.timeseries.TimeSeriesRange;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

@RequiresCommand("ts.create")
public class TimeSeriesCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    private TimeSeriesCommands<String> ts;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));
        ts = ds.timeseries();
    }

    @AfterEach
    void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(ts.getDataSource());
    }

    @Test
    void testCreationAndAddingDataPoint() throws InterruptedException {
        ts.tsCreate(key);
        ts.tsCreate("sensor", new CreateArgs().setRetention(Duration.of(2678400000L, ChronoUnit.MILLIS)));

        ts.tsAlter("sensor", new AlterArgs().chunkSize(1024));

        ts.tsAdd(key, 1626434637914L, 26);
        ts.tsAdd(key, 27);

        Thread.sleep(1); // Do make sure we have a different timestamp
        ts.tsMAdd(SeriesSample.from(key, 51), SeriesSample.from(key, 1626434637915L, 52),
                SeriesSample.from("sensor", 22));

        var list = ts.tsRange(key, TimeSeriesRange.fromTimeSeries());
        assertThat(list).hasSize(4);

        list = ts.tsRange("sensor", TimeSeriesRange.fromTimestampToLatest(0));
        assertThat(list).hasSize(1);
    }

    @Test
    void testAdd() throws InterruptedException {
        ts.tsAdd(key, 25.0);
        Thread.sleep(10); // Make sure the timestamp is different
        ts.tsAdd(key, 30.5, new AddArgs().label("foo", "bar"));
        var list = ts.tsRange(key, TimeSeriesRange.fromTimeSeries());
        assertThat(list).hasSize(2);
    }

    @Test
    void testCreationWhileAdding() {
        long timestamp = System.currentTimeMillis() - 1000;
        ts.tsAdd(key, timestamp, 26,
                new AddArgs().compressed().label("foo", "bar").setRetention(Duration.ofDays(1)).chunkSize(1024));
        ts.tsAdd(key, 27);

        var list = ts.tsRange(key, TimeSeriesRange.fromTimeSeries());
        assertThat(list).hasSize(2);

        ts.tsAdd(key, timestamp, 25, new AddArgs().onDuplicate(DuplicatePolicy.LAST));
        list = ts.tsRange(key, TimeSeriesRange.fromTimeSeries());
        assertThat(list).hasSize(2);
        assertThat(list.stream().map(s -> s.value).collect(Collectors.toList())).containsExactlyInAnyOrder(27.0, 25.0);
    }

    @Test
    void testIncrAndDecr() {
        ts.tsCreate(key);
        ts.tsAdd(key, 10, 26);
        ts.tsAdd(key, 20, 30);

        ts.tsIncrBy(key, 12);

        assertThat(ts.tsGet(key).value).isEqualTo(30 + 12);
        ts.tsIncrBy(key, 12, new IncrementArgs().label("foo", "bar"));
        assertThat(ts.tsGet(key).value).isEqualTo(30 + 12 + 12);

        ts.tsDecrBy(key, 2);
        assertThat(ts.tsGet(key).value).isEqualTo(30 + 12 + 12 - 2);
        ts.tsDecrBy(key, 2, new IncrementArgs().uncompressed());
        assertThat(ts.tsGet(key).value).isEqualTo(30 + 12 + 12 - 2 - 2);
    }

    @Test
    void testDeletion() {
        ts.tsCreate(key);
        ts.tsAdd(key, 500, 50);
        ts.tsAdd(key, 501, 51);
        ts.tsAdd(key, 1000, 26);
        ts.tsAdd(key, 1001, 27);
        ts.tsAdd(key, 2000, 28);
        ts.tsAdd(key, 2001, 29);

        var list = ts.tsRange(key, TimeSeriesRange.fromTimeSeries());
        assertThat(list).hasSize(6);

        ts.tsDel(key, 1000, 1999);
        list = ts.tsRange(key, TimeSeriesRange.fromTimeSeries());
        assertThat(list).hasSize(4);

        ts.tsDel(key, 501, 501);
        list = ts.tsRange(key, TimeSeriesRange.fromTimeSeries());
        assertThat(list).hasSize(3);
    }

    @Test
    void testAggregation() {
        ts.tsCreate(key);
        ts.tsCreate("cc");
        ts.tsCreate("avg");

        ts.tsCreateRule(key, "cc", Aggregation.COUNT, Duration.of(1, ChronoUnit.SECONDS));
        ts.tsCreateRule(key, "avg", Aggregation.AVG, Duration.of(10, ChronoUnit.MILLIS));

        ts.tsAdd(key, 500, 50);
        ts.tsAdd(key, 2003, 42);
        ts.tsAdd(key, 501, 51);
        ts.tsAdd(key, 1000, 26);
        ts.tsAdd(key, 1001, 27);
        ts.tsAdd(key, 2000, 28);
        ts.tsAdd(key, 2001, 29);
        ts.tsAdd(key, 1002, 1002339488832.23477);
        ts.tsAdd(key, 1003, 45.56778);
        ts.tsAdd(key, 2004, 45);
        ts.tsAdd(key, 3000, 45);

        var counts = ts.tsRange("cc", TimeSeriesRange.fromEarliestToTimestamp(5000));
        assertThat(counts).hasSize(3);

        var avg = ts.tsRange("avg", TimeSeriesRange.fromEarliestToTimestamp(5000));
        assertThat(avg).hasSize(3);

        ts.tsDeleteRule(key, "cc");
    }

    @Test
    void testFiltering() {
        ts.tsCreate(key, new CreateArgs().label("area", 1).label("common", "1").label("foo", "bar"));
        ts.tsCreate("sensor", new CreateArgs().label("common", "1").label("foo", "baz"));

        ts.tsAdd(key, 500, 50, new AddArgs().label("area", 51));
        ts.tsAdd("sensor", 2003, 42, new AddArgs().label("area", 52));
        ts.tsAdd("sensor", 501, 51, new AddArgs().label("area", 51));
        ts.tsAdd(key, 1000, 26, new AddArgs().label("area", 51));
        ts.tsAdd(key, 1001, 27, new AddArgs().label("area", 32));
        ts.tsAdd(key, 2000, 28, new AddArgs().label("area", 51).label("name", "h1"));
        ts.tsAdd("sensor", 2001, 29, new AddArgs().label("area", 52));
        ts.tsAdd(key, 1002, 1002339488832.23477, new AddArgs().label("area", 53));
        ts.tsAdd("sensor", 1003, 45.56778, new AddArgs().label("area", 12));
        ts.tsAdd("sensor", 2004, 45, new AddArgs().label("area", 12));
        ts.tsAdd(key, 3000, 45, new AddArgs().label("area", 12));

        var map = ts.tsMRange(TimeSeriesRange.fromTimestamps(10, 10000), Filter.withLabel("foo", "bar"));
        assertThat(map).hasSize(1);
        assertThat(map.get(key).group()).isEqualTo(key);
        assertThat(map.get(key).samples()).hasSize(6);
        assertThat(map.get(key).labels()).isEmpty();

        map = ts.tsMRange(TimeSeriesRange.fromTimestamps(10, 10000), new MRangeArgs().withLabels(),
                Filter.withLabel("foo", "bar"));
        assertThat(map).hasSize(1);
        assertThat(map.get(key).group()).isEqualTo(key);
        assertThat(map.get(key).samples()).hasSize(6);
        assertThat(map.get(key).labels()).hasSize(3);

        map = ts.tsMGet(new MGetArgs().withLabels(), Filter.withLabelHavingValueFrom("foo", "bar", "baz"));
        assertThat(map).hasSize(2);

        map = ts.tsMGet(Filter.withLabelHavingValueFrom("foo", "bar", "baz"));
        assertThat(map).hasSize(2);

        map = ts.tsMGet(new MGetArgs().selectedLabel("foo"), Filter.withLabelHavingValueFrom("foo", "bar", "baz"));
        assertThat(map).hasSize(2);

        var l = ts.tsRange(key, TimeSeriesRange.fromTimeSeries(), new RangeArgs().filterByValue(25, 30));
        assertThat(l).hasSize(3);

        map = ts.tsMRange(TimeSeriesRange.fromTimeSeries(), new MRangeArgs().filterByValue(25, 30),
                Filter.withLabel("foo", "bar"));
        assertThat(map.get(key).samples()).hasSize(3);

        map = ts.tsMRange(TimeSeriesRange.fromTimeSeries(), new MRangeArgs().filterByValue(25, 30),
                Filter.withLabel("foo"), Filter.withLabel("area", 1));
        assertThat(map).hasSize(0);

        l = ts.tsRange(key, TimeSeriesRange.fromTimeSeries(),
                new RangeArgs().filterByTimestamp(1000, 1001, 2000, 2005));
        assertThat(l).hasSize(3);

        map = ts.tsMRange(TimeSeriesRange.fromTimeSeries(), new MRangeArgs().filterByTimestamp(1000, 1001, 2000, 2005),
                Filter.withLabel("foo", "bar"));
        assertThat(map.get(key).samples()).hasSize(3);
    }

    @Test
    void testAggregationInRange() {
        ts.tsCreate(key, new CreateArgs().label("area", 1).label("common", "1").label("foo", "bar"));
        ts.tsCreate("sensor", new CreateArgs().label("common", "1").label("foo", "baz"));

        ts.tsAdd(key, 500, 50);
        ts.tsAdd("sensor", 2003, 42);
        ts.tsAdd("sensor", 501, 51);
        ts.tsAdd(key, 1000, 26);
        ts.tsAdd(key, 1001, 27);
        ts.tsAdd(key, 2000, 28);
        ts.tsAdd("sensor", 2001, 29);
        ts.tsAdd(key, 1002, 1002339488832.23477);
        ts.tsAdd("sensor", 1003, 45.56778);
        ts.tsAdd("sensor", 2004, 45);
        ts.tsAdd(key, 3000, 45);

        var l = ts.tsRange(key, TimeSeriesRange.fromTimeSeries(),
                new RangeArgs().aggregation(Aggregation.AVG, Duration.ofMillis(3600000)));
        assertThat(l).hasSize(1);

        var map = ts.tsMRange(TimeSeriesRange.fromTimeSeries(),
                new MRangeArgs().aggregation(Aggregation.AVG, Duration.ofMillis(3600000)), Filter.withLabel("area", 1));
        assertThat(map.get(key).samples()).hasSize(1);

        map = ts.tsMRange(TimeSeriesRange.fromTimeSeries(),
                new MRangeArgs().aggregation(Aggregation.MIN, Duration.ofMillis(3600000)).align(1000),
                Filter.withLabel("area", 1));
        assertThat(map.get(key).samples()).hasSize(2);
    }

    @Test
    void testAggregationInRevRange() {
        ts.tsCreate(key, new CreateArgs().label("area", 1).label("common", "1").label("foo", "bar"));
        ts.tsCreate("sensor", new CreateArgs().label("common", "1").label("foo", "baz"));

        ts.tsAdd(key, 500, 50);
        ts.tsAdd("sensor", 2003, 42);
        ts.tsAdd("sensor", 501, 51);
        ts.tsAdd(key, 1000, 26);
        ts.tsAdd(key, 1001, 27);
        ts.tsAdd(key, 2000, 28);
        ts.tsAdd("sensor", 2001, 29);
        ts.tsAdd(key, 1002, 1002339488832.23477);
        ts.tsAdd("sensor", 1003, 45.56778);
        ts.tsAdd("sensor", 2004, 45);
        ts.tsAdd(key, 3000, 45);

        var l = ts.tsRevRange(key, TimeSeriesRange.fromTimeSeries(),
                new RangeArgs().aggregation(Aggregation.AVG, Duration.ofMillis(3600000)));
        assertThat(l).hasSize(1);

        var map = ts.tsMRevRange(TimeSeriesRange.fromTimeSeries(),
                new MRangeArgs().aggregation(Aggregation.AVG, Duration.ofMillis(3600000)), Filter.withLabel("area", 1));
        assertThat(map.get(key).samples()).hasSize(1);

        map = ts.tsMRevRange(TimeSeriesRange.fromTimeSeries(),
                new MRangeArgs().aggregation(Aggregation.MIN, Duration.ofMillis(3600000)).align(1000),
                Filter.withLabel("area", 1));
        assertThat(map.get(key).samples()).hasSize(2);
    }

    @Test
    void testQueryIndex() {
        ts.tsCreate("telemetry:study:temperature",
                new CreateArgs().label("rooms", "study").label("type", "temperature"));
        ts.tsCreate("telemetry:study:humidity", new CreateArgs().label("rooms", "study").label("type", "humidity"));
        ts.tsCreate("telemetry:kitchen:temperature",
                new CreateArgs().label("rooms", "kitchen").label("type", "temperature"));
        ts.tsCreate("telemetry:kitchen:humidity", new CreateArgs().label("rooms", "kitchen").label("type", "humidity"));

        var list = ts.tsQueryIndex(Filter.withLabel("rooms", "kitchen"));
        assertThat(list).containsExactlyInAnyOrder("telemetry:kitchen:humidity", "telemetry:kitchen:temperature");

        list = ts.tsQueryIndex(Filter.withLabel("type", "temperature"));
        assertThat(list).containsExactlyInAnyOrder("telemetry:study:temperature", "telemetry:kitchen:temperature");
    }

    @Test
    void testGetAdd() {
        ts.tsCreate("temp:TLV", new CreateArgs().label("type", "temp").label("location", "TLV"));
        ts.tsCreate("temp:JLM", new CreateArgs().label("type", "temp").label("location", "JLM"));

        ts.tsMAdd(SeriesSample.from("temp:TLV", 1000, 30), SeriesSample.from("temp:TLV", 1010, 35),
                SeriesSample.from("temp:TLV", 1020, 9999), SeriesSample.from("temp:TLV", 1030, 40));
        ts.tsMAdd(SeriesSample.from("temp:JLM", 1005, 30), SeriesSample.from("temp:JLM", 1015, 35),
                SeriesSample.from("temp:JLM", 1025, 9999), SeriesSample.from("temp:JLM", 1035, 40));

        assertThat(ts.tsGet("temp:JLM").value).isEqualTo(40);
    }

    @Test
    void testRevRange() {
        ts.tsCreate("temp:TLV", new CreateArgs().label("type", "temp").label("location", "TLV"));

        ts.tsMAdd(SeriesSample.from("temp:TLV", 1000, 30), SeriesSample.from("temp:TLV", 1010, 40),
                SeriesSample.from("temp:TLV", 1030, 35));

        var list = ts.tsRevRange("temp:TLV", TimeSeriesRange.fromTimeSeries());
        assertThat(list).hasSize(3);
        assertThat(list.get(0).value).isEqualTo(35);

        var map = ts.tsMRevRange(TimeSeriesRange.fromTimeSeries(), Filter.withLabel("type", "temp"));
        assertThat(map).hasSize(1);
        assertThat(map.get("temp:TLV").samples()).hasSize(3);
    }

    @SuppressWarnings("unchecked")
    @Test
    void groupByAndReducerTest() {
        String a = "stock:a";
        String b = "stock:b";
        ts.tsCreate(a, new CreateArgs().label("type", "stock").label("name", "a"));
        ts.tsCreate(b, new CreateArgs().label("type", "stock").label("name", "b"));

        ts.tsMAdd(SeriesSample.from(a, 1000, 100), SeriesSample.from(a, 1010, 110), SeriesSample.from(a, 1020, 120));
        ts.tsMAdd(SeriesSample.from(b, 1000, 110), SeriesSample.from(b, 1010, 110), SeriesSample.from(a, 1020, 100));
        var res = ts.tsMRange(TimeSeriesRange.fromTimeSeries(),
                new MRangeArgs().withLabels().groupBy("type", Reducer.MAX), Filter.withLabel("type", "stock"));
        assertThat(res).hasSize(1);
        assertThat(res.get("type=stock").labels().get("type")).isEqualTo("stock");
        assertThat(res.get("type=stock").samples().stream().map(s -> s.value)).containsExactlyInAnyOrder(110.0, 110.0,
                120.0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void groupByReduceAndAggregationTest() {
        String a = "stock:a";
        String b = "stock:b";
        ts.tsCreate(a, new CreateArgs().label("type", "stock").label("name", "a"));
        ts.tsCreate(b, new CreateArgs().label("type", "stock").label("name", "b"));

        ts.tsMAdd(SeriesSample.from(a, 1000, 100), SeriesSample.from(a, 1010, 110), SeriesSample.from(a, 1020, 120));
        ts.tsMAdd(SeriesSample.from(b, 1000, 120), SeriesSample.from(b, 1010, 110), SeriesSample.from(a, 1020, 100));

        ts.tsMAdd(SeriesSample.from(a, 2000, 200), SeriesSample.from(a, 2010, 210), SeriesSample.from(a, 2020, 220));
        ts.tsMAdd(SeriesSample.from(b, 2000, 220), SeriesSample.from(b, 2010, 210), SeriesSample.from(a, 2020, 200));

        ts.tsMAdd(SeriesSample.from(a, 3000, 300), SeriesSample.from(a, 3010, 310), SeriesSample.from(a, 3020, 320));
        ts.tsMAdd(SeriesSample.from(b, 3000, 320), SeriesSample.from(b, 3010, 310), SeriesSample.from(a, 3020, 300));

        var res = ts.tsMRange(
                TimeSeriesRange.fromTimeSeries(), new MRangeArgs().withLabels()
                        .aggregation(Aggregation.AVG, Duration.ofMillis(1000)).groupBy("type", Reducer.MIN),
                Filter.withLabel("type", "stock"));
        assertThat(res).hasSize(1);
        assertThat(res.get("type=stock").labels().get("type")).isEqualTo("stock");
        assertThat(res.get("type=stock").samples().stream().map(s -> s.value)).containsExactlyInAnyOrder(110.0, 210.0,
                310.0);

        res = ts.tsMRange(
                TimeSeriesRange.fromTimeSeries(), new MRangeArgs().withLabels()
                        .aggregation(Aggregation.AVG, Duration.ofMillis(1000)).groupBy("type", Reducer.MIN),
                Filter.withLabel("type", "stock"));
        assertThat(res).hasSize(1);
        assertThat(res.get("type=stock").labels().get("type")).isEqualTo("stock");
        assertThat(res.get("type=stock").samples().stream().map(s -> s.value)).containsExactlyInAnyOrder(110.0, 210.0,
                310.0);

        res = ts.tsMRange(TimeSeriesRange.fromTimestamps(0, 5000),
                new MRangeArgs().withLabels().alignUsingRangeStart()
                        .aggregation(Aggregation.AVG, Duration.ofMillis(1000)).count(2).groupBy("type", Reducer.MIN),
                Filter.withLabel("type", "stock"));
        assertThat(res).hasSize(1);
        assertThat(res.get("type=stock").labels().get("type")).isEqualTo("stock");
        assertThat(res.get("type=stock").samples()).hasSize(2);

        res = ts.tsMRange(
                TimeSeriesRange.fromTimestamps(0, 5000), new MRangeArgs().withLabels().alignUsingRangeEnd()
                        .aggregation(Aggregation.AVG, Duration.ofMillis(1000)).empty().groupBy("type", Reducer.MIN),
                Filter.withLabel("type", "stock"));
        assertThat(res).hasSize(1);
    }

    @Test
    void groupByReturningMultipleGroupTest() {
        ts.tsAdd("ts1", 1548149180000L, 90,
                new AddArgs().label("metric", "cpu").label("metric_name", "system").label("team", "NY"));
        ts.tsAdd("ts1", 1548149185000L, 45);

        ts.tsAdd("ts2", 1548149180000L, 99,
                new AddArgs().label("metric", "cpu").label("metric_name", "user").label("team", "SF"));

        var res = ts.tsMRange(TimeSeriesRange.fromTimeSeries(),
                new MRangeArgs().withLabels().groupBy("metric_name", Reducer.MAX), Filter.withLabel("metric", "cpu"));
        assertThat(res).hasSize(2);
        assertThat(res.get("metric_name=system").samples()).hasSize(2);
        assertThat(res.get("metric_name=user").samples()).containsExactly(new Sample(1548149180000L, 99));

        // Test query by value
        res = ts.tsMRange(TimeSeriesRange.fromTimeSeries(), new MRangeArgs().withLabels().filterByValue(90, 100),
                Filter.withLabel("metric", "cpu"));
        assertThat(res).hasSize(2);
        assertThat(res.get("ts1").samples()).containsExactly(new Sample(1548149180000L, 90));
        assertThat(res.get("ts2").samples()).containsExactly(new Sample(1548149180000L, 99));

        // Test query using label
        res = ts.tsMRange(TimeSeriesRange.fromTimeSeries(), new MRangeArgs().selectedLabels("team"),
                Filter.withLabel("metric", "cpu"));
        assertThat(res).hasSize(2);
        assertThat(res.get("ts1").labels()).containsExactly(entry("team", "NY"));
        assertThat(res.get("ts1").samples()).hasSize(2);
        assertThat(res.get("ts2").labels()).containsExactly(entry("team", "SF"));
        assertThat(res.get("ts2").samples()).containsExactly(new Sample(1548149180000L, 99));
    }

    @Test
    void testSample() {
        Sample s1 = new Sample(1000, 24.5);
        Sample s2 = new Sample(1000, 24.5);
        assertThat(s1.value).isEqualTo(24.5);
        assertThat(s2.timestamp()).isEqualTo(1000);
        assertThat(s1).isEqualTo(s2);
        assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
    }

    @Test
    void testRange() {
        ts.tsCreate("temp", new CreateArgs().chunkSize(1024).compressed().label("location", "TLV").forever());
        ts.tsMAdd(SeriesSample.from("temp", 1000, 30), SeriesSample.from("temp", 1010, 35),
                SeriesSample.from("temp", 1020, 9999), SeriesSample.from("temp", 1030, 40));

        // Filter by value
        assertThat(ts.tsRange("temp", TimeSeriesRange.fromTimeSeries(), new RangeArgs().filterByValue(-100, 100)))
                .hasSize(3);

        // With aggregation
        assertThat(ts.tsRange("temp", TimeSeriesRange.fromTimestamps(0, 2000), new RangeArgs().filterByValue(-100, 100)
                .aggregation(Aggregation.AVG, Duration.ofSeconds(1)).alignUsingRangeEnd())).hasSize(1)
                .containsExactly(new Sample(1000, 35));
    }

    @Test
    void testRangeWithAlignment() {
        String a = "stock:a";
        ts.tsCreate(a, new CreateArgs().label("type", "stock").label("name", "a"));

        ts.tsMAdd(SeriesSample.from(a, 1000, 100), SeriesSample.from(a, 1010, 110), SeriesSample.from(a, 1020, 120));
        ts.tsMAdd(SeriesSample.from(a, 2000, 200), SeriesSample.from(a, 2010, 210), SeriesSample.from(a, 2020, 220));
        ts.tsMAdd(SeriesSample.from(a, 3000, 300), SeriesSample.from(a, 3010, 310), SeriesSample.from(a, 3020, 320));

        var list = ts.tsRange(a, TimeSeriesRange.fromTimeSeries(),
                new RangeArgs().aggregation(Aggregation.MIN, Duration.ofMillis(20)).empty());
        assertThat(list).contains(new Sample(1000, 100), new Sample(1020, 120), new Sample(2000, 200),
                new Sample(2020, 220), new Sample(3000, 300), new Sample(3020, 320));

        list = ts.tsRange(a, TimeSeriesRange.fromTimeSeries(),
                new RangeArgs().align(10).count(2).aggregation(Aggregation.MIN, Duration.ofMillis(20)));
        assertThat(list).hasSize(2).contains(new Sample(990, 100), new Sample(1010, 110));

        list = ts.tsRange(a, TimeSeriesRange.fromTimestampToLatest(5),
                new RangeArgs().alignUsingRangeStart().aggregation(Aggregation.MIN, Duration.ofMillis(20)));
        assertThat(list).contains(new Sample(985, 100), new Sample(1005, 110));

        list = ts.tsRange(a, TimeSeriesRange.fromEarliestToTimestamp(3000),
                new RangeArgs().alignUsingRangeEnd().aggregation(Aggregation.MIN, Duration.ofMillis(20)));
        assertThat(list).contains(new Sample(1000, 100), new Sample(2000, 200));

    }

    @Test
    void testCreation() {
        ts.tsCreate(key, new CreateArgs().forever().duplicatePolicy(DuplicatePolicy.LAST).uncompressed());

        ts.tsAdd(key, 10, 10);
        ts.tsAdd(key, 10, 20);

        assertThat(ts.tsGet(key).value()).isEqualTo(20L);
    }

    @Test
    void testIncrementWithCreation() {
        ts.tsIncrBy(key, 10, new IncrementArgs().setTimestamp(1000).label("foo", "bar").uncompressed().chunkSize(1024)
                .setRetention(Duration.ofDays(1)));
        assertThat(ts.tsGet(key).value()).isEqualTo(10L);
        assertThat(ts.tsGet(key).timestamp()).isEqualTo(1000);
    }

    @Test
    void testTimeSeriesWithTypeReference() throws InterruptedException {
        var ts = ds.timeseries(new TypeReference<List<Person>>() {
            // Empty on purpose.
        });
        var key = List.of(Person.person1, Person.person2);
        var key2 = List.of(Person.person0);
        ts.tsCreate(key);
        ts.tsCreate(key2, new CreateArgs().setRetention(Duration.of(2678400000L, ChronoUnit.MILLIS)));

        ts.tsAlter(key2, new AlterArgs().chunkSize(1024));

        ts.tsAdd(key, 1626434637914L, 26);
        ts.tsAdd(key, 27);

        Thread.sleep(1); // Do make sure we have a different timestamp
        ts.tsMAdd(SeriesSample.from(key, 51), SeriesSample.from(key, 1626434637915L, 52), SeriesSample.from(key2, 22));

        var list = ts.tsRange(key, TimeSeriesRange.fromTimeSeries());
        assertThat(list).hasSize(4);

        list = ts.tsRange(key2, TimeSeriesRange.fromTimestampToLatest(0));
        assertThat(list).hasSize(1);
    }

}
