package io.quarkus.funqy.test;

import java.time.Month;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.funqy.runtime.query.QueryObjectMapper;
import io.quarkus.funqy.runtime.query.QueryReader;

public class QueryReaderTest {

    @Test
    public void testSimple() throws Exception {
        QueryObjectMapper mapper = new QueryObjectMapper();
        QueryReader<Simple> reader = mapper.readerFor(Simple.class);
        Map<String, String> params = new HashMap<>();
        params.put("time", "2020-07-29T10:15:30+01:00");
        params.put("intVal", "42");
        params.put("shortVal", "4");
        params.put("longVal", "442");
        params.put("doubleVal", "4.2");
        params.put("floatVal", "4.2");
        params.put("b", "1");
        params.put("boolVal", "true");
        params.put("value", "hello");

        Simple simple = reader.readValue(params.entrySet().iterator());

        Assertions.assertEquals(42, simple.getIntVal());
        Assertions.assertEquals(4, simple.getShortVal());
        Assertions.assertEquals(442, simple.getLongVal());
        Assertions.assertTrue(simple.getDoubleVal() <= 4.2 && simple.getDoubleVal() > 4.1);
        Assertions.assertTrue(simple.getFloatVal() <= 4.2 && simple.getFloatVal() > 4.1);
        Assertions.assertEquals(1, simple.getB());
        Assertions.assertEquals(true, simple.isBoolVal());
        Assertions.assertEquals("hello", simple.getValue());
        Assertions.assertEquals(Month.JULY, simple.getTime().getMonth());
        Assertions.assertEquals(29, simple.getTime().getDayOfMonth());
    }

    @Test
    public void testNested() throws Exception {
        QueryObjectMapper mapper = new QueryObjectMapper();
        QueryReader<Nested> reader = mapper.readerFor(Nested.class);
        Map<String, String> params = new HashMap<>();
        params.put("nestedOne.intVal", "42");
        params.put("nestedOne.shortVal", "4");
        params.put("nestedOne.longVal", "442");
        params.put("nestedOne.doubleVal", "4.2");
        params.put("nestedOne.floatVal", "4.2");
        params.put("nestedOne.b", "1");
        params.put("nestedOne.boolVal", "true");
        params.put("nestedOne.value", "hello");

        params.put("nestedTwo.intVal", "32");
        params.put("nestedTwo.shortVal", "3");
        params.put("nestedTwo.longVal", "332");
        params.put("nestedTwo.doubleVal", "3.2");
        params.put("nestedTwo.floatVal", "3.2");
        params.put("nestedTwo.b", "2");
        params.put("nestedTwo.boolVal", "true");
        params.put("nestedTwo.value", "world");

        Nested nested = reader.readValue(params.entrySet().iterator());

        Assertions.assertEquals(42, nested.getNestedOne().getIntVal());
        Assertions.assertEquals(4, nested.getNestedOne().getShortVal());
        Assertions.assertEquals(442, nested.getNestedOne().getLongVal());
        Assertions.assertTrue(nested.getNestedOne().getDoubleVal() <= 4.3 && nested.getNestedOne().getDoubleVal() > 4.1);
        Assertions.assertTrue(nested.getNestedOne().getFloatVal() <= 4.3 && nested.getNestedOne().getFloatVal() > 4.1);
        Assertions.assertEquals(1, nested.getNestedOne().getB());
        Assertions.assertEquals(true, nested.getNestedOne().isBoolVal());
        Assertions.assertEquals("hello", nested.getNestedOne().getValue());

        Assertions.assertEquals(32, nested.getNestedTwo().getIntVal());
        Assertions.assertEquals(3, nested.getNestedTwo().getShortVal());
        Assertions.assertEquals(332, nested.getNestedTwo().getLongVal());
        Assertions.assertTrue(nested.getNestedTwo().getDoubleVal() <= 3.3 && nested.getNestedTwo().getDoubleVal() > 3.1);
        Assertions.assertTrue(nested.getNestedTwo().getFloatVal() <= 3.3 && nested.getNestedTwo().getFloatVal() > 3.1);
        Assertions.assertEquals(2, nested.getNestedTwo().getB());
        Assertions.assertEquals(true, nested.getNestedTwo().isBoolVal());
        Assertions.assertEquals("world", nested.getNestedTwo().getValue());
    }

