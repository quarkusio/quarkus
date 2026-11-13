package io.quarkus.redis.lettuce.runtime.internal.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.geo.GeoItem;
import io.quarkus.redis.datasource.geo.GeoPosition;
import io.quarkus.redis.datasource.geo.GeoRadiusArgs;
import io.quarkus.redis.datasource.geo.GeoRadiusStoreArgs;
import io.quarkus.redis.datasource.geo.GeoSearchArgs;
import io.quarkus.redis.datasource.geo.GeoSearchStoreArgs;
import io.quarkus.redis.datasource.geo.GeoUnit;
import io.quarkus.redis.datasource.geo.GeoValue;
import io.quarkus.redis.datasource.geo.ReactiveTransactionalGeoCommands;
import io.quarkus.redis.datasource.geo.TransactionalGeoCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;

class LettuceTransactionalGeoCommandsTest extends CommandsTestBase {

    RedisDataSource blockingDs;
    ReactiveRedisDataSource reactiveDs;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource(Duration.ofSeconds(60));
    }

    @Test
    void geoBlocking() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalGeoCommands<String, String> geo = tx.geo(String.class);
            assertThat(geo.getDataSource()).isEqualTo(tx);
            geo.geoadd(key, 10, 10, "1"); // 0 - true
            geo.geoadd(key, GeoItem.of("2", GeoPosition.of(-1, -1))); // 1 - true
            geo.geoadd(key, GeoItem.of("3", -20, -20)); // 2 - true
            geo.geodist(key, "1", "3", GeoUnit.KM); // 3 - some number
            geo.geopos(key, "2", "3", "4"); // 4 - list of position
            geo.geosearch(key, new GeoSearchArgs<String>().withDistance().ascending().byRadius(10000, GeoUnit.KM)); // 5 - list of geo value
        });
        assertThat(result.size()).isEqualTo(6);
        assertThat(result.discarded()).isFalse();
        assertThat((Boolean) result.get(0)).isTrue();
        assertThat((Boolean) result.get(1)).isTrue();
        assertThat((Boolean) result.get(2)).isTrue();
        assertThat((Double) result.get(3)).isPositive();
        List<GeoPosition> list = result.get(4);
        assertThat(list).hasSize(3);
        assertThat(list.get(2)).isNull();
        List<GeoValue<String>> values = result.get(5);
        assertThat(values).hasSize(3);
    }

    @Test
    void geoBlockingWithWatch() {
        // Members: "1" at (10, 10), "2" at (-1, -1) ~1726 km from "1", "3" at (-20, -20) ~4680 km from "1" and
        // ~2958 km from "2".
        String radiusStore = key + "-radius";
        String radiusDistStore = key + "-radius-dist";
        String byMemberStore = key + "-by-member";
        String searchStore = key + "-search";
        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalGeoCommands<String, String> geo = tx.geo(String.class);
            geo.geoadd(key, GeoItem.of("1", 10, 10), GeoItem.of("2", -1, -1), GeoItem.of("3", -20, -20)); // 0 - 3
            geo.geohash(key, "1", "2", "missing"); // 1 - 3 hashes, the last one null
            geo.georadius(key, GeoPosition.of(10, 10), 100, GeoUnit.KM); // 2 - {"1"}
            geo.georadius(key, 10, 10, 2000, GeoUnit.KM, new GeoRadiusArgs().withDistance().ascending()); // 3 - ["1", "2"]
            geo.georadius(key, GeoPosition.of(10, 10), 2000, GeoUnit.KM, new GeoRadiusArgs().withCoordinates()); // 4 - 2 values
            geo.georadius(key, 10, 10, 2000, GeoUnit.KM, new GeoRadiusStoreArgs<String>().storeKey(radiusStore)); // 5 - 2
            geo.georadius(key, GeoPosition.of(10, 10), 100, GeoUnit.KM,
                    new GeoRadiusStoreArgs<String>().storeDistKey(radiusDistStore)); // 6 - 1
            geo.georadiusbymember(key, "3", 3500, GeoUnit.KM); // 7 - {"2", "3"}
            geo.georadiusbymember(key, "3", 3500, GeoUnit.KM, new GeoRadiusArgs().withDistance().descending()); // 8 - ["2", "3"]
            geo.georadiusbymember(key, "3", 3500, GeoUnit.KM, new GeoRadiusStoreArgs<String>().storeKey(byMemberStore)); // 9 - 2
            geo.geosearchstore(searchStore, key,
                    new GeoSearchStoreArgs<String>().fromMember("1").byRadius(5000, GeoUnit.KM).ascending(), true); // 10 - 3
        }, key);
        assertThat(result.size()).isEqualTo(11);
        assertThat(result.discarded()).isFalse();
        assertThat((Integer) result.get(0)).isEqualTo(3);

        List<String> hashes = result.get(1);
        assertThat(hashes).hasSize(3);
        assertThat(hashes.get(0)).isNotBlank();
        assertThat(hashes.get(1)).isNotBlank();
        assertThat(hashes.get(2)).isNull();

        Set<String> near = result.get(2);
        assertThat(near).containsExactly("1");

        List<GeoValue<String>> withDistance = result.get(3);
        assertThat(withDistance).hasSize(2);
        assertThat(withDistance.get(0).member).isEqualTo("1");
        assertThat(withDistance.get(0).distance).hasValueCloseTo(0.0, within(0.01));
        assertThat(withDistance.get(1).member).isEqualTo("2");
        assertThat(withDistance.get(1).distance.orElseThrow()).isBetween(1700.0, 1750.0);

        List<GeoValue<String>> withCoordinates = result.get(4);
        assertThat(withCoordinates).hasSize(2);
        assertThat(withCoordinates).allSatisfy(v -> {
            assertThat(v.longitude).isPresent();
            assertThat(v.latitude).isPresent();
        });

        assertThat((Long) result.get(5)).isEqualTo(2L);
        assertThat((Long) result.get(6)).isEqualTo(1L);

        Set<String> byMember = result.get(7);
        assertThat(byMember).containsExactlyInAnyOrder("2", "3");

        List<GeoValue<String>> byMemberWithDistance = result.get(8);
        assertThat(byMemberWithDistance).hasSize(2);
        assertThat(byMemberWithDistance.get(0).member).isEqualTo("2");
        assertThat(byMemberWithDistance.get(1).member).isEqualTo("3");
        assertThat(byMemberWithDistance.get(1).distance).hasValueCloseTo(0.0, within(0.01));

        assertThat((Long) result.get(9)).isEqualTo(2L);
        assertThat((Long) result.get(10)).isEqualTo(3L);

        // The STORE variants must have written their destination sorted sets.
        assertThat(connection.sync().zcard(radiusStore.getBytes(StandardCharsets.UTF_8))).isEqualTo(2L);
        assertThat(connection.sync().zcard(radiusDistStore.getBytes(StandardCharsets.UTF_8))).isEqualTo(1L);
        assertThat(connection.sync().zcard(byMemberStore.getBytes(StandardCharsets.UTF_8))).isEqualTo(2L);
        assertThat(connection.sync().zcard(searchStore.getBytes(StandardCharsets.UTF_8))).isEqualTo(3L);
    }

    @Test
    void geoReactive() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalGeoCommands<String, String> geo = tx.geo(String.class);
            return geo.geoadd(key, 10, 10, "1") // 0 - true
                    .chain(() -> geo.geoadd(key, GeoItem.of("2", GeoPosition.of(-1, -1)))) // 1 - true
                    .chain(() -> geo.geoadd(key, GeoItem.of("3", -20, -20))) // 2 - true
                    .chain(() -> geo.geodist(key, "1", "3", GeoUnit.KM)) // 3 - some number
                    .chain(() -> geo.geopos(key, "2", "3", "4")) // 4 - list of position
                    .chain(() -> geo.geosearch(key,
                            new GeoSearchArgs<String>().withDistance().ascending().byRadius(10000, GeoUnit.KM))); // 5 - list of geo value
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(6);
        assertThat(result.discarded()).isFalse();
        assertThat((Boolean) result.get(0)).isTrue();
        assertThat((Boolean) result.get(1)).isTrue();
        assertThat((Boolean) result.get(2)).isTrue();
        assertThat((Double) result.get(3)).isPositive();
        List<GeoPosition> list = result.get(4);
        assertThat(list).hasSize(3);
        List<GeoValue<String>> values = result.get(5);
        assertThat(values).hasSize(3);
    }

}
