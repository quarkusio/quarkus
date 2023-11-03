package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.timeseries.AddArgs;
import io.quarkus.redis.datasource.timeseries.Aggregation;
import io.quarkus.redis.datasource.timeseries.AlterArgs;
import io.quarkus.redis.datasource.timeseries.CreateArgs;
import io.quarkus.redis.datasource.timeseries.Filter;
import io.quarkus.redis.datasource.timeseries.IncrementArgs;
import io.quarkus.redis.datasource.timeseries.MGetArgs;
import io.quarkus.redis.datasource.timeseries.MRangeArgs;
import io.quarkus.redis.datasource.timeseries.RangeArgs;
import io.quarkus.redis.datasource.timeseries.ReactiveTimeSeriesCommands;
import io.quarkus.redis.datasource.timeseries.Sample;
import io.quarkus.redis.datasource.timeseries.SampleGroup;
import io.quarkus.redis.datasource.timeseries.SeriesSample;
import io.quarkus.redis.datasource.timeseries.TimeSeriesRange;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.ResponseType;

public class ReactiveTimeSeriesCommandsImpl<K> extends AbstractTimeSeriesCommands<K>
        implements ReactiveTimeSeriesCommands<K>, ReactiveRedisCommands {

    private final ReactiveRedisDataSource reactive;
    protected final Type keyType;

    public ReactiveTimeSeriesCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k) {
        super(redis, k);
        this.keyType = k;
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<Void> tsCreate(K key, CreateArgs args) {
        return super._tsCreate(key, args)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsCreate(K key) {
        return super._tsCreate(key)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsAdd(K key, long timestamp, double value, AddArgs args) {
        return super._tsAdd(key, timestamp, value, args)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsAdd(K key, double value, AddArgs args) {
        return super._tsAdd(key, value, args)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsAdd(K key, long timestamp, double value) {
        return super._tsAdd(key, timestamp, value)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsAdd(K key, double value) {
        return super._tsAdd(key, value)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsAlter(K key, AlterArgs args) {
        return super._tsAlter(key, args)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsCreateRule(K key, K destKey, Aggregation aggregation, Duration bucketDuration) {
        return super._tsCreateRule(key, destKey, aggregation, bucketDuration)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsCreateRule(K key, K destKey, Aggregation aggregation, Duration bucketDuration, long alignTimestamp) {
        return super._tsCreateRule(key, destKey, aggregation, bucketDuration, alignTimestamp)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsDecrBy(K key, double value) {
        return super._tsDecrBy(key, value)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsDecrBy(K key, double value, IncrementArgs args) {
        return super._tsDecrBy(key, value, args)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsDel(K key, long fromTimestamp, long toTimestamp) {
        return super._tsDel(key, fromTimestamp, toTimestamp)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsDeleteRule(K key, K destKey) {
        return super._tsDeleteRule(key, destKey)
                .replaceWithVoid();
    }

    @Override
    public Uni<Sample> tsGet(K key) {
        return super._tsGet(key)
                .map(this::decodeSample);
    }

    Sample decodeSample(Response response) {
        if (response == null || response.size() == 0) {
            return null;
        }
        return new Sample(response.get(0).toLong(), response.get(1).toDouble());
    }

    @Override
    public Uni<Sample> tsGet(K key, boolean latest) {
        return super._tsGet(key, latest)
                .map(this::decodeSample);
    }

    @Override
    public Uni<Void> tsIncrBy(K key, double value) {
        return super._tsIncrBy(key, value)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsIncrBy(K key, double value, IncrementArgs args) {
        return super._tsIncrBy(key, value, args)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> tsMAdd(SeriesSample<K>... samples) {
        return super._tsMAdd(samples)
                .replaceWithVoid();
    }

    @Override
    public Uni<Map<String, SampleGroup>> tsMGet(MGetArgs args, Filter... filters) {
        return super._tsMGet(args, filters)
                .map(this::decodeGroup);
    }

    @Override
    public Uni<Map<String, SampleGroup>> tsMGet(Filter... filters) {
        return super._tsMGet(filters)
                .map(this::decodeGroup);
    }

    Map<String, SampleGroup> decodeGroup(Response response) {
        if (response == null) {
            return null;
        }
        if (response.size() == 0) {
            return Collections.emptyMap();
        }
        Map<String, SampleGroup> groups = new HashMap<>();

        if (isMap(response)) {
            for (String group : response.getKeys()) {
                Map<String, String> labels = new HashMap<>();
                List<Sample> samples = new ArrayList<>();
                var nested = response.get(group);
                for (Response label : nested.get(0)) {
                    labels.put(label.get(0).toString(), label.get(1).toString());
                }
                for (Response sample : nested.get(nested.size() - 1)) { // samples are last
                    if (sample.type() == ResponseType.MULTI) {
                        samples.add(decodeSample(sample));
                    } else {
                        samples.add(new Sample(sample.toLong(), nested.get(nested.size() - 1).get(1).toDouble()));
                        break;
                    }
                }
                groups.put(group, new SampleGroup(group, labels, samples));
            }
            return groups;
        }

        for (Response nested : response) {
            // this should be the group
            String group = nested.get(0).toString();
            Map<String, String> labels = new HashMap<>();
            List<Sample> samples = new ArrayList<>();
            // labels are in 1
            for (Response label : nested.get(1)) {
                labels.put(label.get(0).toString(), label.get(1).toString());
            }

            // samples are in 2, either as a tuple or a list
            for (Response sample : nested.get(2)) {
                if (sample.type() == ResponseType.MULTI) {
                    samples.add(decodeSample(sample));
                } else {
                    samples.add(new Sample(sample.toLong(), nested.get(2).get(1).toDouble()));
                    break;
                }
            }

            groups.put(group, new SampleGroup(group, labels, samples));
        }

        return groups;
    }

    @Override
    public Uni<Map<String, SampleGroup>> tsMRange(TimeSeriesRange range, Filter... filters) {
        return super._tsMRange(range, filters)
                .map(this::decodeGroup);
    }

    @Override
    public Uni<Map<String, SampleGroup>> tsMRange(TimeSeriesRange range, MRangeArgs args, Filter... filters) {
        return super._tsMRange(range, args, filters)
                .map(this::decodeGroup);
    }

    @Override
    public Uni<Map<String, SampleGroup>> tsMRevRange(TimeSeriesRange range, Filter... filters) {
        return super._tsMRevRange(range, filters)
                .map(this::decodeGroup);
    }

    @Override
    public Uni<Map<String, SampleGroup>> tsMRevRange(TimeSeriesRange range, MRangeArgs args, Filter... filters) {
        return super._tsMRevRange(range, args, filters)
                .map(this::decodeGroup);
    }

    @Override
    public Uni<List<K>> tsQueryIndex(Filter... filters) {
        return super._tsQueryIndex(filters)
                .map(r -> marshaller.decodeAsList(r, keyType));
    }

    @Override
    public Uni<List<Sample>> tsRange(K key, TimeSeriesRange range) {
        return super._tsRange(key, range)
                .map(r -> marshaller.decodeAsList(r, this::decodeSample));
    }

    @Override
    public Uni<List<Sample>> tsRange(K key, TimeSeriesRange range, RangeArgs args) {
        return super._tsRange(key, range, args)
                .map(r -> marshaller.decodeAsList(r, this::decodeSample));
    }

    @Override
    public Uni<List<Sample>> tsRevRange(K key, TimeSeriesRange range) {
        return super._tsRevRange(key, range)
                .map(r -> marshaller.decodeAsList(r, this::decodeSample));
    }

    @Override
    public Uni<List<Sample>> tsRevRange(K key, TimeSeriesRange range, RangeArgs args) {
        return super._tsRevRange(key, range, args)
                .map(r -> marshaller.decodeAsList(r, this::decodeSample));
    }
}
