package io.quarkus.mongodb.panache.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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

import org.bson.BsonDocument;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.UuidRepresentation;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.StringCodec;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

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
        protected Object createUpdate(MongoCollection collection, Class entityClass, Bson docUpdate) {
            return null;
        }

        @Override
        protected Object createQuery(MongoCollection collection, ClientSession session, Bson query, Bson sortDoc) {
            return null;
        }
    };

    private static class DemoObj {
        public String field;
        public List<String> listField;
        public boolean isOk;
        @BsonProperty("value")
        public String property;
    }

    private enum TestEnum {
        VALUE_A,
        VALUE_B
    }

    private static class CustomType {
        private final String inner;

        CustomType(String inner) {
            this.inner = inner;
        }

        public String getInner() {
            return inner;
        }

        @Override
        public String toString() {
            return "CustomType{" + inner + "}";
        }
    }

    private static class CustomTypeCodec implements Codec<CustomType> {
        private final StringCodec stringCodec = new StringCodec();

        @Override
        public void encode(BsonWriter writer, CustomType value, EncoderContext encoderContext) {
            stringCodec.encode(writer, value.getInner(), encoderContext);
        }

        @Override
        public CustomType decode(BsonReader reader, DecoderContext decoderContext) {
            return new CustomType(stringCodec.decode(reader, decoderContext));
        }

        @Override
        public Class<CustomType> getEncoderClass() {
            return CustomType.class;
        }
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

    private static final CodecRegistry CODEC_REGISTRY = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD), new CustomTypeCodec()),
            MongoClientSettings.getDefaultCodecRegistry());

    private static BsonDocument toBsonDoc(Bson bson) {
        return bson.toBsonDocument(BsonDocument.class, CODEC_REGISTRY);
    }

    private static void assertBsonEquals(Bson expected, Bson actual) {
        assertEquals(toBsonDoc(expected), toBsonDoc(actual));
    }

    @Test
    public void testBindShorthandFilter() {
        Bson result = operations.bindFilter(Object.class, "field", new Object[] { "a value" });
        assertBsonEquals(Filters.eq("field", "a value"), result);

        result = operations.bindFilter(Object.class, "field", new Object[] { true });
        assertBsonEquals(Filters.eq("field", true), result);

        result = operations.bindFilter(Object.class, "field", new Object[] { LocalDate.of(2019, 3, 4) });
        assertBsonEquals(Filters.eq("field", LocalDate.of(2019, 3, 4)), result);

        result = operations.bindFilter(Object.class, "field", new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1) });
        assertBsonEquals(Filters.eq("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1)), result);

        result = operations.bindFilter(Object.class, "field",
                new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC) });
        assertBsonEquals(Filters.eq("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC)), result);

        result = operations.bindFilter(Object.class, "field",
                new Object[] { toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1)) });
        assertBsonEquals(Filters.eq("field", toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1))), result);

        result = operations.bindFilter(Object.class, "field",
                new Object[] { UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000") });
        assertBsonEquals(Filters.eq("field", UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000")), result);

        // test field replacement
        result = operations.bindFilter(DemoObj.class, "property", new Object[] { "a value" });
        assertBsonEquals(Filters.eq("value", "a value"), result);

        // keywords (quoted)
        result = operations.bindFilter(Object.class, "`instant` = ?1", new Object[] { "a value" });
        assertBsonEquals(Filters.eq("instant", "a value"), result);

        // keywords (unquoted)
        result = operations.bindFilter(Object.class, "instant = ?1", new Object[] { "a value" });
        assertBsonEquals(Filters.eq("instant", "a value"), result);

        // null value
        result = operations.bindFilter(Object.class, "field", new Object[] { null });
        assertBsonEquals(Filters.eq("field", null), result);
    }

    @Test
    public void testBindShorthandFilterWithCustomType() {
        CustomType custom = new CustomType("test");
        Bson result = operations.bindFilter(Object.class, "field", new Object[] { custom });
        // the raw CustomType object is preserved, and the codec encodes it as "test" (not toString())
        assertBsonEquals(Filters.eq("field", custom), result);
    }

    @Test
    public void testBindShorthandFilterWithEnum() {
        Bson result = operations.bindFilter(Object.class, "field", new Object[] { TestEnum.VALUE_A });
        assertBsonEquals(Filters.eq("field", "VALUE_A"), result);
    }

    @Test
    public void testBindShorthandFilterWithObjectId() {
        ObjectId objectId = new ObjectId("507f1f77bcf86cd799439011");
        Bson result = operations.bindFilter(Object.class, "field", new Object[] { objectId });
        assertBsonEquals(Filters.eq("field", objectId), result);
    }

    private Object toDate(LocalDateTime of) {
        return Date.from(of.atZone(ZoneId.of("UTC")).toInstant());
    }

    @Test
    public void testBindNativeFilterByIndex() {
        Bson result = operations.bindFilter(DemoObj.class, "{'field': ?1}", new Object[] { "a value" });
        assertBsonEquals(Filters.eq("field", "a value"), result);

        result = operations.bindFilter(DemoObj.class, "{'field.sub': ?1}", new Object[] { "a value" });
        assertBsonEquals(Filters.eq("field.sub", "a value"), result);

        // test that there are no field replacement for native queries
        result = operations.bindFilter(DemoObj.class, "{'property': ?1}", new Object[] { "a value" });
        assertBsonEquals(Filters.eq("property", "a value"), result);

        result = operations.bindFilter(Object.class, "{'field': ?1}",
                new Object[] { LocalDate.of(2019, 3, 4) });
        assertBsonEquals(Filters.eq("field", LocalDate.of(2019, 3, 4)), result);

        result = operations.bindFilter(Object.class, "{'field': ?1}",
                new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1) });
        assertBsonEquals(Filters.eq("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1)), result);

        result = operations.bindFilter(Object.class, "{'field': ?1}",
                new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC) });
        assertBsonEquals(Filters.eq("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC)), result);

        result = operations.bindFilter(Object.class, "{'field': ?1}",
                new Object[] { toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1)) });
        assertBsonEquals(Filters.eq("field", toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1))), result);

        result = operations.bindFilter(Object.class, "{'field': ?1}",
                new Object[] { UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000") });
        assertBsonEquals(Filters.eq("field", UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000")), result);

        result = operations.bindFilter(Object.class, "{'field': ?1, 'isOk': ?2}", new Object[] { "a value", true });
        assertInstanceOf(org.bson.Document.class, result);
        org.bson.Document doc = (org.bson.Document) result;
        assertEquals("a value", doc.get("field"));
        assertEquals(true, doc.get("isOk"));

        // queries related to '$in' operator
        List<Object> list = Arrays.asList("f1", "f2");
        result = operations.bindFilter(DemoObj.class, "{ field: { '$in': ?1 } }", new Object[] { list });
        assertInstanceOf(org.bson.Document.class, result);
        doc = (org.bson.Document) result;
        org.bson.Document inDoc = (org.bson.Document) doc.get("field");
        assertEquals(list, inDoc.get("$in"));

        result = operations.bindFilter(DemoObj.class, "{ field: { '$in': ?1 }, isOk: ?2 }", new Object[] { list, true });
        assertInstanceOf(org.bson.Document.class, result);
        doc = (org.bson.Document) result;
        inDoc = (org.bson.Document) doc.get("field");
        assertEquals(list, inDoc.get("$in"));
        assertEquals(true, doc.get("isOk"));

        result = operations.bindFilter(DemoObj.class,
                "{ field: { '$in': ?1 }, $or: [ {'property': ?2}, {'property': ?3} ] }",
                new Object[] { list, "jpg", "gif" });
        assertInstanceOf(org.bson.Document.class, result);
        doc = (org.bson.Document) result;
        inDoc = (org.bson.Document) doc.get("field");
        assertEquals(list, inDoc.get("$in"));
        List<?> orList = (List<?>) doc.get("$or");
        assertEquals(2, orList.size());
        assertEquals("jpg", ((org.bson.Document) orList.get(0)).get("property"));
        assertEquals("gif", ((org.bson.Document) orList.get(1)).get("property"));

        result = operations.bindFilter(DemoObj.class,
                "{ field: { '$in': ?1 }, isOk: ?2, $or: [ {'property': ?3}, {'property': ?4} ] }",
                new Object[] { list, true, "jpg", "gif" });
        assertInstanceOf(org.bson.Document.class, result);
        doc = (org.bson.Document) result;
        inDoc = (org.bson.Document) doc.get("field");
        assertEquals(list, inDoc.get("$in"));
        assertEquals(true, doc.get("isOk"));
        orList = (List<?>) doc.get("$or");
        assertEquals(2, orList.size());
        assertEquals("jpg", ((org.bson.Document) orList.get(0)).get("property"));
        assertEquals("gif", ((org.bson.Document) orList.get(1)).get("property"));
    }

    @Test
    public void testBindNativeFilterByIndexWithCustomType() {
        CustomType custom = new CustomType("test");
        Bson result = operations.bindFilter(DemoObj.class, "{'field': ?1}", new Object[] { custom });
        assertBsonEquals(Filters.eq("field", custom), result);
    }

    @Test
    public void testBindNativeFilterByIndexWithEnum() {
        Bson result = operations.bindFilter(DemoObj.class, "{'field': ?1}", new Object[] { TestEnum.VALUE_A });
        assertBsonEquals(Filters.eq("field", "VALUE_A"), result);
    }

    @Test
    public void testBindNativeFilterByName() {
        Bson result = operations.bindFilter(Object.class, "{'field': :field}",
                Parameters.with("field", "a value").map());
        assertBsonEquals(Filters.eq("field", "a value"), result);

        result = operations.bindFilter(Object.class, "{'field.sub': :field}",
                Parameters.with("field", "a value").map());
        assertBsonEquals(Filters.eq("field.sub", "a value"), result);

        // test that there are no field replacement for native queries
        result = operations.bindFilter(DemoObj.class, "{'property': :field}",
                Parameters.with("field", "a value").map());
        assertBsonEquals(Filters.eq("property", "a value"), result);

        result = operations.bindFilter(Object.class, "{'field': :field}",
                Parameters.with("field", LocalDate.of(2019, 3, 4)).map());
        assertBsonEquals(Filters.eq("field", LocalDate.of(2019, 3, 4)), result);

        result = operations.bindFilter(Object.class, "{'field': :field}",
                Parameters.with("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1)).map());
        assertBsonEquals(Filters.eq("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1)), result);

        result = operations.bindFilter(Object.class, "{'field': :field}",
                Parameters.with("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC)).map());
        assertBsonEquals(Filters.eq("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC)), result);

        result = operations.bindFilter(Object.class, "{'field': :field}",
                Parameters.with("field", toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1))).map());
        assertBsonEquals(Filters.eq("field", toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1))), result);

        result = operations.bindFilter(Object.class, "{'field': :field}",
                Parameters.with("field", UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000")).map());
        assertBsonEquals(Filters.eq("field", UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000")), result);

        result = operations.bindFilter(Object.class, "{'field': :field, 'isOk': :isOk}",
                Parameters.with("field", "a value").and("isOk", true).map());
        assertInstanceOf(org.bson.Document.class, result);
        org.bson.Document doc = (org.bson.Document) result;
        assertEquals("a value", doc.get("field"));
        assertEquals(true, doc.get("isOk"));

        // queries related to '$in' operator
        List<Object> ids = Arrays.asList("f1", "f2");
        result = operations.bindFilter(DemoObj.class, "{ field: { '$in': :fields } }",
                Parameters.with("fields", ids).map());
        assertInstanceOf(org.bson.Document.class, result);
        doc = (org.bson.Document) result;
        org.bson.Document inDoc = (org.bson.Document) doc.get("field");
        assertEquals(ids, inDoc.get("$in"));

        result = operations.bindFilter(DemoObj.class, "{ field: { '$in': :fields }, isOk: :isOk }",
                Parameters.with("fields", ids).and("isOk", true).map());
        assertInstanceOf(org.bson.Document.class, result);
        doc = (org.bson.Document) result;
        inDoc = (org.bson.Document) doc.get("field");
        assertEquals(ids, inDoc.get("$in"));
        assertEquals(true, doc.get("isOk"));

        result = operations.bindFilter(DemoObj.class,
                "{ field: { '$in': :fields }, $or: [ {'property': :p1}, {'property': :p2} ] }",
                Parameters.with("fields", ids).and("p1", "jpg").and("p2", "gif").map());
        assertInstanceOf(org.bson.Document.class, result);
        doc = (org.bson.Document) result;
        inDoc = (org.bson.Document) doc.get("field");
        assertEquals(ids, inDoc.get("$in"));
        List<?> orList = (List<?>) doc.get("$or");
        assertEquals(2, orList.size());
        assertEquals("jpg", ((org.bson.Document) orList.get(0)).get("property"));
        assertEquals("gif", ((org.bson.Document) orList.get(1)).get("property"));

        result = operations.bindFilter(DemoObj.class,
                "{ field: { '$in': :fields }, isOk: :isOk, $or: [ {'property': :p1}, {'property': :p2} ] }",
                Parameters.with("fields", ids)
                        .and("isOk", true)
                        .and("p1", "jpg")
                        .and("p2", "gif").map());
        assertInstanceOf(org.bson.Document.class, result);
        doc = (org.bson.Document) result;
        inDoc = (org.bson.Document) doc.get("field");
        assertEquals(ids, inDoc.get("$in"));
        assertEquals(true, doc.get("isOk"));
        orList = (List<?>) doc.get("$or");
        assertEquals(2, orList.size());
        assertEquals("jpg", ((org.bson.Document) orList.get(0)).get("property"));
        assertEquals("gif", ((org.bson.Document) orList.get(1)).get("property"));
    }

    @Test
    public void testBindNativeFilterByNameWithCustomType() {
        CustomType custom = new CustomType("test");
        Bson result = operations.bindFilter(DemoObj.class, "{'field': :field}",
                Parameters.with("field", custom).map());
        assertBsonEquals(Filters.eq("field", custom), result);
    }

    @Test
    public void testBindEnhancedFilterByIndex() {
        Bson result = operations.bindFilter(Object.class, "field = ?1", new Object[] { "a value" });
        assertBsonEquals(Filters.eq("field", "a value"), result);

        result = operations.bindFilter(Object.class, "{'field.sub': :field}",
                Parameters.with("field", "a value").map());
        assertBsonEquals(Filters.eq("field.sub", "a value"), result);

        // test field replacement
        result = operations.bindFilter(DemoObj.class, "property = ?1", new Object[] { "a value" });
        assertBsonEquals(Filters.eq("value", "a value"), result);

        result = operations.bindFilter(Object.class, "field = ?1", new Object[] { LocalDate.of(2019, 3, 4) });
        assertBsonEquals(Filters.eq("field", LocalDate.of(2019, 3, 4)), result);

        result = operations.bindFilter(Object.class, "field = ?1", new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1) });
        assertBsonEquals(Filters.eq("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1)), result);

        result = operations.bindFilter(Object.class, "field = ?1",
                new Object[] { LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC) });
        assertBsonEquals(Filters.eq("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC)), result);

        result = operations.bindFilter(Object.class, "field = ?1",
                new Object[] { toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1)) });
        assertBsonEquals(Filters.eq("field", toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1))), result);

        result = operations.bindFilter(Object.class, "field = ?1",
                new Object[] { UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000") });
        assertBsonEquals(Filters.eq("field", UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000")), result);

        result = operations.bindFilter(Object.class, "field = ?1 and isOk = ?2", new Object[] { "a value", true });
        assertBsonEquals(Filters.and(Filters.eq("field", "a value"), Filters.eq("isOk", true)), result);

        result = operations.bindFilter(Object.class, "field = ?1 or isOk = ?2", new Object[] { "a value", true });
        assertBsonEquals(Filters.or(Filters.eq("field", "a value"), Filters.eq("isOk", true)), result);

        result = operations.bindFilter(Object.class, "count >= ?1 and count < ?2", new Object[] { 5, 10 });
        assertBsonEquals(Filters.and(Filters.gte("count", 5), Filters.lt("count", 10)), result);

        result = operations.bindFilter(Object.class, "field != ?1", new Object[] { "a value" });
        assertBsonEquals(Filters.ne("field", "a value"), result);

        result = operations.bindFilter(Object.class, "field like ?1", new Object[] { "a value" });
        assertBsonEquals(Filters.regex("field", "a value"), result);

        // regex with JavaScript syntax
        result = operations.bindFilter(Object.class, "field like ?1", new Object[] { "/uppercase.*/i" });
        assertBsonEquals(Filters.regex("field", "uppercase.*", "i"), result);

        result = operations.bindFilter(Object.class, "field is not null", new Object[] {});
        assertBsonEquals(Filters.exists("field", true), result);

        result = operations.bindFilter(Object.class, "field is null", new Object[] {});
        assertBsonEquals(Filters.exists("field", false), result);

        // test with hardcoded value
        result = operations.bindFilter(Object.class, "field = 'some hardcoded value'", new Object[] {});
        assertBsonEquals(Filters.eq("field", "some hardcoded value"), result);

        // queries related to '$in' operator
        List<Object> list = Arrays.asList("f1", "f2");
        result = operations.bindFilter(DemoObj.class, "field in ?1", new Object[] { list });
        assertBsonEquals(Filters.in("field", list), result);

        result = operations.bindFilter(DemoObj.class, "field in ?1 and isOk = ?2", new Object[] { list, true });
        assertBsonEquals(Filters.and(Filters.in("field", list), Filters.eq("isOk", true)), result);

        result = operations.bindFilter(DemoObj.class,
                "field in ?1 and (property = ?2 or property = ?3)",
                new Object[] { list, "jpg", "gif" });
        assertBsonEquals(Filters.and(
                Filters.in("field", list),
                Filters.or(Filters.eq("value", "jpg"), Filters.eq("value", "gif"))), result);

        result = operations.bindFilter(DemoObj.class,
                "field in ?1 and isOk = ?2 and (property = ?3 or property = ?4)",
                new Object[] { list, true, "jpg", "gif" });
        assertBsonEquals(Filters.and(
                Filters.and(Filters.in("field", list), Filters.eq("isOk", true)),
                Filters.or(Filters.eq("value", "jpg"), Filters.eq("value", "gif"))), result);
    }

    @Test
    public void testBindEnhancedFilterByIndexWithCustomType() {
        CustomType custom = new CustomType("test");
        Bson result = operations.bindFilter(Object.class, "field = ?1", new Object[] { custom });
        assertBsonEquals(Filters.eq("field", custom), result);
    }

    @Test
    public void testBindEnhancedFilterByIndexWithEnum() {
        Bson result = operations.bindFilter(Object.class, "field = ?1", new Object[] { TestEnum.VALUE_A });
        assertBsonEquals(Filters.eq("field", "VALUE_A"), result);
    }

    @Test
    public void testBindEnhancedFilterByName() {
        Bson result = operations.bindFilter(Object.class, "field = :field",
                Parameters.with("field", "a value").map());
        assertBsonEquals(Filters.eq("field", "a value"), result);

        result = operations.bindFilter(Object.class, "field.sub = :field",
                Parameters.with("field", "a value").map());
        assertBsonEquals(Filters.eq("field.sub", "a value"), result);

        // test field replacement
        result = operations.bindFilter(DemoObj.class, "property = :field",
                Parameters.with("field", "a value").map());
        assertBsonEquals(Filters.eq("value", "a value"), result);

        result = operations.bindFilter(Object.class, "field = :field",
                Parameters.with("field", LocalDate.of(2019, 3, 4)).map());
        assertBsonEquals(Filters.eq("field", LocalDate.of(2019, 3, 4)), result);

        result = operations.bindFilter(Object.class, "field = :field",
                Parameters.with("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1)).map());
        assertBsonEquals(Filters.eq("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1)), result);

        result = operations.bindFilter(Object.class, "field = :field",
                Parameters.with("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC)).map());
        assertBsonEquals(Filters.eq("field", LocalDateTime.of(2019, 3, 4, 1, 1, 1).toInstant(ZoneOffset.UTC)), result);

        result = operations.bindFilter(Object.class, "field = :field",
                Parameters.with("field", toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1))).map());
        assertBsonEquals(Filters.eq("field", toDate(LocalDateTime.of(2019, 3, 4, 1, 1, 1))), result);

        result = operations.bindFilter(Object.class, "field = :field",
                Parameters.with("field", UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000")).map());
        assertBsonEquals(Filters.eq("field", UUID.fromString("7f000101-7370-1f68-8173-70afa71b0000")), result);

        result = operations.bindFilter(Object.class, "field = :field and isOk = :isOk",
                Parameters.with("field", "a value").and("isOk", true).map());
        assertBsonEquals(Filters.and(Filters.eq("field", "a value"), Filters.eq("isOk", true)), result);

        result = operations.bindFilter(Object.class, "field = :field or isOk = :isOk",
                Parameters.with("field", "a value").and("isOk", true).map());
        assertBsonEquals(Filters.or(Filters.eq("field", "a value"), Filters.eq("isOk", true)), result);

        result = operations.bindFilter(Object.class, "count > :lower and count <= :upper",
                Parameters.with("lower", 5).and("upper", 10).map());
        assertBsonEquals(Filters.and(Filters.gt("count", 5), Filters.lte("count", 10)), result);

        result = operations.bindFilter(Object.class, "field != :field",
                Parameters.with("field", "a value").map());
        assertBsonEquals(Filters.ne("field", "a value"), result);

        result = operations.bindFilter(Object.class, "field like :field",
                Parameters.with("field", "a value").map());
        assertBsonEquals(Filters.regex("field", "a value"), result);

        // queries related to '$in' operator
        List<Object> list = Arrays.asList("f1", "f2");
        result = operations.bindFilter(DemoObj.class, "field in :fields",
                Parameters.with("fields", list).map());
        assertBsonEquals(Filters.in("field", list), result);

        result = operations.bindFilter(DemoObj.class, "field in :fields and isOk = :isOk",
                Parameters.with("fields", list).and("isOk", true).map());
        assertBsonEquals(Filters.and(Filters.in("field", list), Filters.eq("isOk", true)), result);

        result = operations.bindFilter(DemoObj.class,
                "field in :fields and (property = :p1 or property = :p2)",
                Parameters.with("fields", list).and("p1", "jpg").and("p2", "gif").map());
        assertBsonEquals(Filters.and(
                Filters.in("field", list),
                Filters.or(Filters.eq("value", "jpg"), Filters.eq("value", "gif"))), result);

        result = operations.bindFilter(DemoObj.class,
                "field in :fields and isOk = :isOk and (property = :p1 or property = :p2)",
                Parameters.with("fields", list)
                        .and("isOk", true)
                        .and("p1", "jpg")
                        .and("p2", "gif").map());
        assertBsonEquals(Filters.and(
                Filters.and(Filters.in("field", list), Filters.eq("isOk", true)),
                Filters.or(Filters.eq("value", "jpg"), Filters.eq("value", "gif"))), result);
    }

    @Test
    public void testBindEnhancedFilterByNameWithCustomType() {
        CustomType custom = new CustomType("test");
        Bson result = operations.bindFilter(Object.class, "field = :field",
                Parameters.with("field", custom).map());
        assertBsonEquals(Filters.eq("field", custom), result);
    }

    @Test
    public void testBindUpdate() {
        // native update by index without $set
        Bson update = operations.bindUpdate(DemoObj.class, "{'field': ?1}", new Object[] { "a value" });
        assertInstanceOf(org.bson.Document.class, update);
        org.bson.Document updateDoc = (org.bson.Document) update;
        org.bson.Document setDoc = (org.bson.Document) updateDoc.get("$set");
        assertEquals("a value", setDoc.get("field"));

        // native update by index without $set (list value)
        update = operations.bindUpdate(DemoObj.class, "{'listField': ?1}", new Object[] { List.of("value1", "value2") });
        assertInstanceOf(org.bson.Document.class, update);
        updateDoc = (org.bson.Document) update;
        setDoc = (org.bson.Document) updateDoc.get("$set");
        assertEquals(List.of("value1", "value2"), setDoc.get("listField"));

        // native update by name without $set
        update = operations.bindUpdate(Object.class, "{'field': :field}",
                Parameters.with("field", "a value").map());
        assertInstanceOf(org.bson.Document.class, update);
        updateDoc = (org.bson.Document) update;
        setDoc = (org.bson.Document) updateDoc.get("$set");
        assertEquals("a value", setDoc.get("field"));

        // native update by index with $set
        update = operations.bindUpdate(DemoObj.class, "{'$set':{'field': ?1}}", new Object[] { "a value" });
        assertInstanceOf(org.bson.Document.class, update);
        updateDoc = (org.bson.Document) update;
        setDoc = (org.bson.Document) updateDoc.get("$set");
        assertEquals("a value", setDoc.get("field"));

        // native update by name with $set
        update = operations.bindUpdate(Object.class, "{'$set':{'field': :field}}",
                Parameters.with("field", "a value").map());
        assertInstanceOf(org.bson.Document.class, update);
        updateDoc = (org.bson.Document) update;
        setDoc = (org.bson.Document) updateDoc.get("$set");
        assertEquals("a value", setDoc.get("field"));

        // native update by index with $inc
        update = operations.bindUpdate(DemoObj.class, "{'$inc':{'field': ?1}}", new Object[] { "a value" });
        assertInstanceOf(org.bson.Document.class, update);
        updateDoc = (org.bson.Document) update;
        org.bson.Document incDoc = (org.bson.Document) updateDoc.get("$inc");
        assertEquals("a value", incDoc.get("field"));

        // native update by name with $inc
        update = operations.bindUpdate(Object.class, "{'$inc':{'field': :field}}",
                Parameters.with("field", "a value").map());
        assertInstanceOf(org.bson.Document.class, update);
        updateDoc = (org.bson.Document) update;
        incDoc = (org.bson.Document) updateDoc.get("$inc");
        assertEquals("a value", incDoc.get("field"));

        // shorthand update
        update = operations.bindUpdate(Object.class, "field", new Object[] { "a value" });
        assertInstanceOf(org.bson.Document.class, update);
        updateDoc = (org.bson.Document) update;
        org.bson.Document innerDoc = (org.bson.Document) updateDoc.get("$set");
        assertEquals("a value", innerDoc.get("field"));

        // enhanced update by index
        update = operations.bindUpdate(Object.class, "field = ?1", new Object[] { "a value" });
        assertBsonEquals(
                new org.bson.Document("$set", Filters.eq("field", "a value")),
                update);

        // enhanced update by name
        update = operations.bindUpdate(Object.class, "field = :field",
                Parameters.with("field", "a value").map());
        assertBsonEquals(
                new org.bson.Document("$set", Filters.eq("field", "a value")),
                update);
    }

    @Test
    public void testBindUpdateWithCustomType() {
        CustomType custom = new CustomType("test");
        Bson update = operations.bindUpdate(Object.class, "field", new Object[] { custom });
        assertBsonEquals(
                new org.bson.Document("$set", Filters.eq("field", custom)),
                update);
    }

    @Test
    public void testBindInWithCustomTypes() {
        // PanacheQL IN with custom types
        List<CustomType> customList = Arrays.asList(new CustomType("a"), new CustomType("b"));
        Bson result = operations.bindFilter(Object.class, "field in ?1", new Object[] { customList });
        assertBsonEquals(Filters.in("field", customList), result);

        // PanacheQL IN with custom types by name
        result = operations.bindFilter(Object.class, "field in :values",
                Parameters.with("values", customList).map());
        assertBsonEquals(Filters.in("field", customList), result);

        // native query IN with custom types
        result = operations.bindFilter(Object.class, "{ field: { '$in': ?1 } }", new Object[] { customList });
        assertInstanceOf(org.bson.Document.class, result);
        org.bson.Document doc = (org.bson.Document) result;
        org.bson.Document inDoc = (org.bson.Document) doc.get("field");
        assertEquals(customList, inDoc.get("$in"));

        // PanacheQL IN with a single value (not a collection)
        result = operations.bindFilter(Object.class, "field in ?1", new Object[] { "single" });
        assertBsonEquals(Filters.in("field", "single"), result);
    }

    @Test
    public void testBindStringEscaping() {
        // test string with single quotes
        Bson result = operations.bindFilter(Object.class, "field", new Object[] { "it's a value" });
        assertBsonEquals(Filters.eq("field", "it's a value"), result);

        // test string with backslashes
        result = operations.bindFilter(Object.class, "field", new Object[] { "path\\to\\file" });
        assertBsonEquals(Filters.eq("field", "path\\to\\file"), result);

        // test string with double quotes
        result = operations.bindFilter(Object.class, "field", new Object[] { "say \"hello\"" });
        assertBsonEquals(Filters.eq("field", "say \"hello\""), result);

        // enhanced query with special characters
        result = operations.bindFilter(Object.class, "field = ?1", new Object[] { "it's a value" });
        assertBsonEquals(Filters.eq("field", "it's a value"), result);

        // native query with special characters
        result = operations.bindFilter(Object.class, "{'field': ?1}", new Object[] { "it's a value" });
        assertBsonEquals(Filters.eq("field", "it's a value"), result);
    }
}
