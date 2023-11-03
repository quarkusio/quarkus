package io.quarkus.mongodb.panache.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;

import io.quarkus.mongodb.panache.common.PanacheUpdate;
import io.quarkus.panache.common.Parameters;

class MongoOperationsTest {
    private final MongoOperations<Object, PanacheUpdate> operations = new MongoOperations() {
        @Override
        protected List<?> list(Object o) {
            return null;
        }

        @Override
        protected Stream<?> stream(Object o) {
            return null;
        }

        @Override
        protected Object createUpdate(MongoCollection collection, Class entityClass, Document docUpdate) {
            return null;
        }

        @Override
        protected Object createQuery(MongoCollection collection, ClientSession session, Document query, Document sortDoc) {
            return null;
        }
    };

    private static class DemoObj {
        public String field;
        public boolean isOk;
        @BsonProperty("value")
        public String property;
    }

    @BeforeAll
    static void setupFieldReplacement() {
        Map<String, Map<String, String>> replacementCache = new HashMap<>();
        Map<String, String> replacementMap = new HashMap<>();
        replacementMap.put("property", "value");
        replacementCache.put(DemoObj.class.getName(), replacementMap);
        replacementCache.put(Object.class.getName(), Collections.emptyMap());//because the test use Object ...
        MongoPropertyUtil.setReplacementCache(replacementCache);
    }

