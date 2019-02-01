package io.vertx.codegen.testmodel;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DataObjectTCKImpl implements DataObjectTCK {

  @Override
  public DataObjectWithValues getDataObjectWithValues() {
    DataObjectWithValues dataObject = new DataObjectWithValues();
    dataObject.setBooleanValue(true);
    dataObject.setShortValue((short) 520);
    dataObject.setIntValue(123456);
    dataObject.setLongValue(123456789L);
    dataObject.setFloatValue(1.1f);
    dataObject.setDoubleValue(1.11d);
    dataObject.setBoxedShortValue((short) 1040);
    dataObject.setBoxedBooleanValue(true);
    dataObject.setBoxedIntValue(654321);
    dataObject.setBoxedLongValue(987654321L);
    dataObject.setBoxedFloatValue(2.2f);
    dataObject.setBoxedDoubleValue(2.22d);
    dataObject.setStringValue("wibble");
    dataObject.setInstantValue(Instant.parse("1984-05-27T00:05:00Z"));
    dataObject.setJsonObjectValue(new JsonObject().put("foo", "eek").put("bar", "wibble"));
    dataObject.setJsonArrayValue(new JsonArray().add("eek").add("wibble"));
    dataObject.setEnumValue(TestEnum.TIM);
    dataObject.setGenEnumValue(TestGenEnum.MIKE);
    dataObject.setDataObjectValue(new TestDataObject().setFoo("1").setBar(1).setWibble(1.1));
    return dataObject;
  }

  @Override
  public void setDataObjectWithValues(DataObjectWithValues dataObject) {
    assertEquals(true, dataObject.booleanValue);
    assertEquals(520, dataObject.shortValue);
    assertEquals(123456, dataObject.intValue);
    assertEquals(123456789L, dataObject.longValue);
    assertEquals(1.1f, dataObject.floatValue, 0.01f);
    assertEquals(1.11f, dataObject.doubleValue, 0.01f);
    assertEquals(true, dataObject.boxedBooleanValue);
    assertEquals(1040, (int) dataObject.boxedShortValue);
    assertEquals(654321, (int) dataObject.boxedIntValue);
    assertEquals(987654321L, (long) dataObject.boxedLongValue);
    assertEquals(2.2f, dataObject.boxedFloatValue, 0.01f);
    assertEquals(2.22f, dataObject.boxedDoubleValue, 0.01f);
    assertEquals("wibble", dataObject.stringValue);
    assertEquals(Instant.parse("1984-05-27T00:05:00Z"), dataObject.instantValue);
    assertEquals(new JsonObject().put("foo", "eek").put("bar", "wibble"), dataObject.jsonObjectValue);
    assertEquals(new JsonArray().add("eek").add("wibble"), dataObject.jsonArrayValue);
    assertEquals(TestEnum.TIM, dataObject.enumValue);
    assertEquals(TestGenEnum.MIKE, dataObject.genEnumValue);
    assertEquals("1", dataObject.dataObjectValue.getFoo());
    assertEquals(1, dataObject.dataObjectValue.getBar());
    assertEquals(1.1f, dataObject.dataObjectValue.getWibble(), 0.01f);
  }

  @Override
  public DataObjectWithLists getDataObjectWithLists() {
    DataObjectWithLists dataObject = new DataObjectWithLists();
    dataObject.setBooleanValues(Arrays.asList(true, false, true));
    dataObject.setShortValues(Arrays.asList((short) 0, (short) 520, (short) 1040));
    dataObject.setIntegerValues(Arrays.asList(0, 123456, 654321));
    dataObject.setLongValues(Arrays.asList(0L, 123456789L, 987654321L));
    dataObject.setFloatValues(Arrays.asList(1.1f, 2.2f, 3.3f));
    dataObject.setDoubleValues(Arrays.asList(1.11d, 2.22d, 3.33d));
    dataObject.setStringValues(Arrays.asList("stringValues1", "stringValues2", "stringValues3"));
    dataObject.setInstantValues(Arrays.asList(Instant.parse("1984-05-27T00:05:00Z"), Instant.parse("2018-07-05T08:23:21Z")));
    dataObject.setJsonObjectValues(Arrays.asList(new JsonObject().put("foo", "eek"), new JsonObject().put("foo", "wibble")));
    dataObject.setJsonArrayValues(Arrays.asList(new JsonArray().add("foo"), new JsonArray().add("bar")));
    dataObject.setDataObjectValues(Arrays.asList(new TestDataObject().setFoo("1").setBar(1).setWibble(1.1), new TestDataObject().setFoo("2").setBar(2).setWibble(2.2)));
    dataObject.setEnumValues(Arrays.asList(TestEnum.TIM, TestEnum.JULIEN));
    dataObject.setGenEnumValues(Arrays.asList(TestGenEnum.BOB, TestGenEnum.LAURA));
    return dataObject;
  }

  @Override
  public void setDataObjectWithLists(DataObjectWithLists dataObject) {
    assertEquals(Arrays.asList(true, false, true), dataObject.booleanValues);
    assertEquals(Arrays.asList((short)0, (short)520, (short)1040), dataObject.shortValues);
    assertEquals(Arrays.asList(0, 123456, 654321), dataObject.integerValues);
    assertEquals(Arrays.asList(0L, 123456789L, 987654321L), dataObject.longValues);
    assertEquals(Arrays.asList(1.1f, 2.2f, 3.3f), dataObject.floatValues);
    assertEquals(Arrays.asList(1.11d, 2.22d, 3.33d), dataObject.doubleValues);
    assertEquals(Arrays.asList("stringValues1", "stringValues2", "stringValues3"), dataObject.stringValues);
    assertEquals(Arrays.asList(Instant.parse("1984-05-27T00:05:00Z"), Instant.parse("2018-07-05T08:23:21Z")), dataObject.instantValues);
    assertEquals(Arrays.asList(new JsonObject().put("foo", "eek"), new JsonObject().put("foo", "wibble")), dataObject.jsonObjectValues);
    assertEquals(Arrays.asList(new JsonArray().add("foo"), new JsonArray().add("bar")), dataObject.jsonArrayValues);
    assertEquals(2, dataObject.dataObjectValues.size());
    assertEquals("1", dataObject.dataObjectValues.get(0).getFoo());
    assertEquals(1, dataObject.dataObjectValues.get(0).getBar());
    assertEquals(1.1, dataObject.dataObjectValues.get(0).getWibble(), 0.01);
    assertEquals("2", dataObject.dataObjectValues.get(1).getFoo());
    assertEquals(2, dataObject.dataObjectValues.get(1).getBar());
    assertEquals(2.2, dataObject.dataObjectValues.get(1).getWibble(), 0.01);
    assertEquals(Arrays.asList(TestEnum.TIM, TestEnum.JULIEN), dataObject.enumValues);
    assertEquals(Arrays.asList(TestGenEnum.BOB, TestGenEnum.LAURA), dataObject.genEnumValues);
  }

  private <T> Map<String, T> map(T first, T second) {
    LinkedHashMap<String, T> map = new LinkedHashMap<>();
    map.put("1", first);
    map.put("2", second);
    return map;
  }

  @Override
  public DataObjectWithMaps getDataObjectWithMaps() {
    DataObjectWithMaps dataObject = new DataObjectWithMaps();
    dataObject.setBooleanValues(map(true, false));
    dataObject.setShortValues(map((short) 520, (short) 1040));
    dataObject.setIntegerValues(map(123456, 654321));
    dataObject.setLongValues(map(123456789L, 987654321L));
    dataObject.setFloatValues(map(1.1f, 2.2f));
    dataObject.setDoubleValues(map(1.11d, 2.22d));
    dataObject.setStringValues(map("stringValues1", "stringValues2"));
    dataObject.setInstantValues(map(Instant.parse("1984-05-27T00:05:00Z"), Instant.parse("2018-07-05T08:23:21Z")));
    dataObject.setJsonObjectValues(map(new JsonObject().put("foo", "eek"), new JsonObject().put("foo", "wibble")));
    dataObject.setJsonArrayValues(map(new JsonArray().add("foo"), new JsonArray().add("bar")));
    dataObject.setDataObjectValues(map(new TestDataObject().setFoo("1").setBar(1).setWibble(1.1), new TestDataObject().setFoo("2").setBar(2).setWibble(2.2)));
    dataObject.setEnumValues(map(TestEnum.TIM, TestEnum.JULIEN));
    dataObject.setGenEnumValues(map(TestGenEnum.BOB, TestGenEnum.LAURA));
    return dataObject;
  }

  private <T> Map<String, T> assertMap(Map<String, T> map, T first, T second) {
    return assertMap(map, first, second, Assert::assertEquals);
  }

  private <T> Map<String, T> assertMap(Map<String, T> map, T first, T second, BiConsumer<T, T> assertEquals) {
    assertEquals.accept(first, map.get("1"));
    assertEquals.accept(second, map.get("2"));
    return map;
  }

  @Override
  public void setDataObjectWithMaps(DataObjectWithMaps dataObject) {
    assertMap(dataObject.booleanValues, true, false);
    assertMap(dataObject.shortValues, (short) 520, (short) 1040);
    assertMap(dataObject.integerValues, 123456, 654321);
    assertMap(dataObject.longValues, 123456789L, 987654321L);
    assertMap(dataObject.floatValues, 1.1f, 2.2f, (f1, f2) -> assertEquals(f1, f2, 0.1d));
    assertMap(dataObject.doubleValues, 1.11d, 2.22d, (f1, f2) -> assertEquals(f1, f2, 0.1d));
    assertMap(dataObject.stringValues, "stringValues1", "stringValues2");
    assertMap(dataObject.instantValues, Instant.parse("1984-05-27T00:05:00Z"), Instant.parse("2018-07-05T08:23:21Z"));
    assertMap(dataObject.jsonObjectValues, new JsonObject().put("foo", "eek"), new JsonObject().put("foo", "wibble"));
    assertMap(dataObject.jsonArrayValues, new JsonArray().add("foo"), new JsonArray().add("bar"));
    assertEquals(2, dataObject.dataObjectValues.size());
    assertEquals("1", dataObject.dataObjectValues.get("1").getFoo());
    assertEquals(1, dataObject.dataObjectValues.get("1").getBar());
    assertEquals(1.1, dataObject.dataObjectValues.get("1").getWibble(), 0.01);
    assertEquals("2", dataObject.dataObjectValues.get("2").getFoo());
    assertEquals(2, dataObject.dataObjectValues.get("2").getBar());
    assertEquals(2.2, dataObject.dataObjectValues.get("2").getWibble(), 0.01);
    assertMap(dataObject.enumValues, TestEnum.TIM, TestEnum.JULIEN);
    assertMap(dataObject.genEnumValues, TestGenEnum.BOB, TestGenEnum.LAURA);
  }

  @Override
  public void methodWithOnlyJsonObjectConstructorDataObject(
      DataObjectWithOnlyJsonObjectConstructor dataObject) {
    assertEquals(dataObject.toJson(), new JsonObject().put("foo", "bar"));
  }

  @Override
  public void setDataObjectWithBuffer(DataObjectWithNestedBuffer dataObject) {
    assertEquals("Hello World", new String(dataObject.getBuffer().getBytes()));
    assertEquals("Bye World", new String(dataObject.getNested().getBuffer().getBytes()));
    assertEquals("one", new String(dataObject.getBuffers().get(0).getBytes()));
    assertEquals("two", new String(dataObject.getBuffers().get(1).getBytes()));
  }

  @Override
  public void setDataObjectWithListAdders(DataObjectWithListAdders dataObject) {
    setDataObjectWithLists(dataObject.value);
  }

  @Override
  public void setDataObjectWithMapAdders(DataObjectWithMapAdders dataObject) {
    setDataObjectWithMaps(dataObject.value);
  }

  @Override
  public void setDataObjectWithRecursion(DataObjectWithRecursion dataObject) {
    assertEquals("first", dataObject.getData());
    assertEquals("second", dataObject.getNext().getData());
    assertEquals("third", dataObject.getNext().getNext().getData());
    assertEquals(null, dataObject.getNext().getNext().getNext());
  }
}
