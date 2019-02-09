package io.vertx.codegen.testmodel;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CollectionTCKImpl implements CollectionTCK {

  @Override
  public void methodWithListParams(List<String> listString, List<Byte> listByte, List<Short> listShort, List<Integer> listInt, List<Long> listLong, List<JsonObject> listJsonObject, List<JsonArray> listJsonArray, List<RefedInterface1> listVertxGen, List<TestDataObject> listDataObject, List<TestEnum> listEnum, List<Object> listObject) {
    assertEquals("foo", listString.get(0));
    assertEquals("bar", listString.get(1));
    assertEquals((byte) 2, listByte.get(0).byteValue());
    assertEquals((byte) 3, listByte.get(1).byteValue());
    assertEquals((short) 12, listShort.get(0).shortValue());
    assertEquals((short) 13, listShort.get(1).shortValue());
    assertEquals((int) 1234, listInt.get(0).intValue());
    assertEquals((int) 1345, listInt.get(1).intValue());
    System.out.println("entry type is " + ((List) listLong).get(0).getClass().getName());
    assertEquals(123l, listLong.get(0).longValue());
    assertEquals(456l, listLong.get(1).longValue());
    assertEquals(new JsonObject().put("foo", "bar"), listJsonObject.get(0));
    assertEquals(new JsonObject().put("eek", "wibble"), listJsonObject.get(1));
    assertEquals(new JsonArray().add("foo"), listJsonArray.get(0));
    assertEquals(new JsonArray().add("blah"), listJsonArray.get(1));
    assertEquals("foo", listVertxGen.get(0).getString());
    assertEquals("bar", listVertxGen.get(1).getString());
    assertEquals(new JsonObject().put("foo", "String 1").put("bar", 1).put("wibble", 1.1), listDataObject.get(0).toJson());
    assertEquals(new JsonObject().put("foo", "String 2").put("bar", 2).put("wibble", 2.2), listDataObject.get(1).toJson());
    assertEquals(Arrays.asList(TestEnum.JULIEN, TestEnum.TIM), new ArrayList<>(listEnum));
    assertEquals(6, listObject.size());
    assertEquals("foo", listObject.get(0));
    assertEquals(4, ((Number)listObject.get(1)).intValue());
    assertEquals(3.4f, ((Number)listObject.get(2)).floatValue(), 0.1);
    assertEquals(true, listObject.get(3));
    assertEquals(new JsonObject().put("wibble", "eek"), listObject.get(4));
    assertEquals(new JsonArray().add("one").add(2), listObject.get(5));
  }

  @Override
  public void methodWithSetParams(Set<String> setString, Set<Byte> setByte, Set<Short> setShort, Set<Integer> setInt, Set<Long> setLong, Set<JsonObject> setJsonObject, Set<JsonArray> setJsonArray, Set<RefedInterface1> setVertxGen, Set<TestDataObject> setDataObject, Set<TestEnum> setEnum, Set<Object> setObject) {
    assertTrue(setString.contains("foo"));
    assertTrue(setString.contains("bar"));
    assertTrue(setByte.contains((byte) 2));
    assertTrue(setByte.contains((byte) 3));
    assertTrue(setShort.contains((short) 12));
    assertTrue(setShort.contains((short) 13));
    assertTrue(setInt.contains(1234));
    assertTrue(setInt.contains(1345));
    assertTrue(setLong.contains(123l));
    assertTrue(setLong.contains(456l));
    assertTrue(setJsonObject.contains(new JsonObject().put("foo", "bar")));
    assertTrue(setJsonObject.contains(new JsonObject().put("eek", "wibble")));
    assertTrue(setJsonArray.contains(new JsonArray().add("foo")));
    assertTrue(setJsonArray.contains(new JsonArray().add("blah")));
    assertTrue(setVertxGen.contains(new RefedInterface1Impl().setString("foo")));
    assertTrue(setVertxGen.contains(new RefedInterface1Impl().setString("bar")));
    assertEquals(2, setDataObject.size());
    Set<JsonObject> setDataObjectJson = setDataObject.stream().map(d -> d.toJson()).collect(Collectors.toSet());
    assertTrue(setDataObjectJson.contains(new JsonObject().put("foo", "String 1").put("bar", 1).put("wibble", 1.1)));
    assertTrue(setDataObjectJson.contains(new JsonObject().put("foo", "String 2").put("bar", 2).put("wibble", 2.2)));
    assertEquals(2, setEnum.size());
    assertTrue(setEnum.contains(TestEnum.JULIEN));
    assertTrue(setEnum.contains(TestEnum.TIM));
    assertEquals(6, setObject.size());
    assertTrue(setObject.contains("foo"));
    assertTrue(setObject.contains(true));
    assertTrue(setObject.contains(true));
    assertTrue(setObject.contains(new JsonObject().put("wibble", "eek")));
    assertTrue(setObject.contains(new JsonArray().add("one").add(2)));
  }

  @Override
  public void methodWithMapParams(Map<String, String> mapString, Map<String, Byte> mapByte, Map<String, Short> mapShort, Map<String, Integer> mapInt, Map<String, Long> mapLong, Map<String, JsonObject> mapJsonObject, Map<String, JsonArray> mapJsonArray, Map<String, RefedInterface1> mapVertxGen, Map<String, Object> mapObject) {
    assertEquals("bar", mapString.get("foo"));
    assertEquals("wibble", mapString.get("eek"));
    assertEquals((byte) 2, mapByte.get("foo").byteValue());
    assertEquals((byte) 3, mapByte.get("eek").byteValue());
    assertEquals((short) 12, mapShort.get("foo").shortValue());
    assertEquals((short) 13, mapShort.get("eek").shortValue());
    assertEquals(1234, mapInt.get("foo").intValue());
    assertEquals(1345, mapInt.get("eek").intValue());
    assertEquals(123L, mapLong.get("foo").longValue());
    assertEquals(456L, mapLong.get("eek").longValue());
    assertEquals(new JsonObject().put("foo", "bar"), mapJsonObject.get("foo"));
    assertEquals(new JsonObject().put("eek", "wibble"), mapJsonObject.get("eek"));
    assertEquals(new JsonArray().add("foo"), mapJsonArray.get("foo"));
    assertEquals(new JsonArray().add("blah"), mapJsonArray.get("eek"));
    assertEquals(new RefedInterface1Impl().setString("foo"), mapVertxGen.get("foo"));
    assertEquals(new RefedInterface1Impl().setString("bar"), mapVertxGen.get("eek"));
    assertEquals("foo", mapObject.get("string"));
    assertEquals(4, ((Number)mapObject.get("integer")).intValue());
    assertEquals(3.4f, ((Number)mapObject.get("float")).floatValue(), 0.1);
    assertEquals(true, mapObject.get("boolean"));
    assertEquals(new JsonObject().put("wibble", "eek"), mapObject.get("object"));
    assertEquals(new JsonArray().add("one").add(2), mapObject.get("array"));
  }


  @Override
  public void methodWithHandlerListAndSet(Handler<List<String>> listStringHandler, Handler<List<Integer>> listIntHandler,
                                          Handler<Set<String>> setStringHandler, Handler<Set<Integer>> setIntHandler) {
    List<String> listString = Arrays.asList("foo", "bar", "wibble");
    List<Integer> listInt = Arrays.asList(5, 12, 100);
    Set<String> setString = new LinkedHashSet<>( Arrays.asList("foo", "bar", "wibble"));
    Set<Integer> setInt = new LinkedHashSet<>(Arrays.asList(new Integer[] {5, 12, 100}));
    listStringHandler.handle(listString);
    listIntHandler.handle(listInt);
    setStringHandler.handle(setString);
    setIntHandler.handle(setInt);
  }

  @Override
  public void methodWithHandlerAsyncResultListString(Handler<AsyncResult<List<String>>> handler) {
    List<String> listString = Arrays.asList("foo", "bar", "wibble");
    handler.handle(Future.succeededFuture(listString));
  }

  @Override
  public void methodWithHandlerAsyncResultListInteger(Handler<AsyncResult<List<Integer>>> handler) {
    List<Integer> listInt = Arrays.asList(5, 12, 100);
    handler.handle(Future.succeededFuture(listInt));
  }

  @Override
  public void methodWithHandlerAsyncResultSetString(Handler<AsyncResult<Set<String>>> handler) {
    Set<String> setString = new LinkedHashSet<>( Arrays.asList("foo", "bar", "wibble"));
    handler.handle(Future.succeededFuture(setString));
  }

  @Override
  public void methodWithHandlerAsyncResultSetInteger(Handler<AsyncResult<Set<Integer>>> handler) {
    Set<Integer> setInt = new LinkedHashSet<>(Arrays.asList(5, 12, 100));
    handler.handle(Future.succeededFuture(setInt));
  }

  @Override
  public void methodWithHandlerListVertxGen(Handler<List<RefedInterface1>> listHandler) {
    List<RefedInterface1> list = Arrays.asList(new RefedInterface1Impl().setString("foo"), new RefedInterface1Impl().setString("bar"));
    listHandler.handle(list);
  }

  @Override
  public void methodWithHandlerSetVertxGen(Handler<Set<RefedInterface1>> setHandler) {
    Set<RefedInterface1> list = new LinkedHashSet<>(Arrays.asList(new RefedInterface1Impl().setString("foo"), new RefedInterface1Impl().setString("bar")));
    setHandler.handle(list);
  }

  @Override
  public void methodWithHandlerListAbstractVertxGen(Handler<List<RefedInterface2>> listHandler) {
    List<RefedInterface2> list = Arrays.asList(new RefedInterface2Impl().setString("abstractfoo"), new RefedInterface2Impl().setString("abstractbar"));
    listHandler.handle(list);
  }

  @Override
  public void methodWithHandlerSetAbstractVertxGen(Handler<Set<RefedInterface2>> setHandler) {
    Set<RefedInterface2> list = new LinkedHashSet<>(Arrays.asList(new RefedInterface2Impl().setString("abstractfoo"), new RefedInterface2Impl().setString("abstractbar")));
    setHandler.handle(list);
  }

  @Override
  public void methodWithHandlerListJsonObject(Handler<List<JsonObject>> listHandler) {
    List<JsonObject> list = Arrays.asList(new JsonObject().put("cheese", "stilton"), new JsonObject().put("socks", "tartan"));
    listHandler.handle(list);
  }

  @Override
  public void methodWithHandlerListComplexJsonObject(Handler<List<JsonObject>> listHandler) {
    List<JsonObject> list = Arrays.asList(new JsonObject().put("outer", new JsonObject().put("socks", "tartan")).put("list", new JsonArray().add("yellow").add("blue")));
    listHandler.handle(list);
  }

  @Override
  public void methodWithHandlerSetJsonObject(Handler<Set<JsonObject>> setHandler) {
    Set<JsonObject> set = new LinkedHashSet<>(Arrays.asList(new JsonObject().put("cheese", "stilton"), new JsonObject().put("socks", "tartan")));
    setHandler.handle(set);
  }

  @Override
  public void methodWithHandlerSetComplexJsonObject(Handler<Set<JsonObject>> setHandler) {
    Set<JsonObject> set = new LinkedHashSet<>(Arrays.asList(new JsonObject().put("outer", new JsonObject().put("socks", "tartan")).put("list", new JsonArray().add("yellow").add("blue"))));
    setHandler.handle(set);
  }

  @Override
  public void methodWithHandlerListJsonArray(Handler<List<JsonArray>> listHandler) {
    List<JsonArray> list = Arrays.asList(new JsonArray().add("green").add("blue"), new JsonArray().add("yellow").add("purple"));
    listHandler.handle(list);
  }

  @Override
  public void methodWithHandlerListComplexJsonArray(Handler<List<JsonArray>> listHandler) {
    List<JsonArray> list = Arrays.asList(new JsonArray().add(new JsonObject().put("foo", "hello")), new JsonArray().add(new JsonObject().put("bar", "bye")));
    listHandler.handle(list);
  }

  @Override
  public void methodWithHandlerSetJsonArray(Handler<Set<JsonArray>> listHandler) {
    Set<JsonArray> set = new LinkedHashSet<>(Arrays.asList(new JsonArray().add("green").add("blue"), new JsonArray().add("yellow").add("purple")));
    listHandler.handle(set);
  }

  @Override
  public void methodWithHandlerSetComplexJsonArray(Handler<Set<JsonArray>> setHandler) {
    List<JsonArray> list = Arrays.asList(new JsonArray().add(new JsonObject().put("foo", "hello")), new JsonArray().add(new JsonObject().put("bar", "bye")));
    Set<JsonArray> set = new LinkedHashSet<>(list);
    setHandler.handle(set);
  }

  @Override
  public void methodWithHandlerListDataObject(Handler<List<TestDataObject>> listHandler) {
    List<TestDataObject> list =
        Arrays.asList(new TestDataObject().setFoo("String 1").setBar(1).setWibble(1.1), new TestDataObject().setFoo("String 2").setBar(2).setWibble(2.2));
    listHandler.handle(list);
  }

  @Override
  public void methodWithHandlerSetDataObject(Handler<Set<TestDataObject>> setHandler) {
    Set<TestDataObject> set =
        new LinkedHashSet<>(Arrays.asList(new TestDataObject().setFoo("String 1").setBar(1).setWibble(1.1), new TestDataObject().setFoo("String 2").setBar(2).setWibble(2.2)));
    setHandler.handle(set);
  }

  @Override
  public void methodWithHandlerListEnum(Handler<List<TestEnum>> listHandler) {
    listHandler.handle(Arrays.asList(TestEnum.TIM, TestEnum.JULIEN));
  }

  @Override
  public void methodWithHandlerSetEnum(Handler<Set<TestEnum>> setHandler) {
    setHandler.handle(new LinkedHashSet<>(Arrays.asList(TestEnum.TIM, TestEnum.JULIEN)));
  }

  @Override
  public void methodWithHandlerAsyncResultListVertxGen(Handler<AsyncResult<List<RefedInterface1>>> listHandler) {
    List<RefedInterface1> list = Arrays.asList(new RefedInterface1Impl().setString("foo"), new RefedInterface1Impl().setString("bar"));
    listHandler.handle(Future.succeededFuture(list));
  }

  @Override
  public void methodWithHandlerAsyncResultSetVertxGen(Handler<AsyncResult<Set<RefedInterface1>>> setHandler) {
    Set<RefedInterface1> list = new LinkedHashSet<>(Arrays.asList(new RefedInterface1Impl().setString("foo"), new RefedInterface1Impl().setString("bar")));
    setHandler.handle(Future.succeededFuture(list));
  }

  @Override
  public void methodWithHandlerAsyncResultListAbstractVertxGen(Handler<AsyncResult<List<RefedInterface2>>> listHandler) {
    List<RefedInterface2> list = Arrays.asList(new RefedInterface2Impl().setString("abstractfoo"), new RefedInterface2Impl().setString("abstractbar"));
    listHandler.handle(Future.succeededFuture(list));
  }

  @Override
  public void methodWithHandlerAsyncResultSetAbstractVertxGen(Handler<AsyncResult<Set<RefedInterface2>>> setHandler) {
    Set<RefedInterface2> list = new LinkedHashSet<>(Arrays.asList(new RefedInterface2Impl().setString("abstractfoo"), new RefedInterface2Impl().setString("abstractbar")));
    setHandler.handle(Future.succeededFuture(list));
  }

  @Override
  public void methodWithHandlerAsyncResultListJsonObject(Handler<AsyncResult<List<JsonObject>>> listHandler) {
    List<JsonObject> list = Arrays.asList(new JsonObject().put("cheese", "stilton"), new JsonObject().put("socks", "tartan"));
    listHandler.handle(Future.succeededFuture(list));
  }

  @Override
  public void methodWithHandlerAsyncResultListComplexJsonObject(Handler<AsyncResult<List<JsonObject>>> listHandler) {
    List<JsonObject> list = Arrays.asList(new JsonObject().put("outer", new JsonObject().put("socks", "tartan")).put("list", new JsonArray().add("yellow").add("blue")));
    listHandler.handle(Future.succeededFuture(list));
  }

  @Override
  public void methodWithHandlerAsyncResultSetJsonObject(Handler<AsyncResult<Set<JsonObject>>> setHandler) {
    Set<JsonObject> set = new LinkedHashSet<>(Arrays.asList(new JsonObject().put("cheese", "stilton"), new JsonObject().put("socks", "tartan")));
    setHandler.handle(Future.succeededFuture(set));
  }

  @Override
  public void methodWithHandlerAsyncResultSetComplexJsonObject(Handler<AsyncResult<Set<JsonObject>>> setHandler) {
    Set<JsonObject> set = new LinkedHashSet<>(Arrays.asList(new JsonObject().put("outer", new JsonObject().put("socks", "tartan")).put("list", new JsonArray().add("yellow").add("blue"))));
    setHandler.handle(Future.succeededFuture(set));
  }

  @Override
  public void methodWithHandlerAsyncResultListJsonArray(Handler<AsyncResult<List<JsonArray>>> listHandler) {
    List<JsonArray> list = Arrays.asList(new JsonArray().add("green").add("blue"), new JsonArray().add("yellow").add("purple"));
    listHandler.handle(Future.succeededFuture(list));
  }

  @Override
  public void methodWithHandlerAsyncResultListComplexJsonArray(Handler<AsyncResult<List<JsonArray>>> listHandler) {
    List<JsonArray> list = Arrays.asList(new JsonArray().add(new JsonObject().put("foo", "hello")), new JsonArray().add(new JsonObject().put("bar", "bye")));
    listHandler.handle(Future.succeededFuture(list));
  }

  @Override
  public void methodWithHandlerAsyncResultSetJsonArray(Handler<AsyncResult<Set<JsonArray>>> listHandler) {
    Set<JsonArray> set = new LinkedHashSet<>(Arrays.asList(new JsonArray().add("green").add("blue"), new JsonArray().add("yellow").add("purple")));
    listHandler.handle(Future.succeededFuture(set));
  }

  @Override
  public void methodWithHandlerAsyncResultSetComplexJsonArray(Handler<AsyncResult<Set<JsonArray>>> listHandler) {
    Set<JsonArray> set = new LinkedHashSet<>(Arrays.asList(new JsonArray().add(new JsonObject().put("foo", "hello")), new JsonArray().add(new JsonObject().put("bar", "bye"))));
    listHandler.handle(Future.succeededFuture(set));
  }

  @Override
  public void methodWithHandlerAsyncResultListDataObject(Handler<AsyncResult<List<TestDataObject>>> listHandler) {
    List<TestDataObject> list =
        Arrays.asList(new TestDataObject().setFoo("String 1").setBar(1).setWibble(1.1), new TestDataObject().setFoo("String 2").setBar(2).setWibble(2.2));
    listHandler.handle(Future.succeededFuture(list));
  }

  @Override
  public void methodWithHandlerAsyncResultSetDataObject(Handler<AsyncResult<Set<TestDataObject>>> setHandler) {
    Set<TestDataObject> set =
        new LinkedHashSet<>(Arrays.asList(new TestDataObject().setFoo("String 1").setBar(1).setWibble(1.1), new TestDataObject().setFoo("String 2").setBar(2).setWibble(2.2)));
    setHandler.handle(Future.succeededFuture(set));
  }

  @Override
  public void methodWithHandlerAsyncResultListEnum(Handler<AsyncResult<List<TestEnum>>> listHandler) {
    listHandler.handle(Future.succeededFuture(Arrays.asList(TestEnum.TIM, TestEnum.JULIEN)));
  }

  @Override
  public void methodWithHandlerAsyncResultSetEnum(Handler<AsyncResult<Set<TestEnum>>> setHandler) {
    setHandler.handle(Future.succeededFuture(new LinkedHashSet<>(Arrays.asList(TestEnum.TIM, TestEnum.JULIEN))));
  }

  @Override
  public List<String> methodWithListStringReturn() {
    return Arrays.asList("foo", "bar", "wibble");
  }

  @Override
  public Set<String> methodWithSetStringReturn() {
    return new LinkedHashSet<>( Arrays.asList("foo", "bar", "wibble"));
  }

  @Override
  public Map<String, String> methodWithMapStringReturn(Handler<String> handler) {
    Map<String, String> map = new StringHandlerTestMap(handler);
    map.put("foo", "bar");
    return map;
  }

  @Override
  public Map<String, JsonObject> methodWithMapJsonObjectReturn(Handler<String> handler) {
    Map<String, JsonObject> map = new JsonObjectHandlerTestMap(handler);
    map.put("foo", new JsonObject().put("wibble", "eek"));
    return map;
  }

  @Override
  public Map<String, JsonObject> methodWithMapComplexJsonObjectReturn(Handler<String> handler) {
    Map<String, JsonObject> map = new JsonObjectHandlerTestMap(handler);
    map.put("foo", new JsonObject().put("outer", new JsonObject().put("socks", "tartan")).put("list", new JsonArray().add("yellow").add("blue")));
    return map;
  }

  @Override
  public Map<String, JsonArray> methodWithMapJsonArrayReturn(Handler<String> handler) {
    Map<String, JsonArray> map = new JsonArrayHandlerTestMap(handler);
    map.put("foo", new JsonArray().add("wibble"));
    return map;
  }

  @Override
  public Map<String, JsonArray> methodWithMapComplexJsonArrayReturn(Handler<String> handler) {
    Map<String, JsonArray> map = new JsonArrayHandlerTestMap(handler);
    map.put("foo", new JsonArray().add(new JsonObject().put("foo", "hello")).add(new JsonObject().put("bar", "bye")));
    return map;
  }

  @Override
  public Map<String, Object> methodWithMapObjectReturn(Handler<String> handler) {
    Map<String, Object> map = new ObjectHandlerTestMap(handler);
    map.put("string", "foo");
    map.put("integer", 4);
    map.put("float", 3.4f);
    map.put("object", new JsonObject().put("wibble", "eek"));
    map.put("array", new JsonArray().add("one").add(2));
    map.put("boolean", true);
    return map;
  }

  @Override
  public Map<String, Long> methodWithMapLongReturn(Handler<String> handler) {
    Map<String, Long> map = new LongHandlerTestMap(handler);
    map.put("foo", 123l);
    return map;
  }

  @Override
  public Map<String, Integer> methodWithMapIntegerReturn(Handler<String> handler) {
    Map<String, Integer> map = new IntegerHandlerTestMap(handler);
    map.put("foo", 123);
    return map;
  }

  @Override
  public Map<String, Short> methodWithMapShortReturn(Handler<String> handler) {
    Map<String, Short> map = new ShortHandlerTestMap(handler);
    map.put("foo", (short)123);
    return map;
  }

  @Override
  public Map<String, Byte> methodWithMapByteReturn(Handler<String> handler) {
    Map<String, Byte> map = new ByteHandlerTestMap(handler);
    map.put("foo", (byte)123);
    return map;
  }

  @Override
  public Map<String, Character> methodWithMapCharacterReturn(Handler<String> handler) {
    Map<String, Character> map = new CharacterHandlerTestMap(handler);
    map.put("foo", 'X');
    return map;
  }

  @Override
  public Map<String, Boolean> methodWithMapBooleanReturn(Handler<String> handler) {
    Map<String, Boolean> map = new BooleanHandlerTestMap(handler);
    map.put("foo", true);
    return map;
  }

  @Override
  public Map<String, Float> methodWithMapFloatReturn(Handler<String> handler) {
    Map<String, Float> map = new FloatHandlerTestMap(handler);
    map.put("foo", 0.123f);
    return map;
  }

  @Override
  public Map<String, Double> methodWithMapDoubleReturn(Handler<String> handler) {
    Map<String, Double> map = new DoubleHandlerTestMap(handler);
    map.put("foo", 0.123d);
    return map;
  }

  @Override
  public List<Long> methodWithListLongReturn() {
    return Arrays.asList(123l, 456l);
  }

  @Override
  public List<RefedInterface1> methodWithListVertxGenReturn() {
    return Arrays.asList(new RefedInterface1Impl().setString("foo"), new RefedInterface1Impl().setString("bar"));
  }

  @Override
  public List<JsonObject> methodWithListJsonObjectReturn() {
    return Arrays.asList(new JsonObject().put("foo", "bar"), new JsonObject().put("blah", "eek"));
  }

  @Override
  public List<JsonObject> methodWithListComplexJsonObjectReturn() {
    return Arrays.asList(new JsonObject().put("outer", new JsonObject().put("socks", "tartan")).put("list", new JsonArray().add("yellow").add("blue")));
  }

  @Override
  public List<JsonArray> methodWithListJsonArrayReturn() {
    return Arrays.asList(new JsonArray().add("foo"), new JsonArray().add("blah"));
  }

  @Override
  public List<JsonArray> methodWithListComplexJsonArrayReturn() {
    return Arrays.asList(new JsonArray().add(new JsonObject().put("foo", "hello")), new JsonArray().add(new JsonObject().put("bar", "bye")));
  }

  @Override
  public List<TestDataObject> methodWithListDataObjectReturn() {
    return Arrays.asList(new TestDataObject().setFoo("String 1").setBar(1).setWibble(1.1), new TestDataObject().setFoo("String 2").setBar(2).setWibble(2.2));
  }

  @Override
  public List<TestEnum> methodWithListEnumReturn() {
    return Arrays.asList(TestEnum.JULIEN, TestEnum.TIM);
  }

  @Override
  public List<Object> methodWithListObjectReturn() {
    return Arrays.asList("foo", 4, 3.4f, true, new JsonObject().put("wibble", "eek"), new JsonArray().add("one").add(2));
  }

  @Override
  public Set<Long> methodWithSetLongReturn() {
    return new LinkedHashSet<>(Arrays.asList(123l, 456l));
  }

  @Override
  public Set<RefedInterface1> methodWithSetVertxGenReturn() {
    return new LinkedHashSet<>(Arrays.asList(new RefedInterface1Impl().setString("foo"), new RefedInterface1Impl().setString("bar")));
  }

  @Override
  public Set<JsonObject> methodWithSetJsonObjectReturn() {
    return new LinkedHashSet<>(Arrays.asList(new JsonObject().put("foo", "bar"), new JsonObject().put("blah", "eek")));
  }

  @Override
  public Set<JsonObject> methodWithSetComplexJsonObjectReturn() {
    return new LinkedHashSet<>(Arrays.asList(new JsonObject().put("outer", new JsonObject().put("socks", "tartan")).put("list", new JsonArray().add("yellow").add("blue"))));
  }

  @Override
  public Set<JsonArray> methodWithSetJsonArrayReturn() {
    return new LinkedHashSet<>(Arrays.asList(new JsonArray().add("foo"), new JsonArray().add("blah")));
  }

  @Override
  public Set<JsonArray> methodWithSetComplexJsonArrayReturn() {
    return new LinkedHashSet<>(Arrays.asList(new JsonArray().add(new JsonObject().put("foo", "hello")), new JsonArray().add(new JsonObject().put("bar", "bye"))));
  }

  @Override
  public Set<TestDataObject> methodWithSetDataObjectReturn() {
    return new LinkedHashSet<>(methodWithListDataObjectReturn());
  }

  @Override
  public Set<TestEnum> methodWithSetEnumReturn() {
    return new LinkedHashSet<>(methodWithListEnumReturn());
  }

  @Override
  public Set<Object> methodWithSetObjectReturn() {
    return new LinkedHashSet<>(methodWithListObjectReturn());
  }

  private static class HandlerTestMap<V> implements Map<String, V> {
    private Handler<String> handler;
    private Map<String, V> map;

    private HandlerTestMap(Handler<String> handler) {
      map = new HashMap<>();
      this.handler = handler;
    }

    @Override
    public int size() {
      handler.handle("size()");
      return map.size();
    }

    @Override
    public boolean isEmpty() {
      handler.handle("isEmpty()");
      return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
      handler.handle("containsKey(" + key + ")");
      return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      handler.handle("containsValue(" + value + ")");
      return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
      handler.handle("get(" + key + ")");
      return map.get(key);
    }

    @Override
    public V put(String key, V value) {
      handler.handle("put(" + key + "," + value + ")");
      return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
      handler.handle("remove(" + key + ")");
      return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> m) {
      handler.handle("putAll(m)");
      map.putAll(m);
    }

    @Override
    public void clear() {
      handler.handle("clear()");
      map.clear();
    }

    @Override
    public Set<String> keySet() {
      handler.handle("keySet()");
      return map.keySet();
    }

    @Override
    public Collection<V> values() {
      handler.handle("values()");
      return map.values();
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
      handler.handle("entrySet()");
      return map.entrySet();
    }
  }

  private static class FloatHandlerTestMap extends HandlerTestMap<Float> {
    public FloatHandlerTestMap(Handler<String> handler) {
      super(handler);
    }

    /**
     * This method exists on purpose. On a put, this force a cast to Float allowing us to test
     * that values are converted properly.
     */
    @Override
    public Float put(String key, Float value) {
      return super.put(key, value);
    }
  }

  private static class CharacterHandlerTestMap extends HandlerTestMap<Character> {
    public CharacterHandlerTestMap(Handler<String> handler) {
      super(handler);
    }

    /**
     * This method exists on purpose. On a put, this force a cast to Character allowing us to test
     * that values are converted properly.
     */
    @Override
    public Character put(String key, Character value) {
      return super.put(key, value);
    }
  }

  private static class ByteHandlerTestMap extends HandlerTestMap<Byte> {
    public ByteHandlerTestMap(Handler<String> handler) {
      super(handler);
    }

    /**
     * This method exists on purpose. On a put, this force a cast to Byte allowing us to test
     * that values are converted properly.
     */
    @Override
    public Byte put(String key, Byte value) {
      return super.put(key, value);
    }
  }

  private static class IntegerHandlerTestMap extends HandlerTestMap<Integer> {
    public IntegerHandlerTestMap(Handler<String> handler) {
      super(handler);
    }

    /**
     * This method exists on purpose. On a put, this force a cast to Integer allowing us to test
     * that values are converted properly.
     */
    @Override
    public Integer put(String key, Integer value) {
      return super.put(key, value);
    }
  }

  private static class ShortHandlerTestMap extends HandlerTestMap<Short> {
    public ShortHandlerTestMap(Handler<String> handler) {
      super(handler);
    }

    /**
     * This method exists on purpose. On a put, this force a cast to Short allowing us to test
     * that values are converted properly.
     */
    @Override
    public Short put(String key, Short value) {
      return super.put(key, value);
    }
  }

  private static class LongHandlerTestMap extends HandlerTestMap<Long> {
    public LongHandlerTestMap(Handler<String> handler) {
      super(handler);
    }

    /**
     * This method exists on purpose. On a put, this force a cast to Long allowing us to test
     * that values are converted properly.
     */
    @Override
    public Long put(String key, Long value) {
      return super.put(key, value);
    }
  }

  private static class JsonArrayHandlerTestMap extends HandlerTestMap<JsonArray> {
    public JsonArrayHandlerTestMap(Handler<String> handler) {
      super(handler);
    }

    /**
     * This method exists on purpose. On a put, this force a cast to JsonArray allowing us to test
     * that values are converted properly.
     */
    @Override
    public JsonArray put(String key, JsonArray value) {
      return super.put(key, value);
    }
  }

  private static class ObjectHandlerTestMap extends HandlerTestMap<Object> {
    public ObjectHandlerTestMap(Handler<String> handler) {
      super(handler);
    }

    /**
     * This method exists on purpose. On a put, this force a cast to JsonArray allowing us to test
     * that values are converted properly.
     */
    @Override
    public Object put(String key, Object value) {
      return super.put(key, value);
    }
  }

  private static class DataObjectHandlerTestMap extends HandlerTestMap<TestDataObject> {
    public DataObjectHandlerTestMap(Handler<String> handler) {
      super(handler);
    }

    /**
     * This method exists on purpose. On a put, this force a cast to JsonArray allowing us to test
     * that values are converted properly.
     */
    @Override
    public TestDataObject put(String key, TestDataObject value) {
      return super.put(key, value);
    }
  }

  private static class EnumHandlerTestMap extends HandlerTestMap<TestEnum> {
    public EnumHandlerTestMap(Handler<String> handler) {
      super(handler);
    }

    /**
     * This method exists on purpose. On a put, this force a cast to JsonArray allowing us to test
     * that values are converted properly.
     */
    @Override
    public TestEnum put(String key, TestEnum value) {
      return super.put(key, value);
    }
  }

  private static class JsonObjectHandlerTestMap extends HandlerTestMap<JsonObject> {
    public JsonObjectHandlerTestMap(Handler<String> handler) {
      super(handler);
    }

    /**
     * This method exists on purpose. On a put, this force a cast to JsonObject allowing us to test
     * that values are converted properly.
     */
    @Override
    public JsonObject put(String key, JsonObject value) {
      return super.put(key, value);
    }
  }

  private static class StringHandlerTestMap extends HandlerTestMap<String> {
    public StringHandlerTestMap(Handler<String> handler) {
      super(handler);
    }

    /**
     * This method exists on purpose. On a put, this force a cast to String allowing us to test
     * that values are converted properly.
     */
    @Override
    public String put(String key, String value) {
      return super.put(key, value);
    }
  }

  private static class BooleanHandlerTestMap extends HandlerTestMap<Boolean> {
    public BooleanHandlerTestMap(Handler<String> handler) {
      super(handler);
    }

    /**
     * This method exists on purpose. On a put, this force a cast to Boolean allowing us to test
     * that values are converted properly.
     */
    @Override
    public Boolean put(String key, Boolean value) {
      return super.put(key, value);
    }
  }

  static class DoubleHandlerTestMap extends HandlerTestMap<Double> {
    public DoubleHandlerTestMap(Handler<String> handler) {
      super(handler);
    }
    /**
     * This method exists on purpose. On a put, this force a cast to Double allowing us to test
     * that values are converted properly.
     */
    @Override
    public Double put(String key, Double value) {
      return super.put(key, value);
    }
  }
}
