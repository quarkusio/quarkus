package io.quarkus.redis.it.lettuce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;

@QuarkusTest
class LettuceBackendTest {

    String getKey(String k) {
        return k;
    }

    @Test
    public void ping() {
        RestAssured.given()
                .when()
                .get("/lettuce/ping")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("PONG"));
    }

    @Test
    public void pingCommand() {
        RestAssured.given()
                .when()
                .get("/lettuce/ping/command")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("PONG"));
    }

    @Test
    public void securePing() {
        RestAssured.given()
                .when()
                .get("/lettuce/secure/ping")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("PONG"));
    }

    @Test
    public void clusterPing() {
        RestAssured.given()
                .when()
                .get("/lettuce/cluster/ping")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("PONG"));
    }

    @Test
    public void clusterTopologyIsDiscoveredFromTheSeeds() {
        // three seeds are configured, the three replicas are discovered
        RestAssured.given()
                .when()
                .get("/lettuce/cluster/topology")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("3,3"));
    }

    @Test
    public void clusterValueSetGet() {
        // keys hashing to different slots, so they live on different nodes
        for (int i = 0; i < 10; i++) {
            String key = getKey("cluster-value-" + i);
            RestAssured.given().body("v" + i).when().post("/lettuce/cluster/value/" + key).then().statusCode(204);
        }
        for (int i = 0; i < 10; i++) {
            String key = getKey("cluster-value-" + i);
            RestAssured.given().when().get("/lettuce/cluster/value/" + key).then()
                    .statusCode(200).body(CoreMatchers.is("v" + i));
        }
    }

    @Test
    public void clusterKeyScan() {
        String prefix = getKey("cluster-scan-");
        for (int i = 0; i < 5; i++) {
            RestAssured.given().body("v").when().post("/lettuce/cluster/value/" + prefix + i).then().statusCode(204);
        }
        RestAssured.given().queryParam("match", prefix + "*").when().get("/lettuce/cluster/key/scan").then()
                .statusCode(200)
                .body("$", CoreMatchers.hasItems(prefix + "0", prefix + "1", prefix + "2", prefix + "3", prefix + "4"))
                .body("size()", CoreMatchers.is(5));
    }

    @Test
    public void clusterRejectsTransactions() {
        String key = getKey("cluster-tx");
        RestAssured.given().body("v").when().post("/lettuce/cluster/with-transaction/" + key).then()
                .statusCode(200)
                .body(CoreMatchers.containsString("not supported on a Redis cluster"));
        RestAssured.given().when().get("/lettuce/cluster/value/" + key).then().statusCode(204);
    }

    @Test
    public void sentinelPing() {
        RestAssured.given().when().get("/lettuce/sentinel/ping").then()
                .statusCode(200).body(CoreMatchers.is("PONG"));
    }

    @Test
    public void sentinelConnectsToTheMaster() {
        RestAssured.given().when().get("/lettuce/sentinel/role").then()
                .statusCode(200).body(CoreMatchers.is("master"));
    }

    @Test
    public void sentinelValueSetGet() {
        String key = getKey("sentinel-value");
        RestAssured.given().body("s1").when().post("/lettuce/sentinel/value/" + key).then().statusCode(204);
        RestAssured.given().when().get("/lettuce/sentinel/value/" + key).then()
                .statusCode(200).body(CoreMatchers.is("s1"));
    }

    @Test
    public void replicationPing() {
        RestAssured.given().when().get("/lettuce/replication/ping").then()
                .statusCode(200).body(CoreMatchers.is("PONG"));
    }

    @Test
    public void replicationWritesToTheMaster() {
        String key = getKey("replication-value");
        RestAssured.given().when().get("/lettuce/replication/role").then()
                .statusCode(200).body(CoreMatchers.is("master"));
        RestAssured.given().body("r1").when().post("/lettuce/replication/value/" + key).then().statusCode(204);
        RestAssured.given().when().get("/lettuce/replication/value/" + key).then()
                .statusCode(200).body(CoreMatchers.is("r1"));
    }

    @Test
    public void dataSourcesAreServedByLettuce() {
        RestAssured.given()
                .when()
                .get("/lettuce/backend")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("lettuce"));
    }

    @Test
    public void valueSetGet() {
        String key = getKey("value-sync");
        String value = "lettuce-value";

        RestAssured.given()
                .when()
                .get("/lettuce/value/" + key)
                .then()
                .statusCode(204);

        RestAssured.given()
                .body(value)
                .when()
                .post("/lettuce/value/" + key)
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .get("/lettuce/value/" + key)
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(value));
    }

    @Test
    public void valueLcs() {
        String key1 = getKey("lcs-1");
        String key2 = getKey("lcs-2");

        RestAssured.given()
                .body("ohmytext")
                .when()
                .post("/lettuce/value/" + key1)
                .then()
                .statusCode(204);

        RestAssured.given()
                .body("mynewtext")
                .when()
                .post("/lettuce/value/" + key2)
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .get("/lettuce/value/lcs/" + key1 + "/" + key2)
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("mytext,6"));
    }

    @Test
    public void valueGetReactive() {
        String key = getKey("value-reactive");
        String value = "lettuce-reactive";

        RestAssured.given()
                .body(value)
                .when()
                .post("/lettuce/value/" + key)
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .get("/lettuce/reactive/" + key)
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(value));
    }

    @Test
    public void selectChangesDatabase() {
        String key = getKey("select-key");
        try {
            RestAssured.given().body("db0").when().post("/lettuce/value/" + key).then().statusCode(204);

            RestAssured.given().when().post("/lettuce/select/1").then().statusCode(204);

            RestAssured.given().when().get("/lettuce/value/" + key).then().statusCode(204);

            RestAssured.given().body("db1").when().post("/lettuce/value/" + key).then().statusCode(204);
            RestAssured.given().when().get("/lettuce/value/" + key).then()
                    .statusCode(200).body(CoreMatchers.is("db1"));

            RestAssured.given().when().post("/lettuce/select/0").then().statusCode(204);
            RestAssured.given().when().get("/lettuce/value/" + key).then()
                    .statusCode(200).body(CoreMatchers.is("db0"));
        } finally {
            RestAssured.given().when().post("/lettuce/select/1").then().statusCode(204);
            RestAssured.given().when().delete("/lettuce/flushall").then().statusCode(204);
            RestAssured.given().when().post("/lettuce/select/0").then().statusCode(204);
        }
    }

    @Test
    public void flushall() {
        String key = getKey("flush-key");

        RestAssured.given()
                .body("to-be-flushed")
                .when()
                .post("/lettuce/value/" + key)
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .delete("/lettuce/flushall")
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .get("/lettuce/value/" + key)
                .then()
                .statusCode(204);
    }

    @Test
    public void keyExistsAndDel() {
        String key = getKey("key-exists");
        RestAssured.given().body("v").when().post("/lettuce/value/" + key).then().statusCode(204);

        RestAssured.given().when().get("/lettuce/key/exists/" + key).then()
                .statusCode(200).body(CoreMatchers.is("true"));

        RestAssured.given().when().delete("/lettuce/key/" + key).then()
                .statusCode(200).body(CoreMatchers.is("1"));

        RestAssured.given().when().get("/lettuce/key/exists/" + key).then()
                .statusCode(200).body(CoreMatchers.is("false"));
    }

    @Test
    public void keyExpireTtlPersist() {
        String key = getKey("key-ttl");
        RestAssured.given().body("v").when().post("/lettuce/value/" + key).then().statusCode(204);

        RestAssured.given().when().post("/lettuce/key/expire/" + key + "/100").then()
                .statusCode(200).body(CoreMatchers.is("true"));

        long ttl = Long.parseLong(RestAssured.given().when().get("/lettuce/key/ttl/" + key)
                .then().statusCode(200).extract().asString());
        assert ttl > 0 && ttl <= 100 : "expected ttl in (0, 100], got " + ttl;

        RestAssured.given().when().post("/lettuce/key/persist/" + key).then()
                .statusCode(200).body(CoreMatchers.is("true"));

        RestAssured.given().when().get("/lettuce/key/ttl/" + key).then()
                .statusCode(200).body(CoreMatchers.is("-1"));
    }

    @Test
    public void keyReactiveTtl() {
        String key = getKey("key-reactive-ttl");
        RestAssured.given().body("v").when().post("/lettuce/value/" + key).then().statusCode(204);
        RestAssured.given().when().post("/lettuce/key/expire/" + key + "/200").then().statusCode(200);

        long ttl = Long.parseLong(RestAssured.given().when().get("/lettuce/key/reactive/ttl/" + key)
                .then().statusCode(200).extract().asString());
        assert ttl > 0 && ttl <= 200 : "expected ttl in (0, 200], got " + ttl;
    }

    @Test
    public void keyRenameAndCopy() {
        String src = getKey("key-src");
        String dst = getKey("key-dst");
        String copyDst = getKey("key-copy-dst");
        RestAssured.given().body("payload").when().post("/lettuce/value/" + src).then().statusCode(204);

        RestAssured.given().when().post("/lettuce/key/copy/" + src + "/" + copyDst).then()
                .statusCode(200).body(CoreMatchers.is("true"));
        RestAssured.given().when().get("/lettuce/value/" + copyDst).then()
                .statusCode(200).body(CoreMatchers.is("payload"));

        RestAssured.given().when().post("/lettuce/key/rename/" + src + "/" + dst).then()
                .statusCode(204);
        RestAssured.given().when().get("/lettuce/key/exists/" + src).then()
                .statusCode(200).body(CoreMatchers.is("false"));
        RestAssured.given().when().get("/lettuce/value/" + dst).then()
                .statusCode(200).body(CoreMatchers.is("payload"));
    }

    @Test
    public void keyType() {
        String key = getKey("key-type");
        RestAssured.given().body("v").when().post("/lettuce/value/" + key).then().statusCode(204);
        RestAssured.given().when().get("/lettuce/key/type/" + key).then()
                .statusCode(200).body(CoreMatchers.is("STRING"));

        String missing = getKey("key-type-missing");
        RestAssured.given().when().get("/lettuce/key/type/" + missing).then()
                .statusCode(200).body(CoreMatchers.is("NONE"));
    }

    @Test
    public void keyScan() {
        String prefix = getKey("scan-");
        for (int i = 0; i < 5; i++) {
            RestAssured.given().body("v").when().post("/lettuce/value/" + prefix + i).then().statusCode(204);
        }
        RestAssured.given().queryParam("match", prefix + "*").when().get("/lettuce/key/scan").then()
                .statusCode(200)
                .body("$", CoreMatchers.hasItems(prefix + "0", prefix + "1", prefix + "2", prefix + "3", prefix + "4"));
    }

    @Test
    public void hashSetGetAll() {
        String key = getKey("hash-sync");

        RestAssured.given().when().get("/lettuce/hash/" + key + "/f1").then()
                .statusCode(204);

        RestAssured.given().body("v1").when().post("/lettuce/hash/" + key + "/f1").then()
                .statusCode(200).body(CoreMatchers.is("true"));
        RestAssured.given().body("v1-updated").when().post("/lettuce/hash/" + key + "/f1").then()
                .statusCode(200).body(CoreMatchers.is("false"));
        RestAssured.given().body("v2").when().post("/lettuce/hash/" + key + "/f2").then()
                .statusCode(200).body(CoreMatchers.is("true"));

        RestAssured.given().when().get("/lettuce/hash/" + key + "/f1").then()
                .statusCode(200).body(CoreMatchers.is("v1-updated"));

        RestAssured.given().when().get("/lettuce/hash/" + key).then()
                .statusCode(200)
                .body("f1", CoreMatchers.is("v1-updated"))
                .body("f2", CoreMatchers.is("v2"))
                .body("size()", CoreMatchers.is(2));
    }

    @Test
    public void hashGetReactive() {
        String key = getKey("hash-reactive");

        RestAssured.given().body("rv").when().post("/lettuce/hash/" + key + "/field").then()
                .statusCode(200).body(CoreMatchers.is("true"));

        RestAssured.given().when().get("/lettuce/hash/reactive/" + key + "/field").then()
                .statusCode(200).body(CoreMatchers.is("rv"));
    }

    @Test
    public void listPushAndRange() {
        String key = getKey("list-sync");

        RestAssured.given().when().get("/lettuce/list/" + key).then()
                .statusCode(200).body("$", CoreMatchers.equalTo(List.of()));

        RestAssured.given().body("a").when().post("/lettuce/list/" + key).then()
                .statusCode(200).body(CoreMatchers.is("1"));
        RestAssured.given().body("b").when().post("/lettuce/list/" + key).then()
                .statusCode(200).body(CoreMatchers.is("2"));
        RestAssured.given().body("c").when().post("/lettuce/list/" + key).then()
                .statusCode(200).body(CoreMatchers.is("3"));

        RestAssured.given().when().get("/lettuce/list/" + key).then()
                .statusCode(200)
                .body("$", CoreMatchers.equalTo(List.of("c", "b", "a")));
    }

    @Test
    public void listRangeReactive() {
        String key = getKey("list-reactive");

        RestAssured.given().body("x").when().post("/lettuce/list/" + key).then().statusCode(200);
        RestAssured.given().body("y").when().post("/lettuce/list/" + key).then().statusCode(200);

        RestAssured.given().when().get("/lettuce/list/reactive/" + key).then()
                .statusCode(200)
                .body("$", CoreMatchers.equalTo(List.of("y", "x")));
    }

    @Test
    public void setAddMembersAndIsMember() {
        String key = getKey("set-sync");

        RestAssured.given().when().get("/lettuce/set/" + key).then()
                .statusCode(200).body("$", CoreMatchers.equalTo(List.of()));

        RestAssured.given().body("a").when().post("/lettuce/set/" + key).then()
                .statusCode(200).body(CoreMatchers.is("1"));
        RestAssured.given().body("a").when().post("/lettuce/set/" + key).then()
                .statusCode(200).body(CoreMatchers.is("0"));
        RestAssured.given().body("b").when().post("/lettuce/set/" + key).then()
                .statusCode(200).body(CoreMatchers.is("1"));

        RestAssured.given().when().get("/lettuce/set/" + key).then()
                .statusCode(200)
                .body("$", CoreMatchers.hasItems("a", "b"))
                .body("size()", CoreMatchers.is(2));

        RestAssured.given().when().get("/lettuce/set/ismember/" + key + "/a").then()
                .statusCode(200).body(CoreMatchers.is("true"));
        RestAssured.given().when().get("/lettuce/set/ismember/" + key + "/missing").then()
                .statusCode(200).body(CoreMatchers.is("false"));
    }

    @Test
    public void setCardReactive() {
        String key = getKey("set-reactive");

        RestAssured.given().when().get("/lettuce/set/reactive/" + key).then()
                .statusCode(200).body(CoreMatchers.is("0"));

        RestAssured.given().body("x").when().post("/lettuce/set/" + key).then().statusCode(200);
        RestAssured.given().body("y").when().post("/lettuce/set/" + key).then().statusCode(200);

        RestAssured.given().when().get("/lettuce/set/reactive/" + key).then()
                .statusCode(200).body(CoreMatchers.is("2"));
    }

    @Test
    public void sortedSetAddCardScoreRank() {
        String key = getKey("zset-sync");

        RestAssured.given().when().get("/lettuce/sortedset/score/" + key + "/a").then()
                .statusCode(204);

        RestAssured.given().body("a").when().post("/lettuce/sortedset/add/" + key + "/1.0").then()
                .statusCode(200).body(CoreMatchers.is("true"));
        RestAssured.given().body("a").when().post("/lettuce/sortedset/add/" + key + "/1.0").then()
                .statusCode(200).body(CoreMatchers.is("false"));
        RestAssured.given().body("b").when().post("/lettuce/sortedset/add/" + key + "/2.5").then()
                .statusCode(200).body(CoreMatchers.is("true"));

        RestAssured.given().when().get("/lettuce/sortedset/card/" + key).then()
                .statusCode(200).body(CoreMatchers.is("2"));
        RestAssured.given().when().get("/lettuce/sortedset/score/" + key + "/b").then()
                .statusCode(200).body(CoreMatchers.is("2.5"));
        RestAssured.given().when().get("/lettuce/sortedset/rank/" + key + "/a").then()
                .statusCode(200).body(CoreMatchers.is("0"));
        RestAssured.given().when().get("/lettuce/sortedset/rank/" + key + "/b").then()
                .statusCode(200).body(CoreMatchers.is("1"));
        RestAssured.given().when().get("/lettuce/sortedset/rank/" + key + "/missing").then()
                .statusCode(204);
    }

    @Test
    public void sortedSetPopMin() {
        String key = getKey("zset-popmin");

        RestAssured.given().body("low").when().post("/lettuce/sortedset/add/" + key + "/1.0").then()
                .statusCode(200).body(CoreMatchers.is("true"));
        RestAssured.given().body("high").when().post("/lettuce/sortedset/add/" + key + "/9.0").then()
                .statusCode(200).body(CoreMatchers.is("true"));

        RestAssured.given().when().post("/lettuce/sortedset/popmin/" + key).then()
                .statusCode(200).body(CoreMatchers.is("low,1.0"));
        RestAssured.given().when().get("/lettuce/sortedset/card/" + key).then()
                .statusCode(200).body(CoreMatchers.is("1"));
    }

    @Test
    public void sortedSetRangeCommands() {
        String key = getKey("zset-range");
        String dest = getKey("zset-range-dest");

        RestAssured.given().body("a").when().post("/lettuce/sortedset/add/" + key + "/1.0").then().statusCode(200);
        RestAssured.given().body("b").when().post("/lettuce/sortedset/add/" + key + "/2.0").then().statusCode(200);
        RestAssured.given().body("c").when().post("/lettuce/sortedset/add/" + key + "/3.0").then().statusCode(200);
        RestAssured.given().body("d").when().post("/lettuce/sortedset/add/" + key + "/4.0").then().statusCode(200);

        // ZRANGE ... REV orders from the highest score down
        RestAssured.given().when().get("/lettuce/sortedset/range/" + key + "/0/-1?rev=true").then()
                .statusCode(200).body("$", CoreMatchers.equalTo(List.of("d", "c", "b", "a")));
        // ZRANGE ... BYSCORE LIMIT 1 2 skips the lowest match and then takes two
        RestAssured.given().when().get("/lettuce/sortedset/rangebyscore/" + key + "/1.0/4.0?offset=1&count=2").then()
                .statusCode(200).body("$", CoreMatchers.equalTo(List.of("b", "c")));
        // ZRANGESTORE copies the two lowest-ranked members into the destination
        RestAssured.given().when().post("/lettuce/sortedset/rangestore/" + dest + "/" + key + "/0/1").then()
                .statusCode(200).body(CoreMatchers.is("2"));
        RestAssured.given().when().get("/lettuce/sortedset/range/" + dest + "/0/-1").then()
                .statusCode(200).body("$", CoreMatchers.equalTo(List.of("a", "b")));
    }

    @Test
    public void sortedSetScoreReactive() {
        String key = getKey("zset-reactive");

        RestAssured.given().body("m").when().post("/lettuce/sortedset/add/" + key + "/5.0").then()
                .statusCode(200).body(CoreMatchers.is("true"));

        RestAssured.given().when().get("/lettuce/sortedset/reactive/score/" + key + "/m").then()
                .statusCode(200).body(CoreMatchers.is("5.0"));
    }

    @Test
    public void bitmapSetBitGetBitBitCount() {
        String key = getKey("bitmap-sync");

        RestAssured.given().when().get("/lettuce/bitmap/bitcount/" + key).then()
                .statusCode(200).body(CoreMatchers.is("0"));

        RestAssured.given().body("1").when().post("/lettuce/bitmap/setbit/" + key + "/7").then()
                .statusCode(200).body(CoreMatchers.is("0"));
        RestAssured.given().body("1").when().post("/lettuce/bitmap/setbit/" + key + "/7").then()
                .statusCode(200).body(CoreMatchers.is("1"));
        RestAssured.given().body("1").when().post("/lettuce/bitmap/setbit/" + key + "/100").then()
                .statusCode(200).body(CoreMatchers.is("0"));

        RestAssured.given().when().get("/lettuce/bitmap/getbit/" + key + "/7").then()
                .statusCode(200).body(CoreMatchers.is("1"));
        RestAssured.given().when().get("/lettuce/bitmap/getbit/" + key + "/8").then()
                .statusCode(200).body(CoreMatchers.is("0"));
        RestAssured.given().when().get("/lettuce/bitmap/bitcount/" + key).then()
                .statusCode(200).body(CoreMatchers.is("2"));
    }

    @Test
    public void bitmapBitField() {
        String key = getKey("bitmap-bitfield");

        // SET i8 #2 5 -> previous 0, GET i8 16 -> 5, INCRBY i8 #2 1 -> 6
        RestAssured.given().body("5").when().post("/lettuce/bitmap/bitfield/" + key).then()
                .statusCode(200).body("$", CoreMatchers.equalTo(List.of(0, 5, 6)));
        // second round reads the incremented value back as the previous one
        RestAssured.given().body("9").when().post("/lettuce/bitmap/bitfield/" + key).then()
                .statusCode(200).body("$", CoreMatchers.equalTo(List.of(6, 9, 10)));
        // the write landed at absolute bit 16 (#2 * 8), not at bit 2
        RestAssured.given().when().get("/lettuce/bitmap/getbit/" + key + "/2").then()
                .statusCode(200).body(CoreMatchers.is("0"));
        RestAssured.given().when().get("/lettuce/bitmap/bitcount/" + key).then()
                .statusCode(200).body(CoreMatchers.is("2"));
    }

    @Test
    public void bitmapBitCountReactive() {
        String key = getKey("bitmap-reactive");

        RestAssured.given().when().get("/lettuce/bitmap/reactive/bitcount/" + key).then()
                .statusCode(200).body(CoreMatchers.is("0"));

        RestAssured.given().body("1").when().post("/lettuce/bitmap/setbit/" + key + "/0").then().statusCode(200);
        RestAssured.given().body("1").when().post("/lettuce/bitmap/setbit/" + key + "/3").then().statusCode(200);
        RestAssured.given().body("1").when().post("/lettuce/bitmap/setbit/" + key + "/9").then().statusCode(200);

        RestAssured.given().when().get("/lettuce/bitmap/reactive/bitcount/" + key).then()
                .statusCode(200).body(CoreMatchers.is("3"));
    }

    @Test
    public void hyperloglogPfAddPfCount() {
        String key = getKey("hll-sync");

        RestAssured.given().when().get("/lettuce/hyperloglog/pfcount/" + key).then()
                .statusCode(200).body(CoreMatchers.is("0"));

        RestAssured.given().body("a,b,c").when().post("/lettuce/hyperloglog/pfadd/" + key).then()
                .statusCode(200).body(CoreMatchers.is("true"));
        // re-adding already-observed elements alters no register
        RestAssured.given().body("a,b").when().post("/lettuce/hyperloglog/pfadd/" + key).then()
                .statusCode(200).body(CoreMatchers.is("false"));
        RestAssured.given().body("d").when().post("/lettuce/hyperloglog/pfadd/" + key).then()
                .statusCode(200).body(CoreMatchers.is("true"));

        // small cardinalities are exact
        RestAssured.given().when().get("/lettuce/hyperloglog/pfcount/" + key).then()
                .statusCode(200).body(CoreMatchers.is("4"));
        RestAssured.given().when().get("/lettuce/key/type/" + key).then()
                .statusCode(200).body(CoreMatchers.is("STRING"));
    }

    @Test
    public void hyperloglogPfMergeAndUnionCount() {
        String key1 = getKey("hll-merge-1");
        String key2 = getKey("hll-merge-2");
        String dest = getKey("hll-merge-dest");

        RestAssured.given().body("a,b,c").when().post("/lettuce/hyperloglog/pfadd/" + key1).then()
                .statusCode(200).body(CoreMatchers.is("true"));
        RestAssured.given().body("c,d,e").when().post("/lettuce/hyperloglog/pfadd/" + key2).then()
                .statusCode(200).body(CoreMatchers.is("true"));

        // union count over two keys does not modify either of them
        RestAssured.given().when().get("/lettuce/hyperloglog/pfcount/" + key1 + "/" + key2).then()
                .statusCode(200).body(CoreMatchers.is("5"));
        RestAssured.given().when().get("/lettuce/hyperloglog/pfcount/" + key1).then()
                .statusCode(200).body(CoreMatchers.is("3"));

        RestAssured.given().when().post("/lettuce/hyperloglog/pfmerge/" + dest + "/" + key1 + "/" + key2).then()
                .statusCode(204);
        RestAssured.given().when().get("/lettuce/hyperloglog/pfcount/" + dest).then()
                .statusCode(200).body(CoreMatchers.is("5"));
        RestAssured.given().when().get("/lettuce/key/exists/" + dest).then()
                .statusCode(200).body(CoreMatchers.is("true"));
    }

    @Test
    public void hyperloglogReactive() {
        String key = getKey("hll-reactive");

        RestAssured.given().when().get("/lettuce/hyperloglog/reactive/pfcount/" + key).then()
                .statusCode(200).body(CoreMatchers.is("0"));

        RestAssured.given().body("x,y").when().post("/lettuce/hyperloglog/reactive/pfadd/" + key).then()
                .statusCode(200).body(CoreMatchers.is("true"));
        RestAssured.given().body("x").when().post("/lettuce/hyperloglog/reactive/pfadd/" + key).then()
                .statusCode(200).body(CoreMatchers.is("false"));

        RestAssured.given().when().get("/lettuce/hyperloglog/reactive/pfcount/" + key).then()
                .statusCode(200).body(CoreMatchers.is("2"));
        // blocking and reactive views agree on the same key
        RestAssured.given().when().get("/lettuce/hyperloglog/pfcount/" + key).then()
                .statusCode(200).body(CoreMatchers.is("2"));
    }

    private void addSicily(String key) {
        RestAssured.given().body("Palermo").when().post("/lettuce/geo/add/" + key + "/13.361389/38.115556").then()
                .statusCode(200).body(CoreMatchers.is("true"));
        RestAssured.given().body("Catania").when().post("/lettuce/geo/add/" + key + "/15.087269/37.502669").then()
                .statusCode(200).body(CoreMatchers.is("true"));
    }

    @Test
    public void geoAddDistHashPos() {
        String key = getKey("geo-sync");

        RestAssured.given().when().get("/lettuce/geo/dist/" + key + "/Palermo/Catania").then()
                .statusCode(204);

        addSicily(key);
        // re-adding an existing member at the same position adds nothing
        RestAssured.given().body("Palermo").when().post("/lettuce/geo/add/" + key + "/13.361389/38.115556").then()
                .statusCode(200).body(CoreMatchers.is("false"));

        double distance = Double.parseDouble(RestAssured.given().when()
                .get("/lettuce/geo/dist/" + key + "/Palermo/Catania").then().statusCode(200).extract().asString());
        assertEquals(166.2742, distance, 0.001);
        RestAssured.given().when().get("/lettuce/geo/dist/" + key + "/Palermo/missing").then()
                .statusCode(204);

        RestAssured.given().when().get("/lettuce/geo/hash/" + key + "/Palermo").then()
                .statusCode(200).body(CoreMatchers.is("sqc8b49rny0"));
        RestAssured.given().when().get("/lettuce/geo/hash/" + key + "/Catania").then()
                .statusCode(200).body(CoreMatchers.is("sqdtr74hyu0"));
        RestAssured.given().when().get("/lettuce/geo/hash/" + key + "/missing").then()
                .statusCode(204);

        String[] position = RestAssured.given().when().get("/lettuce/geo/pos/" + key + "/Palermo").then()
                .statusCode(200).extract().asString().split(",");
        // geohash encoding loses precision, so the stored position is only close to the submitted one
        assertEquals(13.361389, Double.parseDouble(position[0]), 0.0001);
        assertEquals(38.115556, Double.parseDouble(position[1]), 0.0001);
        RestAssured.given().when().get("/lettuce/geo/pos/" + key + "/missing").then()
                .statusCode(204);

        // a geo index is stored as a sorted set
        RestAssured.given().when().get("/lettuce/key/type/" + key).then()
                .statusCode(200).body(CoreMatchers.is("ZSET"));
    }

    @Test
    public void geoSearchAndRadiusByMember() {
        String key = getKey("geo-search");
        addSicily(key);

        List<String> found = RestAssured.given()
                .queryParam("longitude", 15).queryParam("latitude", 37).queryParam("radius", 200)
                .when().get("/lettuce/geo/search/" + key).then()
                .statusCode(200).extract().jsonPath().getList("$", String.class);
        assertEquals(2, found.size(), () -> "expected both members within 200 km, got " + found);
        // ascending: Catania (~56 km) comes before Palermo (~190 km)
        String[] first = found.get(0).split(",");
        String[] second = found.get(1).split(",");
        assertEquals("Catania", first[0]);
        assertEquals(56.4413, Double.parseDouble(first[1]), 0.001);
        assertEquals("Palermo", second[0]);
        assertEquals(190.4424, Double.parseDouble(second[1]), 0.001);

        RestAssured.given()
                .queryParam("longitude", 15).queryParam("latitude", 37).queryParam("radius", 100)
                .when().get("/lettuce/geo/search/" + key).then()
                .statusCode(200).body("size()", CoreMatchers.is(1))
                .body("[0]", CoreMatchers.startsWith("Catania,"));

        RestAssured.given().when().get("/lettuce/geo/radiusbymember/" + key + "/Catania/200").then()
                .statusCode(200)
                .body("$", CoreMatchers.hasItems("Palermo", "Catania"))
                .body("size()", CoreMatchers.is(2));
        // Palermo is ~166 km from Catania, so only Catania itself is within 100 km
        RestAssured.given().when().get("/lettuce/geo/radiusbymember/" + key + "/Catania/100").then()
                .statusCode(200)
                .body("$", CoreMatchers.equalTo(List.of("Catania")));
    }

    @Test
    public void geoSearchStore() {
        String key = getKey("geo-store-src");
        String dest = getKey("geo-store-dest");
        addSicily(key);

        RestAssured.given().when().post("/lettuce/geo/searchstore/" + dest + "/" + key + "/Palermo/200").then()
                .statusCode(200).body(CoreMatchers.is("2"));
        RestAssured.given().when().get("/lettuce/key/type/" + dest).then()
                .statusCode(200).body(CoreMatchers.is("ZSET"));
        // the destination is a full geo index: positions survive the copy
        RestAssured.given().when().get("/lettuce/geo/hash/" + dest + "/Catania").then()
                .statusCode(200).body(CoreMatchers.is("sqdtr74hyu0"));

        // a narrower radius overwrites the destination with fewer members
        RestAssured.given().when().post("/lettuce/geo/searchstore/" + dest + "/" + key + "/Palermo/100").then()
                .statusCode(200).body(CoreMatchers.is("1"));
        RestAssured.given().when().get("/lettuce/sortedset/card/" + dest).then()
                .statusCode(200).body(CoreMatchers.is("1"));
    }

    @Test
    public void geoReactive() {
        String key = getKey("geo-reactive");

        RestAssured.given().when().get("/lettuce/geo/reactive/dist/" + key + "/Palermo/Catania").then()
                .statusCode(204);

        RestAssured.given().body("Palermo").when().post("/lettuce/geo/reactive/add/" + key + "/13.361389/38.115556")
                .then().statusCode(200).body(CoreMatchers.is("true"));
        RestAssured.given().body("Catania").when().post("/lettuce/geo/reactive/add/" + key + "/15.087269/37.502669")
                .then().statusCode(200).body(CoreMatchers.is("true"));
        RestAssured.given().body("Catania").when().post("/lettuce/geo/reactive/add/" + key + "/15.087269/37.502669")
                .then().statusCode(200).body(CoreMatchers.is("false"));

        double distance = Double.parseDouble(RestAssured.given().when()
                .get("/lettuce/geo/reactive/dist/" + key + "/Palermo/Catania").then().statusCode(200).extract()
                .asString());
        assertEquals(166.2742, distance, 0.001);
        // blocking and reactive views agree on the same key
        RestAssured.given().when().get("/lettuce/geo/hash/" + key + "/Palermo").then()
                .statusCode(200).body(CoreMatchers.is("sqc8b49rny0"));
    }

    @Test
    public void withConnectionBlockingClientIds() {
        String body = RestAssured.given().when().get("/lettuce/with-connection/client-ids")
                .then().statusCode(200).extract().asString();
        String[] parts = body.split(",");
        long inside1 = Long.parseLong(parts[0]);
        long inside2 = Long.parseLong(parts[1]);
        long outside = Long.parseLong(parts[2]);
        assertTrue(inside1 > 0 && inside2 > 0 && outside > 0, () -> "expected positive ids, got " + body);
        assertEquals(inside1, inside2, () -> "expected stable id inside block, got " + body);
        assertNotEquals(inside1, outside, () -> "expected fresh connection inside block, got " + body);
    }

    @Test
    public void withConnectionReactiveClientIds() {
        String body = RestAssured.given().when().get("/lettuce/with-connection/reactive/client-ids")
                .then().statusCode(200).extract().asString();
        String[] parts = body.split(",");
        long inside1 = Long.parseLong(parts[0]);
        long inside2 = Long.parseLong(parts[1]);
        long outside = Long.parseLong(parts[2]);
        assertTrue(inside1 > 0 && inside2 > 0 && outside > 0, () -> "expected positive ids, got " + body);
        assertEquals(inside1, inside2, () -> "expected stable id inside block, got " + body);
        assertNotEquals(inside1, outside, () -> "expected fresh connection inside block, got " + body);
    }

    @Test
    public void withConnectionNestedReusesConnection() {
        String body = RestAssured.given().when().get("/lettuce/with-connection/nested")
                .then().statusCode(200).extract().asString();
        String[] parts = body.split(",");
        long outer = Long.parseLong(parts[0]);
        long inner = Long.parseLong(parts[1]);
        assertEquals(outer, inner, () -> "expected nested withConnection to reuse outer connection, got " + body);
    }

    @Test
    public void withTransactionBlocking() {
        String key = getKey("tx-blocking");
        String body = RestAssured.given().body("v1").when().post("/lettuce/with-transaction/blocking/" + key)
                .then().statusCode(200).extract().asString();
        assertEquals("false,2,v1", body);
        RestAssured.given().when().get("/lettuce/value/" + key).then()
                .statusCode(200).body(CoreMatchers.is("v1"));
    }

    @Test
    public void withTransactionReactive() {
        String key = getKey("tx-reactive");
        String body = RestAssured.given().body("rv1").when().post("/lettuce/with-transaction/reactive/" + key)
                .then().statusCode(200).extract().asString();
        assertEquals("false,2,rv1", body);
        RestAssured.given().when().get("/lettuce/value/" + key).then()
                .statusCode(200).body(CoreMatchers.is("rv1"));
    }

    @Test
    public void withTransactionDiscard() {
        String key = getKey("tx-discard");
        String body = RestAssured.given().body("v").when().post("/lettuce/with-transaction/discard/" + key)
                .then().statusCode(200).extract().asString();
        assertEquals("true,null", body);
        RestAssured.given().when().get("/lettuce/value/" + key).then().statusCode(204);
    }

    @Test
    public void withTransactionOptimistic() {
        String key = getKey("tx-optimistic");
        RestAssured.given().body("10").when().post("/lettuce/value/" + key).then().statusCode(204);
        String body = RestAssured.given().body("0").when().post("/lettuce/with-transaction/optimistic/" + key)
                .then().statusCode(200).extract().asString();
        assertEquals("false,10,100", body);
    }

    @Test
    public void withTransactionKey() {
        String key = getKey("tx-key");
        String body = RestAssured.given().when().post("/lettuce/with-transaction/key/" + key)
                .then().statusCode(200).extract().asString();
        assertEquals("false,4,true,true,true,STRING", body);
    }

    @Test
    public void withTransactionSortedSet() {
        String key = getKey("tx-zset");
        String body = RestAssured.given().when().post("/lettuce/with-transaction/sortedset/" + key)
                .then().statusCode(200).extract().asString();
        // after ZPOPMIN removed "a", ZRANGE 0 -1 REV WITHSCORES yields c then b, and ZRANGESTORE copies both
        assertEquals("false,6,true,2,3,a,1.0,c:3.0,b:2.0,2", body);
        RestAssured.given().when().get("/lettuce/sortedset/card/" + key + "-dst").then()
                .statusCode(200).body(CoreMatchers.is("2"));
    }

    @Test
    public void withTransactionHyperLogLog() {
        String key = getKey("tx-hll");
        String body = RestAssured.given().when().post("/lettuce/with-transaction/hyperloglog/" + key)
                .then().statusCode(200).extract().asString();
        // pfadd(a,b,c) -> true, pfadd(a) -> false, pfadd(other: c,d) -> true, pfmerge -> null (discarded), pfcount(merged) -> 4
        assertEquals("false,5,true,false,true,true,4", body);
        RestAssured.given().when().get("/lettuce/hyperloglog/pfcount/" + key + "-merged").then()
                .statusCode(200).body(CoreMatchers.is("4"));
    }

    @Test
    public void withTransactionGeo() {
        String key = getKey("tx-geo");
        String body = RestAssured.given().when().post("/lettuce/with-transaction/geo/" + key)
                .then().statusCode(200).extract().asString();
        // geoadd x2 -> true, geodist -> ~166 km, geopos(Palermo, missing) -> 2 entries with a null second,
        // geosearch from Palermo within 200 km ascending -> Palermo (0 km) then Catania
        assertEquals("false,5,true,true,166,2,true,Palermo,Catania", body);
        RestAssured.given().when().get("/lettuce/sortedset/card/" + key).then()
                .statusCode(200).body(CoreMatchers.is("2"));
    }

    private static String jsonSet(String key, String document) {
        return RestAssured.given().body(document).when().post("/lettuce/json/" + key)
                .then().statusCode(200).extract().asString();
    }

    @Test
    public void jsonSetGet() {
        String key = getKey("json-doc");
        String document = "{\"a\":2,\"b\":3,\"nested\":{\"a\":4,\"b\":null}}";

        RestAssured.given().when().get("/lettuce/json/" + key).then().statusCode(204);

        assertEquals("[" + document + "]", jsonSet(key, document));
        RestAssured.given().when().get("/lettuce/json/" + key).then()
                .statusCode(200).body(CoreMatchers.is(document));
        RestAssured.given().queryParam("path", "$..b").when().get("/lettuce/json/path/" + key).then()
                .statusCode(200).body(CoreMatchers.is("[3,null]"));

        String multi = RestAssured.given().queryParam("path", "$..a").queryParam("path", "$..b")
                .when().get("/lettuce/json/paths/" + key).then().statusCode(200).extract().asString();
        assertEquals(new JsonObject("{\"$..a\":[2,4],\"$..b\":[3,null]}"), new JsonObject(multi));
    }

    @Test
    public void jsonSetNxXx() {
        String key = getKey("json-nxxx");
        jsonSet(key, "{\"name\":\"luke\"}");

        // NX creates the path, a second NX on the same path is a no-op
        RestAssured.given().queryParam("path", "$.friends").queryParam("nx", true).body("[\"Obiwan\",\"Han\"]")
                .when().post("/lettuce/json/" + key).then().statusCode(200);
        RestAssured.given().queryParam("path", "$.friends").queryParam("nx", true).body("[\"Leia\"]")
                .when().post("/lettuce/json/" + key).then().statusCode(200);
        RestAssured.given().queryParam("path", "$.friends").when().get("/lettuce/json/path/" + key).then()
                .statusCode(200).body(CoreMatchers.is("[[\"Obiwan\",\"Han\"]]"));

        // XX only updates an existing path
        RestAssured.given().queryParam("path", "$.missing").queryParam("xx", true).body("\"nope\"")
                .when().post("/lettuce/json/" + key).then().statusCode(200);
        RestAssured.given().queryParam("path", "$.missing").when().get("/lettuce/json/path/" + key).then()
                .statusCode(200).body(CoreMatchers.is("[]"));
        RestAssured.given().queryParam("path", "$.name").queryParam("xx", true).body("\"leia\"")
                .when().post("/lettuce/json/" + key).then().statusCode(200);
        RestAssured.given().queryParam("path", "$.name").when().get("/lettuce/json/path/" + key).then()
                .statusCode(200).body(CoreMatchers.is("[\"leia\"]"));
    }

    @Test
    public void jsonTypedRoundTrip() {
        String key = getKey("json-person");
        RestAssured.given().body("luke,20").when().post("/lettuce/json/person/" + key).then()
                .statusCode(200).body(CoreMatchers.is("luke,20,true"));
        RestAssured.given().when().get("/lettuce/json/" + key).then()
                .statusCode(200).body(CoreMatchers.is("{\"name\":\"luke\",\"age\":20}"));
    }

    @Test
    public void jsonArrays() {
        String key = getKey("json-arr");
        jsonSet(key, "{\"arr\":[1,2],\"nums\":[5,6,7]}");

        RestAssured.given().queryParam("path", "$.arr").body("a,b").when().post("/lettuce/json/arrappend/" + key)
                .then().statusCode(200).body(CoreMatchers.is("[4]"));
        RestAssured.given().queryParam("path", "$.arr").when().get("/lettuce/json/arrlen/" + key)
                .then().statusCode(200).body(CoreMatchers.is("[4]"));
        RestAssured.given().queryParam("path", "$.arr").when().get("/lettuce/json/path/" + key)
                .then().statusCode(200).body(CoreMatchers.is("[[1,2,\"a\",\"b\"]]"));
        RestAssured.given().queryParam("path", "$.nums").when().post("/lettuce/json/arrpop/" + key)
                .then().statusCode(200).body(CoreMatchers.is("[7]"));
        RestAssured.given().queryParam("path", "$.arr").when().get("/lettuce/json/type/" + key)
                .then().statusCode(200).body(CoreMatchers.is("[\"array\"]"));

        // root-level array
        String rootArray = getKey("json-root-arr");
        jsonSet(rootArray, "[\"a\",\"b\"]");
        RestAssured.given().when().get("/lettuce/json/array/" + rootArray)
                .then().statusCode(200).body(CoreMatchers.is("[\"a\",\"b\"]"));
        RestAssured.given().when().get("/lettuce/json/arrlen/" + rootArray)
                .then().statusCode(200).body(CoreMatchers.is("[2]"));
    }

    @Test
    public void jsonNumbersStringsBooleans() {
        String key = getKey("json-scalars");
        jsonSet(key, "{\"a\":1,\"n\":{\"a\":2},\"s\":\"foo\",\"t\":true}");

        RestAssured.given().queryParam("path", "$..a").body("2").when().post("/lettuce/json/numincrby/" + key)
                // NUMINCRBY takes a double, so the incremented values come back as floats
                .then().statusCode(200).body(CoreMatchers.is("[3.0,4.0]"));
        RestAssured.given().queryParam("path", "$.s").body("bar").when().post("/lettuce/json/strappend/" + key)
                .then().statusCode(200).body(CoreMatchers.is("[6]"));
        RestAssured.given().queryParam("path", "$.s").when().get("/lettuce/json/strlen/" + key)
                .then().statusCode(200).body(CoreMatchers.is("[6]"));
        RestAssured.given().queryParam("path", "$.t").when().post("/lettuce/json/toggle/" + key)
                .then().statusCode(200).body(CoreMatchers.is("[false]"));
        RestAssured.given().queryParam("path", "$.t").when().post("/lettuce/json/toggle/" + key)
                .then().statusCode(200).body(CoreMatchers.is("[true]"));
        RestAssured.given().queryParam("path", "$..a").when().get("/lettuce/json/type/" + key)
                .then().statusCode(200).body(CoreMatchers.is("[\"number\",\"number\"]"));
    }

    @Test
    public void jsonObjects() {
        String key = getKey("json-obj");
        jsonSet(key, "{\"a\":1,\"b\":2}");

        RestAssured.given().when().get("/lettuce/json/objkeys/" + key)
                .then().statusCode(200).body(CoreMatchers.is("[\"a\",\"b\"]"));
        RestAssured.given().when().get("/lettuce/json/objlen/" + key)
                .then().statusCode(200).body(CoreMatchers.is("2"));
        RestAssured.given().when().get("/lettuce/json/objlen/" + getKey("json-obj-missing"))
                .then().statusCode(204);
    }

    @Test
    public void jsonMget() {
        String doc1 = getKey("json-mget-1");
        String doc2 = getKey("json-mget-2");
        jsonSet(doc1, "{\"a\":1,\"nested\":{\"a\":3}}");
        jsonSet(doc2, "{\"a\":4,\"nested\":{\"a\":6}}");

        RestAssured.given().queryParam("path", "$..a")
                .queryParam("key", doc1).queryParam("key", doc2).queryParam("key", getKey("json-mget-missing"))
                .when().get("/lettuce/json/mget")
                .then().statusCode(200)
                .body("size()", CoreMatchers.is(3))
                .body("[0]", CoreMatchers.is("[1,3]"))
                .body("[1]", CoreMatchers.is("[4,6]"))
                .body("[2]", CoreMatchers.nullValue());
    }

    @Test
    public void jsonClearDel() {
        String key = getKey("json-clear");
        jsonSet(key, "{\"obj\":{\"a\":1},\"arr\":[1,2],\"s\":\"x\"}");

        // JSON.CLEAR empties containers and zeroes numbers, strings are left alone
        RestAssured.given().queryParam("path", "$.*").when().post("/lettuce/json/clear/" + key)
                .then().statusCode(200).body(CoreMatchers.is("2"));
        RestAssured.given().queryParam("path", "$.s").when().delete("/lettuce/json/" + key)
                .then().statusCode(200).body(CoreMatchers.is("1"));
        RestAssured.given().when().get("/lettuce/json/" + key)
                .then().statusCode(200).body(CoreMatchers.is("{\"obj\":{},\"arr\":[]}"));

        RestAssured.given().when().delete("/lettuce/json/" + key)
                .then().statusCode(200).body(CoreMatchers.is("1"));
        RestAssured.given().when().get("/lettuce/json/" + key).then().statusCode(204);
        RestAssured.given().when().delete("/lettuce/json/" + key)
                .then().statusCode(200).body(CoreMatchers.is("0"));
    }

    @Test
    public void jsonReactive() {
        String key = getKey("json-reactive");
        RestAssured.given().body("{\"a\":1,\"b\":\"two\"}").when().post("/lettuce/json/reactive/" + key)
                .then().statusCode(200).body(CoreMatchers.is("{\"a\":1,\"b\":\"two\"}"));
        RestAssured.given().queryParam("path", "$.b").when().get("/lettuce/json/reactive/" + key)
                .then().statusCode(200).body(CoreMatchers.is("[\"two\"]"));
    }

    @Test
    public void withTransactionJson() {
        String key = getKey("tx-json");
        String body = RestAssured.given().when().post("/lettuce/with-transaction/json/" + key)
                .then().statusCode(200).extract().asString();
        // jsonSet -> null, arrappend -> [2], arrlen -> [2], numincrby -> null, strappend -> [14] ("luke skywalker"),
        // type -> [number] (NUMINCRBY takes a double, so age became a float), then the document, del of $.tags -> 1,
        // objkeys of $ -> [[name, age]]
        String prefix = "false,9,true,[2],[2],true,[14],[number],";
        String suffix = ",1,[[name, age]]";
        assertTrue(body.startsWith(prefix), body);
        assertTrue(body.endsWith(suffix), body);
        JsonObject document = new JsonObject(body.substring(prefix.length(), body.length() - suffix.length()));
        assertEquals(new JsonObject("{\"name\":\"luke skywalker\",\"tags\":[\"jedi\",\"pilot\"],\"age\":22.0}"),
                document);
    }

    @Test
    public void countMinIncrByAndQuery() {
        String key = getKey("cms-sync");
        String probKey = getKey("cms-prob");

        RestAssured.given().when().post("/lettuce/countmin/init/" + key + "/100/5").then().statusCode(204);
        RestAssured.given().when().post("/lettuce/countmin/initbyprob/" + probKey + "/0.001/0.01").then().statusCode(204);
        // re-initialising an existing sketch is a server-side error
        RestAssured.given().when().post("/lettuce/countmin/init/" + key + "/100/5").then().statusCode(500);

        RestAssured.given().when().post("/lettuce/countmin/incrby/" + key + "/leia/10").then()
                .statusCode(200).body(CoreMatchers.is("10"));
        RestAssured.given().body("leia=2,luke=5,anakin=3").when().post("/lettuce/countmin/incrby/" + key).then()
                .statusCode(200)
                .body("leia", CoreMatchers.is(12))
                .body("luke", CoreMatchers.is(5))
                .body("anakin", CoreMatchers.is(3));

        RestAssured.given().when().get("/lettuce/countmin/query/" + key + "/anakin").then()
                .statusCode(200).body(CoreMatchers.is("3"));
        RestAssured.given().queryParam("items", "leia,luke").when().get("/lettuce/countmin/query/" + key).then()
                .statusCode(200).body(CoreMatchers.is("[12,5]"));
        // an item never seen has a zero count
        RestAssured.given().when().get("/lettuce/countmin/query/" + probKey + "/leia").then()
                .statusCode(200).body(CoreMatchers.is("0"));
    }

    @Test
    public void countMinMerge() {
        String key1 = getKey("cms-merge-1");
        String key2 = getKey("cms-merge-2");
        String dest = getKey("cms-merge-dest");
        String weighted = getKey("cms-merge-weighted");

        for (String k : List.of(key1, key2, dest, weighted)) {
            RestAssured.given().when().post("/lettuce/countmin/init/" + k + "/100/5").then().statusCode(204);
        }
        RestAssured.given().when().post("/lettuce/countmin/incrby/" + key1 + "/leia/2").then()
                .statusCode(200).body(CoreMatchers.is("2"));
        RestAssured.given().body("leia=2,luke=5,anakin=10").when().post("/lettuce/countmin/incrby/" + key2).then()
                .statusCode(200);

        // without weights every source counts once
        RestAssured.given().when().post("/lettuce/countmin/merge/" + dest + "/" + key1 + "/" + key2).then()
                .statusCode(204);
        RestAssured.given().queryParam("items", "leia,luke,anakin").when().get("/lettuce/countmin/query/" + dest).then()
                .statusCode(200).body(CoreMatchers.is("[4,5,10]"));

        // with weights the first source is doubled
        RestAssured.given().queryParam("weights", "2,1").when()
                .post("/lettuce/countmin/merge/" + weighted + "/" + key1 + "/" + key2).then()
                .statusCode(204);
        RestAssured.given().queryParam("items", "leia,luke,anakin").when().get("/lettuce/countmin/query/" + weighted)
                .then().statusCode(200).body(CoreMatchers.is("[6,5,10]"));
    }

    @Test
    public void countMinReactive() {
        String key = getKey("cms-reactive");

        RestAssured.given().when().post("/lettuce/countmin/init/" + key + "/100/5").then().statusCode(204);
        RestAssured.given().when().post("/lettuce/countmin/reactive/incrby/" + key + "/x/4").then()
                .statusCode(200).body(CoreMatchers.is("4"));
        RestAssured.given().when().post("/lettuce/countmin/reactive/incrby/" + key + "/x/1").then()
                .statusCode(200).body(CoreMatchers.is("5"));
        RestAssured.given().when().get("/lettuce/countmin/reactive/query/" + key + "/x").then()
                .statusCode(200).body(CoreMatchers.is("5"));
    }

    @Test
    public void withTransactionCountMin() {
        String key = getKey("tx-cms");
        String body = RestAssured.given().when().post("/lettuce/with-transaction/countmin/" + key)
                .then().statusCode(200).extract().asString();
        // 3x init -> null (discarded), incrby(a,3) -> 3, incrby(other: a=2,b=4) -> [2, 4] as a raw list like Vert.x,
        // merge(2*key + 1*other) -> null (discarded), query(merged: a,b) -> [3*2+2, 4]
        assertEquals("false,7,true,3,[2, 4],true,[8, 4]", body);
        RestAssured.given().when().get("/lettuce/countmin/query/" + key + "-merged/a").then()
                .statusCode(200).body(CoreMatchers.is("8"));
    }

    @Test
    public void autoSuggestAddGetDel() {
        String key = getKey("sug-sync");

        RestAssured.given().body("hello world").when().post("/lettuce/autosuggest/add/" + key + "/1").then()
                .statusCode(200).body(CoreMatchers.is("1"));
        // INCR adds to the score of an existing entry instead of creating a new one
        RestAssured.given().queryParam("incr", true).body("hello world").when()
                .post("/lettuce/autosuggest/add/" + key + "/3").then()
                .statusCode(200).body(CoreMatchers.is("1"));
        RestAssured.given().body("bonjour").when().post("/lettuce/autosuggest/add/" + key + "/3").then()
                .statusCode(200).body(CoreMatchers.is("2"));
        RestAssured.given().body("bonjourno").when().post("/lettuce/autosuggest/add/" + key + "/1").then()
                .statusCode(200).body(CoreMatchers.is("3"));

        RestAssured.given().when().get("/lettuce/autosuggest/len/" + key).then()
                .statusCode(200).body(CoreMatchers.is("3"));
        RestAssured.given().when().delete("/lettuce/autosuggest/" + key + "/bonjourno").then()
                .statusCode(200).body(CoreMatchers.is("true"));
        RestAssured.given().when().delete("/lettuce/autosuggest/" + key + "/missing").then()
                .statusCode(200).body(CoreMatchers.is("false"));
        RestAssured.given().when().get("/lettuce/autosuggest/len/" + key).then()
                .statusCode(200).body(CoreMatchers.is("2"));

        RestAssured.given().body("hell").when().post("/lettuce/autosuggest/add/" + key + "/3").then()
                .statusCode(200).body(CoreMatchers.is("3"));

        RestAssured.given().when().get("/lettuce/autosuggest/get/" + key + "/hell").then()
                .statusCode(200)
                .body("size()", CoreMatchers.is(2))
                .body("$", CoreMatchers.hasItems("hell", "hello world"));
        // MAX 1 WITHSCORES: exactly one entry, with its score attached
        RestAssured.given().queryParam("max", 1).queryParam("withScores", true).when()
                .get("/lettuce/autosuggest/get/" + key + "/hel").then()
                .statusCode(200)
                .body("size()", CoreMatchers.is(1))
                .body("[0]", CoreMatchers.startsWith("hell:"));

        RestAssured.given().body("hill").when().post("/lettuce/autosuggest/add/" + key + "/3").then()
                .statusCode(200).body(CoreMatchers.is("4"));
        RestAssured.given().queryParam("fuzzy", true).when().get("/lettuce/autosuggest/get/" + key + "/hell").then()
                .statusCode(200)
                .body("size()", CoreMatchers.is(3))
                .body("$", CoreMatchers.hasItems("hell", "hello world", "hill"));
    }

    @Test
    public void autoSuggestReactive() {
        String key = getKey("sug-reactive");

        RestAssured.given().body("alpha").when().post("/lettuce/autosuggest/reactive/add/" + key + "/1").then()
                .statusCode(200).body(CoreMatchers.is("1"));
        RestAssured.given().body("alphabet").when().post("/lettuce/autosuggest/reactive/add/" + key + "/2").then()
                .statusCode(200).body(CoreMatchers.is("2"));

        RestAssured.given().when().get("/lettuce/autosuggest/reactive/get/" + key + "/alp").then()
                .statusCode(200)
                .body("size()", CoreMatchers.is(2))
                .body("$", CoreMatchers.hasItems("alpha", "alphabet"));
        RestAssured.given().queryParam("withScores", true).when()
                .get("/lettuce/autosuggest/reactive/get/" + key + "/alp").then()
                .statusCode(200)
                .body("size()", CoreMatchers.is(2))
                .body("$", CoreMatchers.everyItem(CoreMatchers.containsString(":")));
    }

    @Test
    public void withTransactionAutoSuggest() {
        String key = getKey("tx-sug");
        String body = RestAssured.given().when().post("/lettuce/with-transaction/autosuggest/" + key)
                .then().statusCode(200).extract().asString();
        // 4x add -> 1..4, del(boo) -> true, len -> 3, sugget(abcd) -> [abcd, abcde],
        // sugget(ab, MAX 1 WITHSCORES) -> the single best match with its score
        assertEquals("false,8,1,4,true,3,[abcd, abcde],[abcde:1.0]", body);
        RestAssured.given().when().get("/lettuce/autosuggest/len/" + key).then()
                .statusCode(200).body(CoreMatchers.is("3"));
    }

    private static final String MOVIE_SCHEMA = "title:TEXT:sortable,release_year:NUMERIC:sortable,"
            + "rating:NUMERIC:sortable,genre:TAG:sortable";

    private static void hset(String key, Map<String, String> fields) {
        fields.forEach((field, value) -> RestAssured.given().body(value).when()
                .post("/lettuce/hash/" + key + "/" + field).then().statusCode(200));
    }

    /** Creates the movie index over four hashes with the given key prefix, and waits until they are indexed. */
    private static void createMovieIndex(String index, String prefix) {
        // an index left over by a previous run against the same server would make FT.CREATE fail
        RestAssured.given().queryParam("dd", true).when().delete("/lettuce/search/index/" + index);
        RestAssured.given().queryParam("prefix", prefix).body(MOVIE_SCHEMA).when()
                .post("/lettuce/search/index/" + index).then().statusCode(204);
        hset(prefix + "11002", Map.of("title", "Star Wars: Episode V - The Empire Strikes Back",
                "plot", "After the Rebels are brutally overpowered by the Empire on the ice planet Hoth, ...",
                "release_year", "1972", "genre", "Action", "rating", "8.7"));
        hset(prefix + "11003", Map.of("title", "The Godfather",
                "plot", "The aging patriarch of an organized crime dynasty transfers control of his ...",
                "release_year", "1972", "genre", "Drama", "rating", "9.2"));
        hset(prefix + "11004", Map.of("title", "Heat",
                "plot", "A group of professional bank robbers start to feel the heat ...",
                "release_year", "1995", "genre", "Thriller", "rating", "8.2"));
        hset(prefix + "11005", Map.of("title", "Star Wars: Episode VI - Return of the Jedi",
                "plot", "The Rebels dispatch to Endor to destroy the second Empire's Death Star.",
                "release_year", "1983", "genre", "Action", "rating", "8.3"));
        awaitIndexed(index, "*", 4);
    }

    private static int searchCount(String index, String query) {
        return RestAssured.given().queryParam("q", query).when().get("/lettuce/search/query/" + index)
                .then().statusCode(200).extract().jsonPath().getInt("count");
    }

    /** Indexing runs in the background: polls until {@code query} matches {@code expected} documents. */
    private static void awaitIndexed(String index, String query, int expected) {
        await(() -> searchCount(index, query) == expected,
                () -> "expected " + expected + " documents matching '" + query + "' in " + index + ", got "
                        + searchCount(index, query));
    }

    private static void await(BooleanSupplier condition, Supplier<String> failure) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail(failure.get());
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    @Test
    public void searchCreateQueryDrop() {
        String index = getKey("idx-movie");
        String prefix = getKey("movie") + ":";
        createMovieIndex(index, prefix);

        RestAssured.given().when().get("/lettuce/search/list").then()
                .statusCode(200).body("$", CoreMatchers.hasItem(index));

        RestAssured.given().queryParam("q", "war").when().get("/lettuce/search/query/" + index).then()
                .statusCode(200)
                .body("count", CoreMatchers.is(2))
                .body("documents.key", CoreMatchers.hasItems(prefix + "11002", prefix + "11005"))
                // without WITHSCORES every score is reported as 1.0
                .body("documents.score", CoreMatchers.everyItem(CoreMatchers.is(1.0f)))
                .body("documents.properties.genre", CoreMatchers.everyItem(CoreMatchers.is("Action")));

        // RETURN with an alias, sorted on the sortable year: Heat (1995) comes first
        RestAssured.given().queryParam("q", "*").queryParam("return", "title,release_year=year")
                .queryParam("sortBy", "release_year").queryParam("desc", true)
                .when().get("/lettuce/search/query/" + index).then()
                .statusCode(200)
                .body("count", CoreMatchers.is(4))
                .body("documents[0].key", CoreMatchers.is(prefix + "11004"))
                .body("documents[0].properties.year", CoreMatchers.is("1995"))
                .body("documents[0].properties.genre", CoreMatchers.nullValue());

        // LIMIT 0 0 only counts
        RestAssured.given().queryParam("q", "*").queryParam("offset", 0).queryParam("count", 0)
                .when().get("/lettuce/search/query/" + index).then()
                .statusCode(200)
                .body("count", CoreMatchers.is(4))
                .body("documents.size()", CoreMatchers.is(0));

        // NOCONTENT returns the keys only
        RestAssured.given().queryParam("q", "@genre:{Thriller}").queryParam("noContent", true)
                .when().get("/lettuce/search/query/" + index).then()
                .statusCode(200)
                .body("documents.key", CoreMatchers.equalTo(List.of(prefix + "11004")))
                .body("documents[0].properties.size()", CoreMatchers.is(0));

        // PARAMS (dialect 2) and WITHSCORES
        RestAssured.given().queryParam("q", "@genre:{$GENRE}").queryParam("param", "GENRE=Action")
                .when().get("/lettuce/search/query/" + index).then()
                .statusCode(200).body("count", CoreMatchers.is(2));
        RestAssured.given().queryParam("q", "war").queryParam("withScores", true)
                .when().get("/lettuce/search/query/" + index).then()
                .statusCode(200)
                .body("count", CoreMatchers.is(2))
                .body("documents.score", CoreMatchers.everyItem(Matchers.greaterThan(0.0f)));

        // DROPINDEX DD removes the index and its documents
        RestAssured.given().queryParam("dd", true).when().delete("/lettuce/search/index/" + index).then()
                .statusCode(204);
        RestAssured.given().when().get("/lettuce/search/list").then()
                .statusCode(200).body("$", CoreMatchers.not(CoreMatchers.hasItem(index)));
        await(() -> RestAssured.given().when().get("/lettuce/key/exists/" + prefix + "11002")
                .then().statusCode(200).extract().asString().equals("false"),
                () -> "expected the documents of " + index + " to be deleted with it");
    }

    @Test
    public void searchAggregateAndCursor() {
        String index = getKey("idx-aggregate");
        String prefix = getKey("aggregate") + ":";
        createMovieIndex(index, prefix);

        // GROUPBY genre REDUCE COUNT, most frequent first: the TAG values come back normalised
        RestAssured.given().queryParam("q", "*").queryParam("groupBy", "@genre").queryParam("reduce", "COUNT:count")
                .queryParam("sortBy", "@count").queryParam("desc", true)
                .when().get("/lettuce/search/aggregate/" + index).then()
                .statusCode(200)
                .body("count", CoreMatchers.is(3))
                .body("cursor", CoreMatchers.is(-1))
                .body("documents[0].properties.genre", CoreMatchers.is("action"))
                .body("documents[0].properties.count", CoreMatchers.is("2"))
                .body("documents.properties.genre", CoreMatchers.hasItems("drama", "thriller"));

        // LOAD with an alias and APPLY, sorted on the computed property
        List<String> doubled = RestAssured.given().queryParam("q", "@genre:{Action}").queryParam("load", "rating=score")
                .queryParam("apply", "doubled=@score * 2").queryParam("sortBy", "@doubled")
                .when().get("/lettuce/search/aggregate/" + index).then()
                .statusCode(200)
                .body("count", CoreMatchers.is(2))
                .extract().jsonPath().getList("documents.properties.doubled", String.class);
        assertEquals(16.6, Double.parseDouble(doubled.get(0)), 0.01);
        assertEquals(17.4, Double.parseDouble(doubled.get(1)), 0.01);

        // WITHCURSOR COUNT 1: one page at a time
        String page = RestAssured.given().queryParam("q", "*").queryParam("load", "*").queryParam("cursor", 1)
                .when().get("/lettuce/search/aggregate/" + index).then()
                .statusCode(200).body("count", CoreMatchers.is(1)).extract().asString();
        long cursor = new JsonObject(page).getLong("cursor");
        assertTrue(cursor > 0, page);

        String next = RestAssured.given().when().get("/lettuce/search/cursor/" + index + "/" + cursor).then()
                .statusCode(200).body("count", CoreMatchers.is(1)).extract().asString();
        assertEquals(cursor, new JsonObject(next).getLong("cursor"));

        RestAssured.given().when().delete("/lettuce/search/cursor/" + index + "/" + cursor).then().statusCode(204);
        // the deleted cursor is gone on the server
        RestAssured.given().when().get("/lettuce/search/cursor/" + index + "/" + cursor).then().statusCode(500);
    }

    @Test
    public void searchAliases() {
        String index = getKey("idx-alias");
        String alias = getKey("alias-movie");
        createMovieIndex(index, getKey("alias") + ":");

        RestAssured.given().when().post("/lettuce/search/alias/" + alias + "/" + index).then().statusCode(204);
        assertEquals(2, searchCount(alias, "war"));
        // adding an existing alias fails, updating it does not
        RestAssured.given().when().post("/lettuce/search/alias/" + alias + "/" + index).then().statusCode(500);
        RestAssured.given().queryParam("update", true).when().post("/lettuce/search/alias/" + alias + "/" + index)
                .then().statusCode(204);

        RestAssured.given().when().delete("/lettuce/search/alias/" + alias).then().statusCode(204);
        RestAssured.given().queryParam("q", "war").when().get("/lettuce/search/query/" + alias).then().statusCode(500);
    }

    @Test
    public void searchAlterSchema() {
        String index = getKey("idx-alter");
        createMovieIndex(index, getKey("alter") + ":");
        assertEquals(0, searchCount(index, "brutally"));

        // the plot was stored in the hashes all along, but is only indexed once the schema has the field
        RestAssured.given().body("plot:TEXT").when().post("/lettuce/search/alter/" + index).then().statusCode(204);
        awaitIndexed(index, "brutally", 1);
    }

    @Test
    public void searchSynonymsDictionariesSpellCheck() {
        String index = getKey("idx-spell");
        String dict = getKey("dict");
        createMovieIndex(index, getKey("spell") + ":");

        // synonyms: the existing documents are re-indexed, so "battle" now matches the two "Star Wars" titles. A group
        // expands the exact indexed token, "wars", not the stem "war" a query would be reduced to
        RestAssured.given().when().get("/lettuce/search/synonyms/" + index).then()
                .statusCode(200).body("size()", CoreMatchers.is(0));
        RestAssured.given().body("wars,battle").when().post("/lettuce/search/synonyms/" + index + "/conflict").then()
                .statusCode(204);
        RestAssured.given().when().get("/lettuce/search/synonyms/" + index).then()
                .statusCode(200).body("conflict", CoreMatchers.equalTo(List.of("battle", "wars")));
        awaitIndexed(index, "battle", 2);

        // dictionaries are global: clear the words of a previous run first
        RestAssured.given().queryParam("words", "hello,bonjour").when().delete("/lettuce/search/dict/" + dict).then()
                .statusCode(204);
        RestAssured.given().when().get("/lettuce/search/dict/" + dict).then()
                .statusCode(200).body("size()", CoreMatchers.is(0));
        RestAssured.given().body("hello,bonjour").when().post("/lettuce/search/dict/" + dict).then().statusCode(204);
        RestAssured.given().when().get("/lettuce/search/dict/" + dict).then()
                .statusCode(200).body("$", CoreMatchers.equalTo(List.of("bonjour", "hello")));
        RestAssured.given().queryParam("words", "hello").when().delete("/lettuce/search/dict/" + dict).then()
                .statusCode(204);
        RestAssured.given().when().get("/lettuce/search/dict/" + dict).then()
                .statusCode(200).body("$", CoreMatchers.equalTo(List.of("bonjour")));

        // spell check against the terms of the index, then with the dictionary included
        RestAssured.given().queryParam("q", "godfathr").when().get("/lettuce/search/spellcheck/" + index).then()
                .statusCode(200)
                .body("correct", CoreMatchers.is(false))
                .body("suggestions.godfathr", CoreMatchers.equalTo(List.of("godfather")));
        RestAssured.given().queryParam("q", "bonjour").when().get("/lettuce/search/spellcheck/" + index).then()
                .statusCode(200)
                .body("correct", CoreMatchers.is(false))
                .body("suggestions.bonjour.size()", CoreMatchers.is(0));
        RestAssured.given().queryParam("q", "bonjour").queryParam("include", dict)
                .when().get("/lettuce/search/spellcheck/" + index).then()
                .statusCode(200).body("correct", CoreMatchers.is(true));
    }

    @Test
    public void searchTagVals() {
        String index = getKey("idx-tagvals");
        createMovieIndex(index, getKey("tagvals") + ":");

        RestAssured.given().when().get("/lettuce/search/tagvals/" + index + "/genre").then()
                .statusCode(200).body("$", CoreMatchers.equalTo(List.of("action", "drama", "thriller")));
        // TAGVALS is only valid on a TAG field
        RestAssured.given().when().get("/lettuce/search/tagvals/" + index + "/title").then().statusCode(500);
    }

    @Test
    public void searchJsonIndex() {
        String index = getKey("idx-json");
        String prefix = getKey("item") + ":";
        RestAssured.given().queryParam("dd", true).when().delete("/lettuce/search/index/" + index);
        RestAssured.given().queryParam("prefix", prefix).queryParam("json", true)
                .body("$.name=name:TEXT,$.price=price:NUMERIC")
                .when().post("/lettuce/search/index/" + index).then().statusCode(204);
        String headphones = "{\"name\":\"Noise-cancelling Bluetooth headphones\",\"price\":99.98}";
        String earbuds = "{\"name\":\"Wireless earbuds\",\"price\":64.99}";
        jsonSet(prefix + "1", headphones);
        jsonSet(prefix + "2", earbuds);
        awaitIndexed(index, "*", 2);

        // a JSON index returns the whole document under "$"
        String body = RestAssured.given().queryParam("q", "@name:(earbuds)").when()
                .get("/lettuce/search/query/" + index).then()
                .statusCode(200)
                .body("count", CoreMatchers.is(1))
                .body("documents[0].key", CoreMatchers.is(prefix + "2"))
                .extract().asString();
        String document = new JsonObject(body).getJsonArray("documents").getJsonObject(0)
                .getJsonObject("properties").getString("$");
        assertEquals(new JsonObject(earbuds), new JsonObject(document));

        RestAssured.given().queryParam("q", "@price:[0 70]").queryParam("return", "name")
                .when().get("/lettuce/search/query/" + index).then()
                .statusCode(200)
                .body("count", CoreMatchers.is(1))
                .body("documents[0].properties.name", CoreMatchers.is("Wireless earbuds"));
    }

    @Test
    public void searchReactive() {
        String index = getKey("idx-reactive");
        createMovieIndex(index, getKey("reactive") + ":");

        RestAssured.given().when().get("/lettuce/search/reactive/list").then()
                .statusCode(200).body("$", CoreMatchers.hasItem(index));
        RestAssured.given().queryParam("q", "%gdfather%").when().get("/lettuce/search/reactive/query/" + index).then()
                .statusCode(200)
                .body("count", CoreMatchers.is(1))
                .body("documents[0].properties.title", CoreMatchers.is("The Godfather"));
    }

    @Test
    public void withTransactionSearch() {
        String index = getKey("tx-search");
        String prefix = getKey("tx-movie") + ":";
        createMovieIndex(index, prefix);

        String body = RestAssured.given().when().post("/lettuce/with-transaction/search/" + index)
                .then().statusCode(200).extract().asString();
        // ft_list contains the index, tagvals -> the normalised genres, aliasadd -> null, syndump -> the group added
        // just before, dictdump after the del -> [bonjour], dropindex -> null
        assertEquals("false,11,true,[action, drama, thriller],true,[greetings],[hello, world],[bonjour],true", body);
        RestAssured.given().when().get("/lettuce/search/list").then()
                .statusCode(200).body("$", CoreMatchers.not(CoreMatchers.hasItem(index)));
        await(() -> RestAssured.given().when().get("/lettuce/key/exists/" + prefix + "11002")
                .then().statusCode(200).extract().asString().equals("false"),
                () -> "expected the documents of " + index + " to be deleted with it");
    }

}