    @Test
    public void testBindShorthandFilter() {
        String query = operations.bindFilter(Object.class, "field", new Object[] { "a value" });
        assertEquals("{'field':'a value'}", query);

        query = operations.bindFilter(Object.class, "field", new Object[] { true });
        assertEquals("{'field':true}", query);

        query = operations.bindFilter(Object.class, "field", new Object[] { LocalDate.of(2019, 3, 4) });
        assertEquals("{'field':{\"$date\": \"2019-03-04T00:00:00.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "field", new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1) });
        assertEquals("{'field':{\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "field",
                new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC) });
        assertEquals("{'field':{\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "field",
                new Object[] { toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1)) });
        assertEquals("{'field':{\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "field",
                new Object[] { UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000") });
        assertEquals("{'field':UUID('7f000101-7370-1f68-8173-70afa71b0000')}", query);

        //test field replacement
        query = operations.bindFilter(DemoObj.class, "property", new Object[] { "a value" });
        assertEquals("{'value':'a value'}", query);
    }

    private Object toDate(LocalDateTime of) {
        return Date.from(of.atZone(ZoneId.of("UTC")).toInstant());
    }

    @Test
    public void testBindNativeFilterByIndex() {
        String query = operations.bindFilter(DemoObj.class, "{'field': ?1}", new Object[] { "a value" });
        assertEquals("{'field': 'a value'}", query);

        query = operations.bindFilter(DemoObj.class, "{'field.sub': ?1}", new Object[] { "a value" });
        assertEquals("{'field.sub': 'a value'}", query);

        //test that there are no field replacement for native queries
        query = operations.bindFilter(DemoObj.class, "{'property': ?1}", new Object[] { "a value" });
        assertEquals("{'property': 'a value'}", query);

        query = operations.bindFilter(Object.class, "{'field': ?1}",
                new Object[] { LocalDate.of(2019, 3, 4) });
        assertEquals("{'field': {\"$date\": \"2019-03-04T00:00:00.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "{'field': ?1}",
                new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1) });
        assertEquals("{'field': {\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "{'field': ?1}",
                new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC) });
        assertEquals("{'field': {\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "{'field': ?1}",
                new Object[] { toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1)) });
        assertEquals("{'field': {\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "{'field': ?1}",
                new Object[] { UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000") });
        assertEquals("{'field': UUID('7f000101-7370-1f68-8173-70afa71b0000')}", query);

        query = operations.bindFilter(Object.class, "{'field': ?1, 'isOk': ?2}", new Object[] { "a value", true });
        assertEquals("{'field': 'a value', 'isOk': true}", query);

        //queries related to '$in' operator
        List<Object> list = Arrays.asList("f1", "f2");
        query = operations.bindFilter(DemoObj.class, "{ field: { '$in': [?1] } }", new Object[] { list });
        assertEquals("{ field: { '$in': ['f1', 'f2'] } }", query);

        query = operations.bindFilter(DemoObj.class, "{ field: { '$in': [?1] }, isOk: ?2 }", new Object[] { list, true });
        assertEquals("{ field: { '$in': ['f1', 'f2'] }, isOk: true }", query);

        query = operations.bindFilter(DemoObj.class,
                "{ field: { '$in': [?1] }, $or: [ {'property': ?2}, {'property': ?3} ] }",
                new Object[] { list, "jpg", "gif" });
        assertEquals("{ field: { '$in': ['f1', 'f2'] }, $or: [ {'property': 'jpg'}, {'property': 'gif'} ] }", query);

        query = operations.bindFilter(DemoObj.class,
                "{ field: { '$in': [?1] }, isOk: ?2, $or: [ {'property': ?3}, {'property': ?4} ] }",
                new Object[] { list, true, "jpg", "gif" });
        assertEquals("{ field: { '$in': ['f1', 'f2'] }, isOk: true, $or: [ {'property': 'jpg'}, {'property': 'gif'} ] }",
                query);
    }

    @Test
    public void testBindNativeFilterByName() {
        String query = operations.bindFilter(Object.class, "{'field': :field}",
                Parameters.with("field", "a value").map());
        assertEquals("{'field': 'a value'}", query);

        query = operations.bindFilter(Object.class, "{'field.sub': :field}",
                Parameters.with("field", "a value").map());
        assertEquals("{'field.sub': 'a value'}", query);

        //test that there are no field replacement for native queries
        query = operations.bindFilter(DemoObj.class, "{'property': :field}",
                Parameters.with("field", "a value").map());
        assertEquals("{'property': 'a value'}", query);

        query = operations.bindFilter(Object.class, "{'field': :field}",
                Parameters.with("field", LocalDate.of(2019, 3, 4)).map());
        assertEquals("{'field': {\"$date\": \"2019-03-04T00:00:00.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "{'field': :field}",
                Parameters.with("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1)).map());
        assertEquals("{'field': {\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "{'field': :field}",
                Parameters.with("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC)).map());
        assertEquals("{'field': {\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "{'field': :field}",
                Parameters.with("field", toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1))).map());
        assertEquals("{'field': {\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "{'field': :field}",
                Parameters.with("field", UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000")).map());
        assertEquals("{'field': UUID('7f000101-7370-1f68-8173-70afa71b0000')}", query);

        query = operations.bindFilter(Object.class, "{'field': :field, 'isOk': :isOk}",
                Parameters.with("field", "a value").and("isOk", true).map());
        assertEquals("{'field': 'a value', 'isOk': true}", query);

        //queries related to '$in' operator
        List<Object> ids = Arrays.asList("f1", "f2");
        query = operations.bindFilter(DemoObj.class, "{ field: { '$in': [:fields] } }",
                Parameters.with("fields", ids).map());
        assertEquals("{ field: { '$in': ['f1', 'f2'] } }", query);

        query = operations.bindFilter(DemoObj.class, "{ field: { '$in': [:fields] }, isOk: :isOk }",
                Parameters.with("fields", ids).and("isOk", true).map());
        assertEquals("{ field: { '$in': ['f1', 'f2'] }, isOk: true }", query);

        query = operations.bindFilter(DemoObj.class,
                "{ field: { '$in': [:fields] }, $or: [ {'property': :p1}, {'property': :p2} ] }",
                Parameters.with("fields", ids).and("p1", "jpg").and("p2", "gif").map());
        assertEquals("{ field: { '$in': ['f1', 'f2'] }, $or: [ {'property': 'jpg'}, {'property': 'gif'} ] }", query);

        query = operations.bindFilter(DemoObj.class,
                "{ field: { '$in': [:fields] }, isOk: :isOk, $or: [ {'property': :p1}, {'property': :p2} ] }",
                Parameters.with("fields", ids)
                        .and("isOk", true)
                        .and("p1", "jpg")
                        .and("p2", "gif").map());
        assertEquals("{ field: { '$in': ['f1', 'f2'] }, isOk: true, $or: [ {'property': 'jpg'}, {'property': 'gif'} ] }",
                query);
    }

    @Test
    public void testBindEnhancedFilterByIndex() {
        String query = operations.bindFilter(Object.class, "field = ?1", new Object[] { "a value" });
        assertEquals("{'field':'a value'}", query);

        query = operations.bindFilter(Object.class, "{'field.sub': :field}",
                Parameters.with("field", "a value").map());
        assertEquals("{'field.sub': 'a value'}", query);

        //test field replacement
        query = operations.bindFilter(DemoObj.class, "property = ?1", new Object[] { "a value" });
        assertEquals("{'value':'a value'}", query);

        query = operations.bindFilter(Object.class, "field = ?1", new Object[] { LocalDate.of(2019, 3, 4) });
        assertEquals("{'field':{\"$date\": \"2019-03-04T00:00:00.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "field = ?1", new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1) });
        assertEquals("{'field':{\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "field = ?1",
                new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC) });
        assertEquals("{'field':{\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "field = ?1",
                new Object[] { toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1)) });
        assertEquals("{'field':{\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "field = ?1",
                new Object[] { UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000") });
        assertEquals("{'field':UUID('7f000101-7370-1f68-8173-70afa71b0000')}", query);

        query = operations.bindFilter(Object.class, "field = ?1 and isOk = ?2", new Object[] { "a value", true });
        assertEquals("{'field':'a value','isOk':true}", query);

        query = operations.bindFilter(Object.class, "field = ?1 or isOk = ?2", new Object[] { "a value", true });
        assertEquals("{'$or':[{'field':'a value'},{'isOk':true}]}", query);

        query = operations.bindFilter(Object.class, "count >= ?1 and count < ?2", new Object[] { 5, 10 });
        assertEquals("{'count':{'$gte':5},'count':{'$lt':10}}", query);

        query = operations.bindFilter(Object.class, "field != ?1", new Object[] { "a value" });
        assertEquals("{'field':{'$ne':'a value'}}", query);

        query = operations.bindFilter(Object.class, "field like ?1", new Object[] { "a value" });
        assertEquals("{'field':{'$regex':'a value'}}", query);

        query = operations.bindFilter(Object.class, "field is not null", new Object[] {});
        assertEquals("{'field':{'$exists':true}}", query);

        query = operations.bindFilter(Object.class, "field is null", new Object[] {});
        assertEquals("{'field':{'$exists':false}}", query);

        // test with hardcoded value
        query = operations.bindFilter(Object.class, "field = 'some hardcoded value'", new Object[] {});
        assertEquals("{'field':'some hardcoded value'}", query);

        //queries related to '$in' operator
        List<Object> list = Arrays.asList("f1", "f2");
        query = operations.bindFilter(DemoObj.class, "field in ?1", new Object[] { list });
        assertEquals("{'field':{'$in':['f1', 'f2']}}", query);

        query = operations.bindFilter(DemoObj.class, "field in ?1 and isOk = ?2", new Object[] { list, true });
        assertEquals("{'field':{'$in':['f1', 'f2']},'isOk':true}", query);

        query = operations.bindFilter(DemoObj.class,
                "field in ?1 and property = ?2 or property = ?3",
                new Object[] { list, "jpg", "gif" });
        assertEquals("{'field':{'$in':['f1', 'f2']},'$or':[{'value':'jpg'},{'value':'gif'}]}", query);

        query = operations.bindFilter(DemoObj.class,
                "field in ?1 and isOk = ?2 and property = ?3 or property = ?4",
                new Object[] { list, true, "jpg", "gif" });
        assertEquals("{'field':{'$in':['f1', 'f2']},'isOk':true,'$or':[{'value':'jpg'},{'value':'gif'}]}", query);
    }

    @Test
    public void testBindEnhancedFilterByName() {
        String query = operations.bindFilter(Object.class, "field = :field",
                Parameters.with("field", "a value").map());
        assertEquals("{'field':'a value'}", query);

        query = operations.bindFilter(Object.class, "field.sub = :field",
                Parameters.with("field", "a value").map());
        assertEquals("{'field.sub':'a value'}", query);

        //test field replacement
        query = operations.bindFilter(DemoObj.class, "property = :field",
                Parameters.with("field", "a value").map());
        assertEquals("{'value':'a value'}", query);

        query = operations.bindFilter(Object.class, "field = :field",
                Parameters.with("field", LocalDate.of(2019, 3, 4)).map());
        assertEquals("{'field':{\"$date\": \"2019-03-04T00:00:00.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "field = :field",
                Parameters.with("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1)).map());
        assertEquals("{'field':{\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "field = :field",
                Parameters.with("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC)).map());
        assertEquals("{'field':{\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "field = :field",
                Parameters.with("field", toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1))).map());
        assertEquals("{'field':{\"$date\": \"2019-03-04T01:01:01.000Z\"} }", query);

        query = operations.bindFilter(Object.class, "field = :field",
                Parameters.with("field", UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000")).map());
        assertEquals("{'field':UUID('7f000101-7370-1f68-8173-70afa71b0000')}", query);

        query = operations.bindFilter(Object.class, "field = :field and isOk = :isOk",
                Parameters.with("field", "a value").and("isOk", true).map());
        assertEquals("{'field':'a value','isOk':true}", query);

        query = operations.bindFilter(Object.class, "field = :field or isOk = :isOk",
                Parameters.with("field", "a value").and("isOk", true).map());
        assertEquals("{'$or':[{'field':'a value'},{'isOk':true}]}", query);

        query = operations.bindFilter(Object.class, "count > :lower and count <= :upper",
                Parameters.with("lower", 5).and("upper", 10).map());
        assertEquals("{'count':{'$gt':5},'count':{'$lte':10}}", query);

        query = operations.bindFilter(Object.class, "field != :field",
                Parameters.with("field", "a value").map());
        assertEquals("{'field':{'$ne':'a value'}}", query);

        query = operations.bindFilter(Object.class, "field like :field",
                Parameters.with("field", "a value").map());
        assertEquals("{'field':{'$regex':'a value'}}", query);

        //queries related to '$in' operator
        List<Object> list = Arrays.asList("f1", "f2");
        query = operations.bindFilter(DemoObj.class, "field in :fields",
                Parameters.with("fields", list).map());
        assertEquals("{'field':{'$in':['f1', 'f2']}}", query);

        query = operations.bindFilter(DemoObj.class, "field in :fields and isOk = :isOk",
                Parameters.with("fields", list).and("isOk", true).map());
        assertEquals("{'field':{'$in':['f1', 'f2']},'isOk':true}", query);

        query = operations.bindFilter(DemoObj.class,
                "field in :fields and property = :p1 or property = :p2",
                Parameters.with("fields", list).and("p1", "jpg").and("p2", "gif").map());
        assertEquals("{'field':{'$in':['f1', 'f2']},'$or':[{'value':'jpg'},{'value':'gif'}]}", query);

        query = operations.bindFilter(DemoObj.class,
                "field in :fields and isOk = :isOk and property = :p1 or property = :p2",
                Parameters.with("fields", list)
                        .and("isOk", true)
                        .and("p1", "jpg")
                        .and("p2", "gif").map());
        assertEquals("{'field':{'$in':['f1', 'f2']},'isOk':true,'$or':[{'value':'jpg'},{'value':'gif'}]}", query);
    }

    @Test
    public void testBindUpdate() {
        // native update by index without $set
        String update = operations.bindUpdate(DemoObj.class, "{'field': ?1}", new Object[] { "a value" });
        assertEquals("{'$set':{'field': 'a value'}}", update);

        // native update by name without $set
        update = operations.bindUpdate(Object.class, "{'field': :field}",
                Parameters.with("field", "a value").map());
        assertEquals("{'$set':{'field': 'a value'}}", update);

        // native update by index with $set
        update = operations.bindUpdate(DemoObj.class, "{'$set':{'field': ?1}}", new Object[] { "a value" });
        assertEquals("{'$set':{'field': 'a value'}}", update);

        // native update by name with $set
        update = operations.bindUpdate(Object.class, "{'$set':{'field': :field}}",
                Parameters.with("field", "a value").map());
        assertEquals("{'$set':{'field': 'a value'}}", update);

        // native update by index with $inc
        update = operations.bindUpdate(DemoObj.class, "{'$inc':{'field': ?1}}", new Object[] { "a value" });
        assertEquals("{'$inc':{'field': 'a value'}}", update);

        // native update by name with $inc
        update = operations.bindUpdate(Object.class, "{'$inc':{'field': :field}}",
                Parameters.with("field", "a value").map());
        assertEquals("{'$inc':{'field': 'a value'}}", update);

        // shortand update
        update = operations.bindUpdate(Object.class, "field", new Object[] { "a value" });
        assertEquals("{'$set':{'field':'a value'}}", update);

        // enhanced update by index
        update = operations.bindUpdate(Object.class, "field = ?1", new Object[] { "a value" });
        assertEquals("{'$set':{'field':'a value'}}", update);

        // enhanced update by name
        update = operations.bindUpdate(Object.class, "field = :field",
                Parameters.with("field", "a value").map());
        assertEquals("{'$set':{'field':'a value'}}", update);
    }
}
