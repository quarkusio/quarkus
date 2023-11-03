package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.geo.GeoAddArgs;
import io.quarkus.redis.datasource.geo.GeoCommands;
import io.quarkus.redis.datasource.geo.GeoItem;
import io.quarkus.redis.datasource.geo.GeoPosition;
import io.quarkus.redis.datasource.geo.GeoRadiusArgs;
import io.quarkus.redis.datasource.geo.GeoRadiusStoreArgs;
import io.quarkus.redis.datasource.geo.GeoSearchArgs;
import io.quarkus.redis.datasource.geo.GeoSearchStoreArgs;
import io.quarkus.redis.datasource.geo.GeoUnit;
import io.quarkus.redis.datasource.geo.GeoValue;
import io.quarkus.redis.datasource.geo.TransactionalGeoCommands;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

@SuppressWarnings("unchecked")
public class GeoCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    static String key = "key-geo";

    private final static double VALENCE_LONGITUDE = 44.9334;
    private final static double VALENCE_LATITUDE = 4.8924;

    private final static double CRUSSOL_LONGITUDE = 44.9396;
    private final static double CRUSSOL_LATITUDE = 4.8520;

    private final static double GRIGNAN_LONGITUDE = 44.4189;
    private final static double GRIGNAN_LATITUDE = 4.9088;

    private final static double SUZE_LONGITUDE = 44.2899;
    private final static double SUZE_LATITUDE = 4.8383;

    private GeoCommands<String, Place> geo;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));
        geo = ds.geo(Place.class);
    }

    @AfterEach
    public void clear() {
        ds.flushall();
    }

    void populate() {
        geo.geoadd(key,
                GeoItem.of(Place.crussol, CRUSSOL_LONGITUDE, CRUSSOL_LATITUDE),
                GeoItem.of(Place.grignan, GRIGNAN_LONGITUDE, GRIGNAN_LATITUDE),
                GeoItem.of(Place.suze, SUZE_LONGITUDE, SUZE_LATITUDE));
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(geo.getDataSource());
    }

    @Test
    void geoadd() {
        boolean added = geo.geoadd(key, 44.9396, CRUSSOL_LATITUDE, Place.crussol);
        assertThat(added).isTrue();

        added = geo.geoadd(key, 44.9396, CRUSSOL_LATITUDE, Place.crussol);
        assertThat(added).isFalse();

        added = geo.geoadd(key, GeoPosition.of(GRIGNAN_LONGITUDE, GRIGNAN_LATITUDE), Place.grignan);
        assertThat(added).isTrue();

        added = geo.geoadd(key, GeoItem.of(Place.suze, SUZE_LONGITUDE, SUZE_LATITUDE));
        assertThat(added).isTrue();
    }

    @Test
    @RequiresRedis6OrHigher
    void geoaddUsingTypeReferences() {
        var g = ds.geo(new TypeReference<Map<String, Place>>() {
            // Empty on purpose
        });

        boolean added = g.geoadd(key, 44.9396, CRUSSOL_LATITUDE, Map.of("a", Place.crussol));
        assertThat(added).isTrue();

        added = g.geoadd(key, 44.9396, CRUSSOL_LATITUDE, Map.of("a", Place.crussol));
        assertThat(added).isFalse();

        added = g.geoadd(key, GeoPosition.of(GRIGNAN_LONGITUDE, GRIGNAN_LATITUDE), Map.of("a", Place.grignan));
        assertThat(added).isTrue();

        added = g.geoadd(key, GeoItem.of(Map.of("a", Place.suze), SUZE_LONGITUDE, SUZE_LATITUDE));
        assertThat(added).isTrue();

        List<GeoValue<Map<String, Place>>> list = g.geosearch(key, new GeoSearchArgs<Map<String, Place>>()
                .byRadius(60, GeoUnit.KM).fromMember(Map.of("a", Place.crussol)).ascending().withDistance());
        assertThat(list).hasSize(2);
        assertThat(list.get(0).member).isEqualTo(Map.of("a", Place.crussol));
        assertThat(list.get(1).member).isEqualTo(Map.of("a", Place.grignan)); // 58 Km from crussol
    }

    @Test
    @RequiresRedis6OrHigher
    void geoaddWithNx() {
        boolean added = geo.geoadd(key, GeoItem.of(new Place("foo", 1), CRUSSOL_LONGITUDE, CRUSSOL_LATITUDE),
                new GeoAddArgs().nx());
        assertThat(added).isTrue();
    }

    @Test
    void geoaddValue() {
        int added = geo.geoadd(key,
                GeoItem.of(Place.crussol, CRUSSOL_LONGITUDE, CRUSSOL_LATITUDE),
                GeoItem.of(Place.grignan, GRIGNAN_LONGITUDE, GRIGNAN_LATITUDE),
                GeoItem.of(Place.suze, SUZE_LONGITUDE, SUZE_LATITUDE));
        assertThat(added).isEqualTo(3);

        Set<Place> placesNearValence = geo.georadius(key, VALENCE_LONGITUDE, VALENCE_LATITUDE, 15, GeoUnit.KM);
        assertThat(placesNearValence).hasSize(1).contains(Place.crussol);
    }

    @Test
    @RequiresRedis6OrHigher
    void geoAddWithXXorCH() {

        boolean added = geo.geoadd(key, 44.9396, CRUSSOL_LATITUDE, Place.crussol, new GeoAddArgs().xx());
        assertThat(added).isFalse();

        Assertions.assertThat(geo.geoadd(key, GRIGNAN_LONGITUDE, GRIGNAN_LATITUDE, Place.grignan)).isTrue();
        Assertions.assertThat(geo.geoadd(key, SUZE_LONGITUDE, SUZE_LATITUDE, Place.suze)).isTrue();

        int changed = geo.geoadd(key, new GeoAddArgs().ch(), GeoItem.of(Place.crussol, CRUSSOL_LONGITUDE + 1, CRUSSOL_LATITUDE),
                GeoItem.of(Place.grignan, GRIGNAN_LONGITUDE, GRIGNAN_LATITUDE),
                GeoItem.of(Place.suze, SUZE_LONGITUDE, SUZE_LATITUDE));

        assertThat(changed).isEqualTo(1);

        int i = geo.geoadd(key, new GeoAddArgs().nx(),
                GeoItem.of(new Place("new-grignan", 1), GRIGNAN_LONGITUDE, GRIGNAN_LATITUDE));
        assertThat(i).isEqualTo(1);

    }

    @Test
    public void geoaddInTransaction() {
        TransactionResult result = ds.withTransaction(tx -> {
            TransactionalGeoCommands<String, Place> geo = tx.geo(Place.class);
            geo.geoadd(key, CRUSSOL_LONGITUDE, CRUSSOL_LATITUDE, Place.crussol);
            geo.geoadd(key, GRIGNAN_LONGITUDE, GRIGNAN_LATITUDE, Place.grignan);
        });
        assertThat(result.discarded()).isFalse();
        assertThat(result).containsSequence(true, true);
    }

    @Test
    public void geoaddMultiGeoItemsInTransaction() {
        TransactionResult result = ds.withTransaction(tx -> {
            TransactionalGeoCommands<String, Place> geo = tx.geo(Place.class);
            geo.geoadd(key,
                    GeoItem.of(Place.crussol, CRUSSOL_LONGITUDE, CRUSSOL_LATITUDE),
                    GeoItem.of(Place.grignan, GRIGNAN_LONGITUDE, GRIGNAN_LATITUDE),
                    GeoItem.of(Place.suze, SUZE_LONGITUDE, SUZE_LATITUDE));
        }, key);
        assertThat(result).contains(3);
    }

    @Test
    void georadius() {
        populate();
        Set<Place> places = geo.georadius(key, 44.9396, CRUSSOL_LATITUDE, 1, GeoUnit.KM);
        assertThat(places).hasSize(1).contains(Place.crussol);

        places = geo.georadius(key, 44.9396, CRUSSOL_LATITUDE, 60, GeoUnit.KM);
        assertThat(places).hasSize(2).containsExactlyInAnyOrder(Place.crussol, Place.grignan);

        List<GeoValue<Place>> list = geo.georadius(key, 44.9396, CRUSSOL_LATITUDE, 60, GeoUnit.KM,
                new GeoRadiusArgs().ascending().withDistance());
        assertThat(list).hasSize(2);
        assertThat(list.get(0).member).isEqualTo(Place.crussol);
        assertThat(list.get(1).member).isEqualTo(Place.grignan); // 58 Km from crussol
    }

    @Test
    void georadiusUsingGeoPosition() {
        populate();
        Set<Place> places = geo.georadius(key, GeoPosition.of(44.9396, CRUSSOL_LATITUDE), 1, GeoUnit.KM);
        assertThat(places).hasSize(1).contains(Place.crussol);

        places = geo.georadius(key, GeoPosition.of(44.9396, CRUSSOL_LATITUDE), 60, GeoUnit.KM);
        assertThat(places).hasSize(2).containsExactlyInAnyOrder(Place.crussol, Place.grignan);

        List<GeoValue<Place>> list = geo.georadius(key, GeoPosition.of(44.9396, CRUSSOL_LATITUDE),
                60, GeoUnit.KM,
                new GeoRadiusArgs().ascending().withDistance());
        assertThat(list).hasSize(2);
        assertThat(list.get(0).member).isEqualTo(Place.crussol);
        assertThat(list.get(1).member).isEqualTo(Place.grignan); // 58 Km from crussol
    }

    @Test
    public void georadiusInTransaction() {
        populate();

        TransactionResult result = ds.withTransaction(tx -> {
            TransactionalGeoCommands<String, Place> geo = tx.geo(Place.class);
            geo.georadius(key, CRUSSOL_LONGITUDE, CRUSSOL_LATITUDE, 1, GeoUnit.KM);
            geo.georadius(key, CRUSSOL_LONGITUDE, CRUSSOL_LATITUDE, 60, GeoUnit.KM);
        });

        Set<Place> georadius = result.get(0);
        Set<Place> largerGeoradius = result.get(1);

        assertThat(georadius).hasSize(1).contains(Place.crussol);
        assertThat(largerGeoradius).hasSize(2).contains(Place.crussol).contains(Place.grignan);
    }

    @Test
    void georadiusWithCoords() {
        populate();
        List<GeoValue<Place>> georadius = geo.georadius(key, VALENCE_LONGITUDE, VALENCE_LATITUDE, 100, GeoUnit.KM,
                new GeoRadiusArgs()
                        .withCoordinates());
        assertThat(georadius).hasSize(3);
        assertThat(getLongitudeOrDie(georadius, 0)).isEqualTo(44.2, Offset.offset(0.5));
        assertThat(getLatitudeOrDie(georadius, 0)).isEqualTo(4.8, Offset.offset(0.5));

        assertThat(getLongitudeOrDie(georadius, 2)).isEqualTo(44.9, Offset.offset(0.5));
        assertThat(getLatitudeOrDie(georadius, 2)).isEqualTo(4.8, Offset.offset(0.5));
    }

    @Test
    void geodist() {
        populate();
        OptionalDouble result = geo.geodist(key, Place.crussol, Place.grignan, GeoUnit.KM);
        assertThat(result).hasValueCloseTo(50.0, Offset.offset(10.0));
    }

    @Test
    void geodistMissingElements() {
        populate();

        OptionalDouble v = geo.geodist(key, new Place("unknown", 1), Place.crussol, GeoUnit.KM);
        assertThat(v).isEmpty();
        v = geo.geodist(key, Place.suze, new Place("unknown", 1), GeoUnit.KM);
        assertThat(v).isEmpty();
    }

    @Test
    public void geopos() {
        populate();

        List<GeoPosition> geopos = geo.geopos(key, Place.crussol);
        assertThat(geopos).hasSize(1);
        assertThat(geopos.get(0).longitude()).isEqualTo(CRUSSOL_LONGITUDE, offset(0.001));
        assertThat(geopos.get(0).latitude()).isEqualTo(CRUSSOL_LATITUDE, offset(0.001));

        geopos = geo.geopos(key, Place.crussol, new Place("missing", 0), Place.grignan);
        assertThat(geopos).hasSize(3);
        assertThat(geopos.get(0).longitude()).isEqualTo(CRUSSOL_LONGITUDE, offset(0.001));
        assertThat(geopos.get(0).latitude()).isEqualTo(CRUSSOL_LATITUDE, offset(0.001));
        assertThat(geopos.get(1)).isNull();
        assertThat(geopos.get(2)).isNotNull();
    }

    @Test
    void georadiusWithArgs() {
        populate();

        GeoRadiusArgs geoArgs = new GeoRadiusArgs().withHash().withCoordinates().withDistance().count(1).descending();

        List<GeoValue<Place>> result = geo.georadius(key, VALENCE_LONGITUDE, VALENCE_LATITUDE, 5, GeoUnit.KM, geoArgs);
        assertThat(result).hasSize(1);

        GeoValue<Place> place = result.get(0);
        assertThat(place.member).isEqualTo(Place.crussol);
        assertThat(place.geohash).hasValue(3426059562325044L);
        assertThat(place.longitude).hasValueCloseTo(CRUSSOL_LONGITUDE, offset(0.1));
        assertThat(place.latitude).hasValueCloseTo(CRUSSOL_LATITUDE, offset(0.1));
        assertThat(place.distance).hasValueCloseTo(5.0, offset(5.0));

        result = geo.georadius(key, VALENCE_LONGITUDE, VALENCE_LATITUDE, 5, GeoUnit.KM, new GeoRadiusArgs());
        assertThat(result).hasSize(1);
        place = result.get(0);
        assertThat(place.member).isEqualTo(Place.crussol);
        assertThat(place.geohash).isEmpty();
        assertThat(place.distance).isEmpty();
        assertThat(place.latitude).isEmpty();
        assertThat(place.longitude).isEmpty();
    }

    @Test
    void geohash() {
        populate();
        List<String> geohash = geo.geohash(key, Place.crussol, Place.suze, new Place("unknown", 0));

        assertThat(geohash).hasSize(3);
        assertThat(geohash.get(0)).isNotBlank();
        assertThat(geohash.get(1)).isNotBlank();
        assertThat(geohash.get(2)).isNull();
    }

    @Test
    void georadiusStore() {
        populate();
        String resultKey = key + "-2";
        long result = geo.georadius(key, VALENCE_LONGITUDE, VALENCE_LATITUDE, 60.0, GeoUnit.KM,
                new GeoRadiusStoreArgs<String>().storeKey(resultKey));
        assertThat(result).isEqualTo(2L);
        List<ScoredValue<Place>> results = ds.sortedSet(String.class, Place.class).zrangeWithScores(resultKey, 0, -1);
        assertThat(results).hasSize(2);
    }

    @Test
    void georadiusStoreWithGeoPosition() {
        populate();
        String resultKey = key + "-2";
        long result = geo.georadius(key, GeoPosition.of(VALENCE_LONGITUDE, VALENCE_LATITUDE), 60.0, GeoUnit.KM,
                new GeoRadiusStoreArgs<String>().storeKey(resultKey));
        assertThat(result).isEqualTo(2L);
        List<ScoredValue<Place>> results = ds.sortedSet(String.class, Place.class).zrangeWithScores(resultKey, 0, -1);
        assertThat(results).hasSize(2);
    }

    @Test
    void georadiusStoreWithCountAndSort() {
        populate();
        String resultKey = key + "-2";
        long result = geo.georadius(key, VALENCE_LONGITUDE, VALENCE_LATITUDE, 100, GeoUnit.KM,
                new GeoRadiusStoreArgs<String>().count(2).descending().storeKey(resultKey));
        assertThat(result).isEqualTo(2);
        List<ScoredValue<Place>> results = ds.sortedSet(String.class, Place.class).zrangeWithScores(resultKey, 0, -1);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).score()).isGreaterThan(99999);
    }

    @Test
    void georadiusStoreDist() {
        populate();
        String resultKey = key + "-2";
        long result = geo.georadius(key, VALENCE_LONGITUDE, VALENCE_LATITUDE, 60, GeoUnit.KM,
                new GeoRadiusStoreArgs<String>().storeDistKey(resultKey));
        assertThat(result).isEqualTo(2);
        List<ScoredValue<Place>> dist = ds.sortedSet(String.class, Place.class).zrangeWithScores(resultKey, 0, -1);
        assertThat(dist).hasSize(2);
    }

    @Test
    void georadiusStoreDistWithCountAndSort() {
        populate();
        String resultKey = key + "-2";
        Long result = geo.georadius(key, VALENCE_LONGITUDE, VALENCE_LATITUDE, 10, GeoUnit.KM,
                new GeoRadiusStoreArgs<String>().count(1).descending().storeDistKey(resultKey));
        assertThat(result).isEqualTo(1);

        List<ScoredValue<Place>> dist = ds.sortedSet(String.class, Place.class).zrangeWithScores(resultKey, 0, -1);
        assertThat(dist).hasSize(1);
        assertThat(dist.get(0).score()).isBetween(3.0, 5.0);
    }

    @Test
    void georadiusWithNullArgs() {
        assertThatThrownBy(() -> geo.georadius(key, VALENCE_LONGITUDE, VALENCE_LATITUDE, 5, GeoUnit.KM, (GeoRadiusArgs) null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
                () -> geo.georadius(key, VALENCE_LONGITUDE, VALENCE_LATITUDE, 5, GeoUnit.KM, (GeoRadiusStoreArgs<String>) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void georadiusbymember() {
        populate();
        Set<Place> match = geo.georadiusbymember(key, Place.crussol, 1, GeoUnit.KM);
        assertThat(match).hasSize(1).contains(Place.crussol);

        Set<Place> georadiusbymember = geo.georadiusbymember(key, Place.crussol, 60, GeoUnit.KM);
        assertThat(georadiusbymember).hasSize(2).contains(Place.crussol, Place.grignan);
    }

    @Test
    @RequiresRedis6OrHigher
    void georadiusbymemberStoreDistWithCountAndSort() {
        populate();
        String resultKey = key + "-2";
        long result = geo.georadiusbymember(key, Place.crussol, 100, GeoUnit.KM,
                new GeoRadiusStoreArgs<String>().count(2).any().descending().storeDistKey(resultKey));
        assertThat(result).isEqualTo(2);

        SortedSetCommands<String, String> commands = ds.sortedSet(String.class);
        List<ScoredValue<String>> dist = commands.zrangeWithScores(resultKey, 0, -1);
        assertThat(dist).hasSize(2);
        assertThat(dist.get(0).score()).isBetween(55d, 60d);
    }

    @Test
    void georadiusbymemberStoreDistWithSort() {
        populate();
        String resultKey = key + "-2";
        long result = geo.georadiusbymember(key, Place.crussol, 100, GeoUnit.KM,
                new GeoRadiusStoreArgs<String>().descending().storeDistKey(resultKey));
        assertThat(result).isEqualTo(3);

        SortedSetCommands<String, String> commands = ds.sortedSet(String.class);
        List<ScoredValue<String>> dist = commands.zrangeWithScores(resultKey, 0, -1);
        assertThat(dist).hasSize(3);
    }

    @Test
    void georadiusbymemberWithArgs() {
        populate();

        List<GeoValue<Place>> match = geo.georadiusbymember(key, Place.crussol, 1, GeoUnit.KM, new GeoRadiusArgs()
                .withHash().withCoordinates().withDistance().descending());
        assertThat(match).isNotEmpty();

        List<GeoValue<Place>> withDistanceAndCoordinates = geo.georadiusbymember(key, Place.crussol, 60, GeoUnit.KM,
                new GeoRadiusArgs().withCoordinates().withDistance().descending());
        assertThat(withDistanceAndCoordinates).hasSize(2);

        GeoValue<Place> p = withDistanceAndCoordinates.get(0);
        assertThat(p.member).isEqualTo(Place.grignan);
        assertThat(p.geohash).isEmpty();
        assertThat(p.distance).isNotEmpty();
        assertThat(p.latitude).isNotEmpty();
        assertThat(p.longitude).isNotEmpty();

        p = withDistanceAndCoordinates.get(1);
        assertThat(p.member).isEqualTo(Place.crussol);
        assertThat(p.geohash).isEmpty();
        assertThat(p.distance).isNotEmpty();
        assertThat(p.latitude).isNotEmpty();
        assertThat(p.longitude).isNotEmpty();

        List<GeoValue<Place>> withDistanceAndHash = geo.georadiusbymember(key, Place.crussol,
                60, GeoUnit.KM, new GeoRadiusArgs().withDistance().withHash().descending());
        assertThat(withDistanceAndHash).hasSize(2);

        p = withDistanceAndHash.get(0);
        assertThat(p.member).isEqualTo(Place.grignan);
        assertThat(p.geohash).isNotEmpty();
        assertThat(p.distance).isNotEmpty();
        assertThat(p.latitude).isEmpty();
        assertThat(p.longitude).isEmpty();

        p = withDistanceAndHash.get(1);
        assertThat(p.member).isEqualTo(Place.crussol);
        assertThat(p.geohash).isNotEmpty();
        assertThat(p.distance).isNotEmpty();
        assertThat(p.latitude).isEmpty();
        assertThat(p.longitude).isEmpty();

        List<GeoValue<Place>> withCoordinates = geo.georadiusbymember(key, Place.crussol, 60, GeoUnit.KM,
                new GeoRadiusArgs().withCoordinates().descending());
        assertThat(withCoordinates).hasSize(2);

        p = withCoordinates.get(0);
        assertThat(p.member).isEqualTo(Place.grignan);
        assertThat(p.geohash).isEmpty();
        assertThat(p.distance).isEmpty();
        assertThat(p.latitude).isNotEmpty();
        assertThat(p.longitude).isNotEmpty();
    }

    @Test
    void georadiusbymemberWithNullArgs() {
        assertThatThrownBy(() -> geo.georadiusbymember(key, Place.crussol, 1, GeoUnit.KM, (GeoRadiusArgs) null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> geo.georadiusbymember(key, Place.crussol, 1, GeoUnit.KM, (GeoRadiusStoreArgs<String>) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @RequiresRedis6OrHigher
    void geosearchWithCountAndSort() {
        populate();

        GeoSearchArgs<Place> args = new GeoSearchArgs<Place>().fromMember(Place.crussol)
                .byRadius(5, GeoUnit.KM);
        List<GeoValue<Place>> places = geo.geosearch(key, args);
        assertThat(places).hasSize(1).allSatisfy(gv -> assertThat(gv.member).isEqualTo(Place.crussol));

        places = geo.geosearch(key, new GeoSearchArgs<Place>().fromMember(Place.crussol)
                .byRadius(60, GeoUnit.KM));
        assertThat(places).hasSize(2);

        places = geo.geosearch(key, new GeoSearchArgs<Place>().fromMember(Place.crussol)
                .byBox(140, 140, GeoUnit.KM)); // grigan but not suze
        assertThat(places).hasSize(2);
    }

    @Test
    @RequiresRedis6OrHigher
    void geosearchWithArgs() {
        populate();

        GeoSearchArgs<Place> args = new GeoSearchArgs<Place>().fromMember(Place.crussol)
                .byRadius(5, GeoUnit.KM).withCoordinates().withDistance().descending();
        List<GeoValue<Place>> places = geo.geosearch(key, args);
        assertThat(places).hasSize(1).allSatisfy(gv -> {
            assertThat(gv.member).isEqualTo(Place.crussol);
            assertThat(gv.longitude).isNotEmpty();
            assertThat(gv.latitude).isNotEmpty();
            assertThat(gv.distance).isNotEmpty();
            assertThat(gv.geohash).isEmpty();
        });

        places = geo.geosearch(key, new GeoSearchArgs<Place>().fromMember(Place.crussol)
                .byRadius(60, GeoUnit.KM).withDistance().withCoordinates());
        assertThat(places).hasSize(2).allSatisfy(gv -> {
            assertThat(gv.member).isIn(Place.crussol, Place.grignan);
            assertThat(gv.longitude).isNotEmpty();
            assertThat(gv.latitude).isNotEmpty();
            assertThat(gv.distance).isNotEmpty();
            assertThat(gv.geohash).isEmpty();
        });

        places = geo.geosearch(key, new GeoSearchArgs<Place>().fromMember(Place.crussol)
                .byBox(140, 140, GeoUnit.KM).withCoordinates().withDistance()); // grigan but not suze
        assertThat(places).hasSize(2).allSatisfy(gv -> {
            assertThat(gv.member).isIn(Place.crussol, Place.grignan);
            assertThat(gv.longitude).isNotEmpty();
            assertThat(gv.latitude).isNotEmpty();
            assertThat(gv.distance).isNotEmpty();
            assertThat(gv.geohash).isEmpty();
        });

        args = new GeoSearchArgs<Place>().fromCoordinate(CRUSSOL_LONGITUDE, CRUSSOL_LATITUDE)
                .byRadius(5, GeoUnit.KM).withCoordinates().withDistance().descending();
        places = geo.geosearch(key, args);
        assertThat(places).hasSize(1).allSatisfy(gv -> {
            assertThat(gv.member).isEqualTo(Place.crussol);
            assertThat(gv.longitude).isNotEmpty();
            assertThat(gv.latitude).isNotEmpty();
            assertThat(gv.distance).isNotEmpty();
            assertThat(gv.geohash).isEmpty();
        });

    }

    @Test
    @RequiresRedis6OrHigher
    void geosearchStoreWithCountAndSort() {
        populate();
        String resultKey = key + "-2";
        String resultKey2 = key + "-3";

        GeoSearchStoreArgs<Place> args = new GeoSearchStoreArgs<Place>().fromMember(Place.crussol)
                .byRadius(60, GeoUnit.KM).descending();
        long count = geo.geosearchstore(resultKey, key, args, true);
        assertThat(count).isEqualTo(2);

        args = new GeoSearchStoreArgs<Place>().fromMember(Place.crussol)
                .byRadius(200, GeoUnit.KM).count(2).descending();
        count = geo.geosearchstore(resultKey2, key, args, true);
        assertThat(count).isEqualTo(2);
    }

    private static double getLatitudeOrDie(List<GeoValue<Place>> georadius, int index) {
        return georadius.get(index).latitude.orElseThrow();
    }

    private static double getLongitudeOrDie(List<GeoValue<Place>> georadius, int index) {
        return georadius.get(index).longitude.orElseThrow();
    }

}