    @Test
    public void testDirectMap() {
        QueryObjectMapper mapper = new QueryObjectMapper();
        QueryReader reader = mapper.readerFor(Map.class);
        Map<String, String> params = new HashMap<>();

        params.put("val", "42");

        Map<String, String> map = (Map<String, String>) reader.readValue(params.entrySet().iterator());

        Assertions.assertEquals("42", map.get("val"));

    }

    @Test
    public void testNestedCollections() {
        QueryObjectMapper mapper = new QueryObjectMapper();
        QueryReader<NestedCollection> reader = mapper.readerFor(NestedCollection.class);
        List<Map.Entry<String, String>> params = new LinkedList<>();

        params.add(entry("intMap.one", "1"));
        params.add(entry("intMap.two", "2"));
        params.add(entry("intKeyMap.1", "one"));
        params.add(entry("intKeyMap.2", "two"));
        params.add(entry("stringList", "one"));
        params.add(entry("stringList", "two"));
        params.add(entry("intSet", "1"));
        params.add(entry("intSet", "2"));
        params.add(entry("simpleMap.one.value", "one"));
        params.add(entry("simpleMap.two.value", "two"));
        params.add(entry("simpleList.one.value", "one"));
        params.add(entry("simpleList.one.intVal", "1"));
        params.add(entry("simpleList.two.value", "two"));
        params.add(entry("simpleList.two.intVal", "2"));
        params.add(entry("simpleSet.one.value", "one"));
        params.add(entry("simpleSet.one.intVal", "1"));
        params.add(entry("simpleSet.two.value", "two"));
        params.add(entry("simpleSet.two.intVal", "2"));
        params.add(entry("time", "2020-07-29T10:15:30+01:00"));

        NestedCollection nested = reader.readValue(params.iterator());

        Assertions.assertEquals(1, nested.getIntMap().get("one"));
        Assertions.assertEquals(2, nested.getIntMap().get("two"));
        Assertions.assertEquals("one", nested.getIntKeyMap().get(1));
        Assertions.assertEquals("two", nested.getIntKeyMap().get(2));
        Assertions.assertEquals("one", nested.getSimpleMap().get("one").getValue());
        Assertions.assertEquals("two", nested.getSimpleMap().get("two").getValue());
        Assertions.assertTrue(nested.getStringList().contains("one"));
        Assertions.assertTrue(nested.getStringList().contains("two"));
        Assertions.assertTrue(nested.getIntSet().contains(1));
        Assertions.assertTrue(nested.getIntSet().contains(2));

        Assertions.assertNotNull(nested.getSimpleList());
        Assertions.assertEquals(2, nested.getSimpleList().size());
        Assertions.assertEquals(1, nested.getSimpleList().get(0).getIntVal());
        Assertions.assertEquals("one", nested.getSimpleList().get(0).getValue());
        Assertions.assertEquals(2, nested.getSimpleList().get(1).getIntVal());
        Assertions.assertEquals("two", nested.getSimpleList().get(1).getValue());

        Assertions.assertNotNull(nested.getSimpleSet());
        Assertions.assertEquals(2, nested.getSimpleSet().size());
        boolean hasOne = false;
        boolean hasTwo = false;
        for (Simple simple : nested.getSimpleSet()) {
            if (simple.getIntVal() == 1) {
                hasOne = true;
                Assertions.assertEquals("one", simple.getValue());
            }
            if (simple.getIntVal() == 2) {
                hasTwo = true;
                Assertions.assertEquals("two", simple.getValue());
            }

        }
        Assertions.assertTrue(hasOne);
        Assertions.assertTrue(hasTwo);

    }

    @Test
    public void testRecursiveType() {
        QueryObjectMapper mapper = new QueryObjectMapper();
        QueryReader<RecursiveType> reader = mapper.readerFor(RecursiveType.class);
    }

    static Map.Entry<String, String> entry(String key, String value) {
        return new Map.Entry() {
            @Override
            public Object getKey() {
                return key;
            }

            @Override
            public Object getValue() {
                return value;
            }

            @Override
            public Object setValue(Object o) {
                throw new RuntimeException();
            }
        };
    }

}
