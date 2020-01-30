package io.quarkus.mongodb.panache.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.panache.common.Parameters;

class MongoOperationsTest {

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
    public void testBindShorthandQuery() {
        String query = MongoOperations.bindQuery(Object.class, "field", new Object[] { "a value" });
        assertEquals("{'field':'a value'}", query);

        query = MongoOperations.bindQuery(Object.class, "field", new Object[] { true });
        assertEquals("{'field':true}", query);

        query = MongoOperations.bindQuery(Object.class, "field", new Object[] { LocalDate.of(2019, 3, 4) });
        assertEquals("{'field':ISODate('2019-03-04')}", query);

        query = MongoOperations.bindQuery(Object.class, "field", new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1) });
        assertEquals("{'field':ISODate('2019-03-04T01:01:01')}", query);

        query = MongoOperations.bindQuery(Object.class, "field",
                new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC) });
        assertEquals("{'field':ISODate('2019-03-04T01:01:01')}", query);

        query = MongoOperations.bindQuery(Object.class, "field",
                new Object[] { toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1)) });
        assertEquals("{'field':ISODate('2019-03-04T01:01:01.000')}", query);

        //test field replacement
        query = MongoOperations.bindQuery(DemoObj.class, "property", new Object[] { "a value" });
        assertEquals("{'value':'a value'}", query);
    }

    private Object toDate(LocalDateTime of) {
        return Date.from(of.atZone(ZoneId.systemDefault()).toInstant());
    }

    @Test
    public void testBindNativeQueryByIndex() {
        String query = MongoOperations.bindQuery(DemoObj.class, "{'field': ?1}", new Object[] { "a value" });
        assertEquals("{'field': 'a value'}", query);

        query = MongoOperations.bindQuery(DemoObj.class, "{'field.sub': ?1}", new Object[] { "a value" });
        assertEquals("{'field.sub': 'a value'}", query);

        //test that there are no field replacement for native queries
        query = MongoOperations.bindQuery(DemoObj.class, "{'property': ?1}", new Object[] { "a value" });
        assertEquals("{'property': 'a value'}", query);

        query = MongoOperations.bindQuery(Object.class, "{'field': ?1}",
                new Object[] { LocalDate.of(2019, 3, 4) });
        assertEquals("{'field': ISODate('2019-03-04')}", query);

        query = MongoOperations.bindQuery(Object.class, "{'field': ?1}",
                new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1) });
        assertEquals("{'field': ISODate('2019-03-04T01:01:01')}", query);

        query = MongoOperations.bindQuery(Object.class, "{'field': ?1}",
                new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC) });
        assertEquals("{'field': ISODate('2019-03-04T01:01:01')}", query);

        query = MongoOperations.bindQuery(Object.class, "{'field': ?1}",
                new Object[] { toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1)) });
        assertEquals("{'field': ISODate('2019-03-04T01:01:01.000')}", query);

        query = MongoOperations.bindQuery(Object.class, "{'field': ?1, 'isOk': ?2}", new Object[] { "a value", true });
        assertEquals("{'field': 'a value', 'isOk': true}", query);
    }

    @Test
    public void testBindNativeQueryByName() {
        String query = MongoOperations.bindQuery(Object.class, "{'field': :field}",
                Parameters.with("field", "a value").map());
        assertEquals("{'field': 'a value'}", query);

        query = MongoOperations.bindQuery(Object.class, "{'field.sub': :field}",
                Parameters.with("field", "a value").map());
        assertEquals("{'field.sub': 'a value'}", query);

        //test that there are no field replacement for native queries
        query = MongoOperations.bindQuery(DemoObj.class, "{'property': :field}",
                Parameters.with("field", "a value").map());
        assertEquals("{'property': 'a value'}", query);

        query = MongoOperations.bindQuery(Object.class, "{'field': :field}",
                Parameters.with("field", LocalDate.of(2019, 3, 4)).map());
        assertEquals("{'field': ISODate('2019-03-04')}", query);

        query = MongoOperations.bindQuery(Object.class, "{'field': :field}",
                Parameters.with("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1)).map());
        assertEquals("{'field': ISODate('2019-03-04T01:01:01')}", query);

        query = MongoOperations.bindQuery(Object.class, "{'field': :field}",
                Parameters.with("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC)).map());
        assertEquals("{'field': ISODate('2019-03-04T01:01:01')}", query);

        query = MongoOperations.bindQuery(Object.class, "{'field': :field}",
                Parameters.with("field", toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1))).map());
        assertEquals("{'field': ISODate('2019-03-04T01:01:01.000')}", query);

        query = MongoOperations.bindQuery(Object.class, "{'field': :field, 'isOk': :isOk}",
                Parameters.with("field", "a value").and("isOk", true).map());
        assertEquals("{'field': 'a value', 'isOk': true}", query);
    }

    @Test
    public void testBindEnhancedQueryByIndex() {
        String query = MongoOperations.bindQuery(Object.class, "field = ?1", new Object[] { "a value" });
        assertEquals("{'field':'a value'}", query);

        query = MongoOperations.bindQuery(Object.class, "{'field.sub': :field}",
                Parameters.with("field", "a value").map());
        assertEquals("{'field.sub': 'a value'}", query);

        //test field replacement
        query = MongoOperations.bindQuery(DemoObj.class, "property = ?1", new Object[] { "a value" });
        assertEquals("{'value':'a value'}", query);

        query = MongoOperations.bindQuery(Object.class, "field = ?1", new Object[] { LocalDate.of(2019, 3, 4) });
        assertEquals("{'field':ISODate('2019-03-04')}", query);

        query = MongoOperations.bindQuery(Object.class, "field = ?1", new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1) });
        assertEquals("{'field':ISODate('2019-03-04T01:01:01')}", query);

        query = MongoOperations.bindQuery(Object.class, "field = ?1",
                new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC) });
        assertEquals("{'field':ISODate('2019-03-04T01:01:01')}", query);

        query = MongoOperations.bindQuery(Object.class, "field = ?1",
                new Object[] { toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1)) });
        assertEquals("{'field':ISODate('2019-03-04T01:01:01.000')}", query);

        query = MongoOperations.bindQuery(Object.class, "field = ?1 and isOk = ?2", new Object[] { "a value", true });
        assertEquals("{'field':'a value','isOk':true}", query);

        query = MongoOperations.bindQuery(Object.class, "field = ?1 or isOk = ?2", new Object[] { "a value", true });
        assertEquals("{'$or':[{'field':'a value'},{'isOk':true}]}", query);

        query = MongoOperations.bindQuery(Object.class, "count >= ?1 and count < ?2", new Object[] { 5, 10 });
        assertEquals("{'count':{'$gte':5},'count':{'$lt':10}}", query);

        query = MongoOperations.bindQuery(Object.class, "field != ?1", new Object[] { "a value" });
        assertEquals("{'field':{'$ne':'a value'}}", query);

        query = MongoOperations.bindQuery(Object.class, "field like ?1", new Object[] { "a value" });
        assertEquals("{'field':{'$regex':'a value'}}", query);

        query = MongoOperations.bindQuery(Object.class, "field is not null", new Object[] {});
        assertEquals("{'field':{'$exists':true}}", query);

        query = MongoOperations.bindQuery(Object.class, "field is null", new Object[] {});
        assertEquals("{'field':{'$exists':false}}", query);

        // test with hardcoded value
        query = MongoOperations.bindQuery(Object.class, "field = 'some hardcoded value'", new Object[] {});
        assertEquals("{'field':'some hardcoded value'}", query);
    }

    @Test
    public void testBindEnhancedQueryByName() {
        String query = MongoOperations.bindQuery(Object.class, "field = :field",
                Parameters.with("field", "a value").map());
        assertEquals("{'field':'a value'}", query);

        query = MongoOperations.bindQuery(Object.class, "field.sub = :field",
                Parameters.with("field", "a value").map());
        assertEquals("{'field.sub':'a value'}", query);

        //test field replacement
        query = MongoOperations.bindQuery(DemoObj.class, "property = :field",
                Parameters.with("field", "a value").map());
        assertEquals("{'value':'a value'}", query);

        query = MongoOperations.bindQuery(Object.class, "field = :field",
                Parameters.with("field", LocalDate.of(2019, 3, 4)).map());
        assertEquals("{'field':ISODate('2019-03-04')}", query);

        query = MongoOperations.bindQuery(Object.class, "field = :field",
                Parameters.with("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1)).map());
        assertEquals("{'field':ISODate('2019-03-04T01:01:01')}", query);

        query = MongoOperations.bindQuery(Object.class, "field = :field",
                Parameters.with("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC)).map());
        assertEquals("{'field':ISODate('2019-03-04T01:01:01')}", query);

        query = MongoOperations.bindQuery(Object.class, "field = :field",
                Parameters.with("field", toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1))).map());
        assertEquals("{'field':ISODate('2019-03-04T01:01:01.000')}", query);

        query = MongoOperations.bindQuery(Object.class, "field = :field and isOk = :isOk",
                Parameters.with("field", "a value").and("isOk", true).map());
        assertEquals("{'field':'a value','isOk':true}", query);

        query = MongoOperations.bindQuery(Object.class, "field = :field or isOk = :isOk",
                Parameters.with("field", "a value").and("isOk", true).map());
        assertEquals("{'$or':[{'field':'a value'},{'isOk':true}]}", query);

        query = MongoOperations.bindQuery(Object.class, "count > :lower and count <= :upper",
                Parameters.with("lower", 5).and("upper", 10).map());
        assertEquals("{'count':{'$gt':5},'count':{'$lte':10}}", query);

        query = MongoOperations.bindQuery(Object.class, "field != :field",
                Parameters.with("field", "a value").map());
        assertEquals("{'field':{'$ne':'a value'}}", query);

        query = MongoOperations.bindQuery(Object.class, "field like :field",
                Parameters.with("field", "a value").map());
        assertEquals("{'field':{'$regex':'a value'}}", query);
    }

}
