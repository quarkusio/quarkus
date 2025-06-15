package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.timeseries.AddArgs;
import io.quarkus.redis.datasource.timeseries.CreateArgs;
import io.quarkus.redis.datasource.timeseries.Filter;
import io.quarkus.redis.datasource.timeseries.MGetArgs;
import io.quarkus.redis.datasource.timeseries.Sample;
import io.quarkus.redis.datasource.timeseries.SampleGroup;
import io.quarkus.redis.datasource.timeseries.SeriesSample;
import io.quarkus.redis.datasource.timeseries.TimeSeriesRange;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

@RequiresCommand("ts.create")
public class TransactionalTimeSeriesCommandsTest extends DatasourceTestBase {

    private RedisDataSource blocking;
    private ReactiveRedisDataSource reactive;

    @BeforeEach
    void initialize() {
        blocking = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(60));
        reactive = new ReactiveRedisDataSourceImpl(vertx, redis, api);
    }

    @AfterEach
    public void clear() {
        blocking.flushall();
    }

    @Test
    public void timeSeriesBlocking() {

        TransactionResult result = blocking.withTransaction(tx -> {
            var ts = tx.timeseries();
            assertThat(ts.getDataSource()).isEqualTo(tx);
            ts.tsCreate("ts1");
            ts.tsAdd("ts2", 10, 1, new AddArgs().label("foo", "bar").compressed().chunkSize(1024));
            ts.tsCreate("ts3", new CreateArgs().forever().label("foo", "baz"));
            ts.tsAdd("ts1", 2);

            ts.tsAdd("ts1", 20, 20);

            ts.tsMAdd(SeriesSample.from("ts1", 30, 3), SeriesSample.from("ts2", 30, 3),
                    SeriesSample.from("ts3", 30, 5));

            ts.tsGet("ts3"); // 5
            ts.tsMGet(Filter.withLabel("foo", "bar")); // ts2
            ts.tsMGet(new MGetArgs().withLabels(), Filter.withLabel("foo", "baz")); // ts3

            ts.tsRange("ts3", TimeSeriesRange.fromTimeSeries());
            ts.tsMRange(TimeSeriesRange.fromTimeSeries(), Filter.withLabelHavingValueFrom("foo", "bar", "baz"));
        });

        assertThat(result.size()).isEqualTo(11);
        assertThat(result.discarded()).isFalse();

        assertThat((Sample) result.get(6)).isEqualTo(new Sample(30, 5.0));
        Map<String, SampleGroup> map = result.get(7);
        assertThat(map).hasSize(1);
        map = result.get(8);
        assertThat(map).hasSize(1);
        assertThat((List<Sample>) result.get(9)).containsExactly(new Sample(30, 5.0));
        map = result.get(10);
        assertThat(map).hasSize(2);
    }

    @Test
    public void timeseriesReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            var ts = tx.timeseries();
            assertThat(ts.getDataSource()).isEqualTo(tx);
            var u1 = ts.tsCreate("ts1");
            var u2 = ts.tsAdd("ts2", 10, 1, new AddArgs().label("foo", "bar").compressed().chunkSize(1024));
            var u3 = ts.tsCreate("ts3", new CreateArgs().forever().label("foo", "baz"));
            var u4 = ts.tsAdd("ts1", 2);

            var u5 = ts.tsAdd("ts1", 20, 20);

            var u6 = ts.tsMAdd(SeriesSample.from("ts1", 30, 3), SeriesSample.from("ts2", 30, 3),
                    SeriesSample.from("ts3", 30, 5));

            var u7 = ts.tsGet("ts3"); // 5
            var u8 = ts.tsMGet(Filter.withLabel("foo", "bar")); // ts2
            var u9 = ts.tsMGet(new MGetArgs().withLabels(), Filter.withLabel("foo", "baz")); // ts3

            var u10 = ts.tsRange("ts3", TimeSeriesRange.fromTimeSeries());
            var u11 = ts.tsMRange(TimeSeriesRange.fromTimeSeries(),
                    Filter.withLabelHavingValueFrom("foo", "bar", "baz"));

            return u1.chain(() -> u2).chain(() -> u3).chain(() -> u4).chain(() -> u5).chain(() -> u6).chain(() -> u7)
                    .chain(() -> u8).chain(() -> u9).chain(() -> u10).chain(() -> u11);
        }).await().indefinitely();

        assertThat(result.size()).isEqualTo(11);
        assertThat(result.discarded()).isFalse();

        assertThat((Sample) result.get(6)).isEqualTo(new Sample(30, 5.0));
        Map<String, SampleGroup> map = result.get(7);
        assertThat(map).hasSize(1);
        map = result.get(8);
        assertThat(map).hasSize(1);
        assertThat((List<Sample>) result.get(9)).containsExactly(new Sample(30, 5.0));
        map = result.get(10);
        assertThat(map).hasSize(2);
    }

}
