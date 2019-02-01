package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The TCK for @Nullable.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface NullableTCK {

  // Test @Nullable Byte type
  boolean methodWithNonNullableByteParam(Byte param);
  void methodWithNullableByteParam(boolean expectNull, @Nullable Byte param);
  void methodWithNullableByteHandler(boolean notNull, Handler<@Nullable Byte> handler);
  void methodWithNullableByteHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Byte>> handler);
  @Nullable Byte methodWithNullableByteReturn(boolean notNull);

  // Test @Nullable Short type
  boolean methodWithNonNullableShortParam(Short param);
  void methodWithNullableShortParam(boolean expectNull, @Nullable Short param);
  void methodWithNullableShortHandler(boolean notNull, Handler<@Nullable Short> handler);
  void methodWithNullableShortHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Short>> handler);
  @Nullable Short methodWithNullableShortReturn(boolean notNull);

  // Test @Nullable Integer type
  boolean methodWithNonNullableIntegerParam(Integer param);
  void methodWithNullableIntegerParam(boolean expectNull, @Nullable Integer param);
  void methodWithNullableIntegerHandler(boolean notNull, Handler<@Nullable Integer> handler);
  void methodWithNullableIntegerHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Integer>> handler);
  @Nullable Integer methodWithNullableIntegerReturn(boolean notNull);

  // Test @Nullable Long type
  boolean methodWithNonNullableLongParam(Long param);
  void methodWithNullableLongParam(boolean expectNull, @Nullable Long param);
  void methodWithNullableLongHandler(boolean notNull, Handler<@Nullable Long> handler);
  void methodWithNullableLongHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Long>> handler);
  @Nullable Long methodWithNullableLongReturn(boolean notNull);

  // Test @Nullable Float type
  boolean methodWithNonNullableFloatParam(Float param);
  void methodWithNullableFloatParam(boolean expectNull, @Nullable Float param);
  void methodWithNullableFloatHandler(boolean notNull, Handler<@Nullable Float> handler);
  void methodWithNullableFloatHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Float>> handler);
  @Nullable Float methodWithNullableFloatReturn(boolean notNull);

  // Test @Nullable Double type
  boolean methodWithNonNullableDoubleParam(Double param);
  void methodWithNullableDoubleParam(boolean expectNull, @Nullable Double param);
  void methodWithNullableDoubleHandler(boolean notNull, Handler<@Nullable Double> handler);
  void methodWithNullableDoubleHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Double>> handler);
  @Nullable Double methodWithNullableDoubleReturn(boolean notNull);

  // Test @Nullable Boolean type
  boolean methodWithNonNullableBooleanParam(Boolean param);
  void methodWithNullableBooleanParam(boolean expectNull, @Nullable Boolean param);
  void methodWithNullableBooleanHandler(boolean notNull, Handler<@Nullable Boolean> handler);
  void methodWithNullableBooleanHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Boolean>> handler);
  @Nullable Boolean methodWithNullableBooleanReturn(boolean notNull);

  // Test @Nullable String type
  boolean methodWithNonNullableStringParam(String param);
  void methodWithNullableStringParam(boolean expectNull, @Nullable String param);
  void methodWithNullableStringHandler(boolean notNull, Handler<@Nullable String> handler);
  void methodWithNullableStringHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable String>> handler);
  @Nullable String methodWithNullableStringReturn(boolean notNull);

  // Test @Nullable Char type
  boolean methodWithNonNullableCharParam(Character param);
  void methodWithNullableCharParam(boolean expectNull, @Nullable Character param);
  void methodWithNullableCharHandler(boolean notNull, Handler<@Nullable Character> handler);
  void methodWithNullableCharHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Character>> handler);
  @Nullable Character methodWithNullableCharReturn(boolean notNull);

  // Test @Nullable JsonObject type
  boolean methodWithNonNullableJsonObjectParam(JsonObject param);
  void methodWithNullableJsonObjectParam(boolean expectNull, @Nullable JsonObject param);
  void methodWithNullableJsonObjectHandler(boolean notNull, Handler<@Nullable JsonObject> handler);
  void methodWithNullableJsonObjectHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable JsonObject>> handler);
  @Nullable JsonObject methodWithNullableJsonObjectReturn(boolean notNull);

  // Test @Nullable JsonArray type
  boolean methodWithNonNullableJsonArrayParam(JsonArray param);
  void methodWithNullableJsonArrayParam(boolean expectNull, @Nullable JsonArray param);
  void methodWithNullableJsonArrayHandler(boolean notNull, Handler<@Nullable JsonArray> handler);
  void methodWithNullableJsonArrayHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable JsonArray>> handler);
  @Nullable JsonArray methodWithNullableJsonArrayReturn(boolean notNull);

  // Test @Nullable Api type
  boolean methodWithNonNullableApiParam(RefedInterface1 param);
  void methodWithNullableApiParam(boolean expectNull, @Nullable RefedInterface1 param);
  void methodWithNullableApiHandler(boolean notNull, Handler<@Nullable RefedInterface1> handler);
  void methodWithNullableApiHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable RefedInterface1>> handler);
  @Nullable RefedInterface1 methodWithNullableApiReturn(boolean notNull);

  // Test @Nullable DataObject type
  boolean methodWithNonNullableDataObjectParam(TestDataObject param);
  void methodWithNullableDataObjectParam(boolean expectNull, @Nullable TestDataObject param);
  void methodWithNullableDataObjectHandler(boolean notNull, Handler<@Nullable TestDataObject> handler);
  void methodWithNullableDataObjectHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable TestDataObject>> handler);
  @Nullable TestDataObject methodWithNullableDataObjectReturn(boolean notNull);

  // Test @Nullable Enum type
  boolean methodWithNonNullableEnumParam(TestEnum param);
  void methodWithNullableEnumParam(boolean expectNull, @Nullable TestEnum param);
  void methodWithNullableEnumHandler(boolean notNull, Handler<@Nullable TestEnum> handler);
  void methodWithNullableEnumHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable TestEnum>> handler);
  @Nullable TestEnum methodWithNullableEnumReturn(boolean notNull);

  // Test @Nullable Enum type
  boolean methodWithNonNullableGenEnumParam(TestGenEnum param);
  void methodWithNullableGenEnumParam(boolean expectNull, @Nullable TestGenEnum param);
  void methodWithNullableGenEnumHandler(boolean notNull, Handler<@Nullable TestGenEnum> handler);
  void methodWithNullableGenEnumHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable TestGenEnum>> handler);
  @Nullable TestGenEnum methodWithNullableGenEnumReturn(boolean notNull);

  // Test @Nullable type variable
  <T> void methodWithNullableTypeVariableParam(boolean expectNull, T param);
  <T> void methodWithNullableTypeVariableHandler(boolean notNull, T value, Handler<T> handler);
  <T> void methodWithNullableTypeVariableHandlerAsyncResult(boolean notNull, T value, Handler<AsyncResult<T>> handler);
  <T> @Nullable T methodWithNullableTypeVariableReturn(boolean notNull, T value);

  // Test @Nullable Object
  void methodWithNullableObjectParam(boolean expectNull, Object param);

  // Test @Nullable List<Byte> type
  boolean methodWithNonNullableListByteParam(List<Byte> param);
  void methodWithNullableListByteParam(boolean expectNull, @Nullable List<Byte> param);
  void methodWithNullableListByteHandler(boolean notNull, Handler<@Nullable List<Byte>> handler);
  void methodWithNullableListByteHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Byte>>> handler);
  @Nullable List<Byte> methodWithNullableListByteReturn(boolean notNull);

  // Test @Nullable List<Short> type
  boolean methodWithNonNullableListShortParam(List<Short> param);
  void methodWithNullableListShortParam(boolean expectNull, @Nullable List<Short> param);
  void methodWithNullableListShortHandler(boolean notNull, Handler<@Nullable List<Short>> handler);
  void methodWithNullableListShortHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Short>>> handler);
  @Nullable List<Short> methodWithNullableListShortReturn(boolean notNull);

  // Test @Nullable List<Integer> type
  boolean methodWithNonNullableListIntegerParam(List<Integer> param);
  void methodWithNullableListIntegerParam(boolean expectNull, @Nullable List<Integer> param);
  void methodWithNullableListIntegerHandler(boolean notNull, Handler<@Nullable List<Integer>> handler);
  void methodWithNullableListIntegerHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Integer>>> handler);
  @Nullable List<Integer> methodWithNullableListIntegerReturn(boolean notNull);

  // Test @Nullable List<Long> type
  boolean methodWithNonNullableListLongParam(List<Long> param);
  void methodWithNullableListLongParam(boolean expectNull, @Nullable List<Long> param);
  void methodWithNullableListLongHandler(boolean notNull, Handler<@Nullable List<Long>> handler);
  void methodWithNullableListLongHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Long>>> handler);
  @Nullable List<Long> methodWithNullableListLongReturn(boolean notNull);

  // Test @Nullable List<Float> type
  boolean methodWithNonNullableListFloatParam(List<Float> param);
  void methodWithNullableListFloatParam(boolean expectNull, @Nullable List<Float> param);
  void methodWithNullableListFloatHandler(boolean notNull, Handler<@Nullable List<Float>> handler);
  void methodWithNullableListFloatHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Float>>> handler);
  @Nullable List<Float> methodWithNullableListFloatReturn(boolean notNull);

  // Test @Nullable List<Double> type
  boolean methodWithNonNullableListDoubleParam(List<Double> param);
  void methodWithNullableListDoubleParam(boolean expectNull, @Nullable List<Double> param);
  void methodWithNullableListDoubleHandler(boolean notNull, Handler<@Nullable List<Double>> handler);
  void methodWithNullableListDoubleHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Double>>> handler);
  @Nullable List<Double> methodWithNullableListDoubleReturn(boolean notNull);

  // Test @Nullable List<Boolean> type
  boolean methodWithNonNullableListBooleanParam(List<Boolean> param);
  void methodWithNullableListBooleanParam(boolean expectNull, @Nullable List<Boolean> param);
  void methodWithNullableListBooleanHandler(boolean notNull, Handler<@Nullable List<Boolean>> handler);
  void methodWithNullableListBooleanHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Boolean>>> handler);
  @Nullable List<Boolean> methodWithNullableListBooleanReturn(boolean notNull);

  // Test @Nullable List<String> type
  boolean methodWithNonNullableListStringParam(List<String> param);
  void methodWithNullableListStringParam(boolean expectNull, @Nullable List<String> param);
  void methodWithNullableListStringHandler(boolean notNull, Handler<@Nullable List<String>> handler);
  void methodWithNullableListStringHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<String>>> handler);
  @Nullable List<String> methodWithNullableListStringReturn(boolean notNull);

  // Test @Nullable List<Character> type
  boolean methodWithNonNullableListCharParam(List<Character> param);
  void methodWithNullableListCharParam(boolean expectNull, @Nullable List<Character> param);
  void methodWithNullableListCharHandler(boolean notNull, Handler<@Nullable List<Character>> handler);
  void methodWithNullableListCharHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Character>>> handler);
  @Nullable List<Character> methodWithNullableListCharReturn(boolean notNull);

  // Test @Nullable List<JsonObject> type
  boolean methodWithNonNullableListJsonObjectParam(List<JsonObject> param);
  void methodWithNullableListJsonObjectParam(boolean expectNull, @Nullable List<JsonObject> param);
  void methodWithNullableListJsonObjectHandler(boolean notNull, Handler<@Nullable List<JsonObject>> handler);
  void methodWithNullableListJsonObjectHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<JsonObject>>> handler);
  @Nullable List<JsonObject> methodWithNullableListJsonObjectReturn(boolean notNull);

  // Test @Nullable List<JsonArray> type
  boolean methodWithNonNullableListJsonArrayParam(List<JsonArray> param);
  void methodWithNullableListJsonArrayParam(boolean expectNull, @Nullable List<JsonArray> param);
  void methodWithNullableListJsonArrayHandler(boolean notNull, Handler<@Nullable List<JsonArray>> handler);
  void methodWithNullableListJsonArrayHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<JsonArray>>> handler);
  @Nullable List<JsonArray> methodWithNullableListJsonArrayReturn(boolean notNull);

  // Test @Nullable List<Api> type
  boolean methodWithNonNullableListApiParam(List<RefedInterface1> param);
  void methodWithNullableListApiParam(boolean expectNull, @Nullable List<RefedInterface1> param);
  void methodWithNullableListApiHandler(boolean notNull, Handler<@Nullable List<RefedInterface1>> handler);
  void methodWithNullableListApiHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<RefedInterface1>>> handler);
  @Nullable List<RefedInterface1> methodWithNullableListApiReturn(boolean notNull);

  // Test @Nullable List<DataObject> type
  boolean methodWithNonNullableListDataObjectParam(List<TestDataObject> param);
  void methodWithNullableListDataObjectParam(boolean expectNull, @Nullable List<TestDataObject> param);
  void methodWithNullableListDataObjectHandler(boolean notNull, Handler<@Nullable List<TestDataObject>> handler);
  void methodWithNullableListDataObjectHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<TestDataObject>>> handler);
  @Nullable List<TestDataObject> methodWithNullableListDataObjectReturn(boolean notNull);

  // Test @Nullable List<Enum> type
  boolean methodWithNonNullableListEnumParam(List<TestEnum> param);
  void methodWithNullableListEnumParam(boolean expectNull, @Nullable List<TestEnum> param);
  void methodWithNullableListEnumHandler(boolean notNull, Handler<@Nullable List<TestEnum>> handler);
  void methodWithNullableListEnumHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<TestEnum>>> handler);
  @Nullable List<TestEnum> methodWithNullableListEnumReturn(boolean notNull);

  // Test @Nullable List<GenEnum> type
  boolean methodWithNonNullableListGenEnumParam(List<TestGenEnum> param);
  void methodWithNullableListGenEnumParam(boolean expectNull, @Nullable List<TestGenEnum> param);
  void methodWithNullableListGenEnumHandler(boolean notNull, Handler<@Nullable List<TestGenEnum>> handler);
  void methodWithNullableListGenEnumHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<TestGenEnum>>> handler);
  @Nullable List<TestGenEnum> methodWithNullableListGenEnumReturn(boolean notNull);

  // Test @Nullable Set<Byte> type
  boolean methodWithNonNullableSetByteParam(Set<Byte> param);
  void methodWithNullableSetByteParam(boolean expectNull, @Nullable Set<Byte> param);
  void methodWithNullableSetByteHandler(boolean notNull, Handler<@Nullable Set<Byte>> handler);
  void methodWithNullableSetByteHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Byte>>> handler);
  @Nullable Set<Byte> methodWithNullableSetByteReturn(boolean notNull);

  // Test @Nullable Set<Short> type
  boolean methodWithNonNullableSetShortParam(Set<Short> param);
  void methodWithNullableSetShortParam(boolean expectNull, @Nullable Set<Short> param);
  void methodWithNullableSetShortHandler(boolean notNull, Handler<@Nullable Set<Short>> handler);
  void methodWithNullableSetShortHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Short>>> handler);
  @Nullable Set<Short> methodWithNullableSetShortReturn(boolean notNull);

  // Test @Nullable Set<Integer> type
  boolean methodWithNonNullableSetIntegerParam(Set<Integer> param);
  void methodWithNullableSetIntegerParam(boolean expectNull, @Nullable Set<Integer> param);
  void methodWithNullableSetIntegerHandler(boolean notNull, Handler<@Nullable Set<Integer>> handler);
  void methodWithNullableSetIntegerHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Integer>>> handler);
  @Nullable Set<Integer> methodWithNullableSetIntegerReturn(boolean notNull);

  // Test @Nullable Set<Long> type
  boolean methodWithNonNullableSetLongParam(Set<Long> param);
  void methodWithNullableSetLongParam(boolean expectNull, @Nullable Set<Long> param);
  void methodWithNullableSetLongHandler(boolean notNull, Handler<@Nullable Set<Long>> handler);
  void methodWithNullableSetLongHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Long>>> handler);
  @Nullable Set<Long> methodWithNullableSetLongReturn(boolean notNull);

  // Test @Nullable Set<Float> type
  boolean methodWithNonNullableSetFloatParam(Set<Float> param);
  void methodWithNullableSetFloatParam(boolean expectNull, @Nullable Set<Float> param);
  void methodWithNullableSetFloatHandler(boolean notNull, Handler<@Nullable Set<Float>> handler);
  void methodWithNullableSetFloatHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Float>>> handler);
  @Nullable Set<Float> methodWithNullableSetFloatReturn(boolean notNull);

  // Test @Nullable Set<Double> type
  boolean methodWithNonNullableSetDoubleParam(Set<Double> param);
  void methodWithNullableSetDoubleParam(boolean expectNull, @Nullable Set<Double> param);
  void methodWithNullableSetDoubleHandler(boolean notNull, Handler<@Nullable Set<Double>> handler);
  void methodWithNullableSetDoubleHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Double>>> handler);
  @Nullable Set<Double> methodWithNullableSetDoubleReturn(boolean notNull);

  // Test @Nullable Set<Boolean> type
  boolean methodWithNonNullableSetBooleanParam(Set<Boolean> param);
  void methodWithNullableSetBooleanParam(boolean expectNull, @Nullable Set<Boolean> param);
  void methodWithNullableSetBooleanHandler(boolean notNull, Handler<@Nullable Set<Boolean>> handler);
  void methodWithNullableSetBooleanHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Boolean>>> handler);
  @Nullable Set<Boolean> methodWithNullableSetBooleanReturn(boolean notNull);

  // Test @Nullable Set<String> type
  boolean methodWithNonNullableSetStringParam(Set<String> param);
  void methodWithNullableSetStringParam(boolean expectNull, @Nullable Set<String> param);
  void methodWithNullableSetStringHandler(boolean notNull, Handler<@Nullable Set<String>> handler);
  void methodWithNullableSetStringHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<String>>> handler);
  @Nullable Set<String> methodWithNullableSetStringReturn(boolean notNull);

  // Test @Nullable Set<Character> type
  boolean methodWithNonNullableSetCharParam(Set<Character> param);
  void methodWithNullableSetCharParam(boolean expectNull, @Nullable Set<Character> param);
  void methodWithNullableSetCharHandler(boolean notNull, Handler<@Nullable Set<Character>> handler);
  void methodWithNullableSetCharHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Character>>> handler);
  @Nullable Set<Character> methodWithNullableSetCharReturn(boolean notNull);

  // Test @Nullable Set<JsonObject> type
  boolean methodWithNonNullableSetJsonObjectParam(Set<JsonObject> param);
  void methodWithNullableSetJsonObjectParam(boolean expectNull, @Nullable Set<JsonObject> param);
  void methodWithNullableSetJsonObjectHandler(boolean notNull, Handler<@Nullable Set<JsonObject>> handler);
  void methodWithNullableSetJsonObjectHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<JsonObject>>> handler);
  @Nullable Set<JsonObject> methodWithNullableSetJsonObjectReturn(boolean notNull);

  // Test @Nullable Set<JsonArray> type
  boolean methodWithNonNullableSetJsonArrayParam(Set<JsonArray> param);
  void methodWithNullableSetJsonArrayParam(boolean expectNull, @Nullable Set<JsonArray> param);
  void methodWithNullableSetJsonArrayHandler(boolean notNull, Handler<@Nullable Set<JsonArray>> handler);
  void methodWithNullableSetJsonArrayHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<JsonArray>>> handler);
  @Nullable Set<JsonArray> methodWithNullableSetJsonArrayReturn(boolean notNull);

  // Test @Nullable Set<Api> type
  boolean methodWithNonNullableSetApiParam(Set<RefedInterface1> param);
  void methodWithNullableSetApiParam(boolean expectNull, @Nullable Set<RefedInterface1> param);
  void methodWithNullableSetApiHandler(boolean notNull, Handler<@Nullable Set<RefedInterface1>> handler);
  void methodWithNullableSetApiHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<RefedInterface1>>> handler);
  @Nullable Set<RefedInterface1> methodWithNullableSetApiReturn(boolean notNull);

  // Test @Nullable Set<DataObject> type
  boolean methodWithNonNullableSetDataObjectParam(Set<TestDataObject> param);
  void methodWithNullableSetDataObjectParam(boolean expectNull, @Nullable Set<TestDataObject> param);
  void methodWithNullableSetDataObjectHandler(boolean notNull, Handler<@Nullable Set<TestDataObject>> handler);
  void methodWithNullableSetDataObjectHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<TestDataObject>>> handler);
  @Nullable Set<TestDataObject> methodWithNullableSetDataObjectReturn(boolean notNull);

  // Test @Nullable Set<Enum> type
  boolean methodWithNonNullableSetEnumParam(Set<TestEnum> param);
  void methodWithNullableSetEnumParam(boolean expectNull, @Nullable Set<TestEnum> param);
  void methodWithNullableSetEnumHandler(boolean notNull, Handler<@Nullable Set<TestEnum>> handler);
  void methodWithNullableSetEnumHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<TestEnum>>> handler);
  @Nullable Set<TestEnum> methodWithNullableSetEnumReturn(boolean notNull);

  // Test @Nullable Set<GenEnum> type
  boolean methodWithNonNullableSetGenEnumParam(Set<TestGenEnum> param);
  void methodWithNullableSetGenEnumParam(boolean expectNull, @Nullable Set<TestGenEnum> param);
  void methodWithNullableSetGenEnumHandler(boolean notNull, Handler<@Nullable Set<TestGenEnum>> handler);
  void methodWithNullableSetGenEnumHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<TestGenEnum>>> handler);
  @Nullable Set<TestGenEnum> methodWithNullableSetGenEnumReturn(boolean notNull);

  // Test @Nullable Map<String, Byte> type
  boolean methodWithNonNullableMapByteParam(Map<String, Byte> param);
  void methodWithNullableMapByteParam(boolean expectNull, @Nullable Map<String, Byte> param);
  void methodWithNullableMapByteHandler(boolean notNull, Handler<@Nullable Map<String, Byte>> handler);
  void methodWithNullableMapByteHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Byte>>> handler);
  @Nullable Map<String, Byte> methodWithNullableMapByteReturn(boolean notNull);

  // Test @Nullable Map<String, Short> type
  boolean methodWithNonNullableMapShortParam(Map<String, Short> param);
  void methodWithNullableMapShortParam(boolean expectNull, @Nullable Map<String, Short> param);
  void methodWithNullableMapShortHandler(boolean notNull, Handler<@Nullable Map<String, Short>> handler);
  void methodWithNullableMapShortHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Short>>> handler);
  @Nullable Map<String, Short> methodWithNullableMapShortReturn(boolean notNull);

  // Test @Nullable Map<String, Integer> type
  boolean methodWithNonNullableMapIntegerParam(Map<String, Integer> param);
  void methodWithNullableMapIntegerParam(boolean expectNull, @Nullable Map<String, Integer> param);
  void methodWithNullableMapIntegerHandler(boolean notNull, Handler<@Nullable Map<String, Integer>> handler);
  void methodWithNullableMapIntegerHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Integer>>> handler);
  @Nullable Map<String, Integer> methodWithNullableMapIntegerReturn(boolean notNull);

  // Test @Nullable Map<String, Long> type
  boolean methodWithNonNullableMapLongParam(Map<String, Long> param);
  void methodWithNullableMapLongParam(boolean expectNull, @Nullable Map<String, Long> param);
  void methodWithNullableMapLongHandler(boolean notNull, Handler<@Nullable Map<String, Long>> handler);
  void methodWithNullableMapLongHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Long>>> handler);
  @Nullable Map<String, Long> methodWithNullableMapLongReturn(boolean notNull);

  // Test @Nullable Map<String, Float> type
  boolean methodWithNonNullableMapFloatParam(Map<String, Float> param);
  void methodWithNullableMapFloatParam(boolean expectNull, @Nullable Map<String, Float> param);
  void methodWithNullableMapFloatHandler(boolean notNull, Handler<@Nullable Map<String, Float>> handler);
  void methodWithNullableMapFloatHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Float>>> handler);
  @Nullable Map<String, Float> methodWithNullableMapFloatReturn(boolean notNull);

  // Test @Nullable Map<String, Double> type
  boolean methodWithNonNullableMapDoubleParam(Map<String, Double> param);
  void methodWithNullableMapDoubleParam(boolean expectNull, @Nullable Map<String, Double> param);
  void methodWithNullableMapDoubleHandler(boolean notNull, Handler<@Nullable Map<String, Double>> handler);
  void methodWithNullableMapDoubleHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Double>>> handler);
  @Nullable Map<String, Double> methodWithNullableMapDoubleReturn(boolean notNull);

  // Test @Nullable Map<String, Boolean> type
  boolean methodWithNonNullableMapBooleanParam(Map<String, Boolean> param);
  void methodWithNullableMapBooleanParam(boolean expectNull, @Nullable Map<String, Boolean> param);
  void methodWithNullableMapBooleanHandler(boolean notNull, Handler<@Nullable Map<String, Boolean>> handler);
  void methodWithNullableMapBooleanHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Boolean>>> handler);
  @Nullable Map<String, Boolean> methodWithNullableMapBooleanReturn(boolean notNull);

  // Test @Nullable Map<String, String> type
  boolean methodWithNonNullableMapStringParam(Map<String, String> param);
  void methodWithNullableMapStringParam(boolean expectNull, @Nullable Map<String, String> param);
  void methodWithNullableMapStringHandler(boolean notNull, Handler<@Nullable Map<String, String>> handler);
  void methodWithNullableMapStringHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, String>>> handler);
  @Nullable Map<String, String> methodWithNullableMapStringReturn(boolean notNull);

  // Test @Nullable Map<String, Character> type
  boolean methodWithNonNullableMapCharParam(Map<String, Character> param);
  void methodWithNullableMapCharParam(boolean expectNull, @Nullable Map<String, Character> param);
  void methodWithNullableMapCharHandler(boolean notNull, Handler<@Nullable Map<String, Character>> handler);
  void methodWithNullableMapCharHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Character>>> handler);
  @Nullable Map<String, Character> methodWithNullableMapCharReturn(boolean notNull);

  // Test @Nullable Map<String, JsonObject> type
  boolean methodWithNonNullableMapJsonObjectParam(Map<String, JsonObject> param);
  void methodWithNullableMapJsonObjectParam(boolean expectNull, @Nullable Map<String, JsonObject> param);
  void methodWithNullableMapJsonObjectHandler(boolean notNull, Handler<@Nullable Map<String, JsonObject>> handler);
  void methodWithNullableMapJsonObjectHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, JsonObject>>> handler);
  @Nullable Map<String, JsonObject> methodWithNullableMapJsonObjectReturn(boolean notNull);

  // Test @Nullable Map<String, JsonArray> type
  boolean methodWithNonNullableMapJsonArrayParam(Map<String, JsonArray> param);
  void methodWithNullableMapJsonArrayParam(boolean expectNull, @Nullable Map<String, JsonArray> param);
  void methodWithNullableMapJsonArrayHandler(boolean notNull, Handler<@Nullable Map<String, JsonArray>> handler);
  void methodWithNullableMapJsonArrayHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, JsonArray>>> handler);
  @Nullable Map<String, JsonArray> methodWithNullableMapJsonArrayReturn(boolean notNull);

  // Test @Nullable Map<String, Api> type
  boolean methodWithNonNullableMapApiParam(Map<String, RefedInterface1> param);
  void methodWithNullableMapApiParam(boolean expectNull, @Nullable Map<String, RefedInterface1> param);

  // Test List<@Nullable Byte> type
  void methodWithListNullableByteParam(List<@Nullable Byte> param);
  void methodWithListNullableByteHandler(Handler<List<@Nullable Byte>> handler);
  void methodWithListNullableByteHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Byte>>> handler);
  List<@Nullable Byte> methodWithListNullableByteReturn();

  // Test List<@Nullable Short> type
  void methodWithListNullableShortParam(List<@Nullable Short> param);
  void methodWithListNullableShortHandler(Handler<List<@Nullable Short>> handler);
  void methodWithListNullableShortHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Short>>> handler);
  List<@Nullable Short> methodWithListNullableShortReturn();

  // Test List<@Nullable Integer> type
  void methodWithListNullableIntegerParam(List<@Nullable Integer> param);
  void methodWithListNullableIntegerHandler(Handler<List<@Nullable Integer>> handler);
  void methodWithListNullableIntegerHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Integer>>> handler);
  List<@Nullable Integer> methodWithListNullableIntegerReturn();

  // Test List<@Nullable Long> type
  void methodWithListNullableLongParam(List<@Nullable Long> param);
  void methodWithListNullableLongHandler(Handler<List<@Nullable Long>> handler);
  void methodWithListNullableLongHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Long>>> handler);
  List<@Nullable Long> methodWithListNullableLongReturn();

  // Test List<@Nullable Boolean> type
  void methodWithListNullableBooleanParam(List<@Nullable Boolean> param);
  void methodWithListNullableBooleanHandler(Handler<List<@Nullable Boolean>> handler);
  void methodWithListNullableBooleanHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Boolean>>> handler);
  List<@Nullable Boolean> methodWithListNullableBooleanReturn();

  // Test List<@Nullable Float> type
  void methodWithListNullableFloatParam(List<@Nullable Float> param);
  void methodWithListNullableFloatHandler(Handler<List<@Nullable Float>> handler);
  void methodWithListNullableFloatHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Float>>> handler);
  List<@Nullable Float> methodWithListNullableFloatReturn();

  // Test List<@Nullable Double> type
  void methodWithListNullableDoubleParam(List<@Nullable Double> param);
  void methodWithListNullableDoubleHandler(Handler<List<@Nullable Double>> handler);
  void methodWithListNullableDoubleHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Double>>> handler);
  List<@Nullable Double> methodWithListNullableDoubleReturn();

  // Test List<@Nullable String> type
  void methodWithListNullableStringParam(List<@Nullable String> param);
  void methodWithListNullableStringHandler(Handler<List<@Nullable String>> handler);
  void methodWithListNullableStringHandlerAsyncResult(Handler<AsyncResult<List<@Nullable String>>> handler);
  List<@Nullable String> methodWithListNullableStringReturn();

  // Test List<@Nullable Character> type
  void methodWithListNullableCharParam(List<@Nullable Character> param);
  void methodWithListNullableCharHandler(Handler<List<@Nullable Character>> handler);
  void methodWithListNullableCharHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Character>>> handler);
  List<@Nullable Character> methodWithListNullableCharReturn();

  // Test List<@Nullable JsonObject> type
  void methodWithListNullableJsonObjectParam(List<@Nullable JsonObject> param);
  void methodWithListNullableJsonObjectHandler(Handler<List<@Nullable JsonObject>> handler);
  void methodWithListNullableJsonObjectHandlerAsyncResult(Handler<AsyncResult<List<@Nullable JsonObject>>> handler);
  List<@Nullable JsonObject> methodWithListNullableJsonObjectReturn();

  // Test List<@Nullable String> type
  void methodWithListNullableJsonArrayParam(List<@Nullable JsonArray> param);
  void methodWithListNullableJsonArrayHandler(Handler<List<@Nullable JsonArray>> handler);
  void methodWithListNullableJsonArrayHandlerAsyncResult(Handler<AsyncResult<List<@Nullable JsonArray>>> handler);
  List<@Nullable JsonArray> methodWithListNullableJsonArrayReturn();

  // Test List<@Nullable Api> type
  void methodWithListNullableApiParam(List<@Nullable RefedInterface1> param);
  void methodWithListNullableApiHandler(Handler<List<@Nullable RefedInterface1>> handler);
  void methodWithListNullableApiHandlerAsyncResult(Handler<AsyncResult<List<@Nullable RefedInterface1>>> handler);
  List<@Nullable RefedInterface1> methodWithListNullableApiReturn();

  // Test List<@Nullable DataObject> type
  void methodWithListNullableDataObjectParam(List<@Nullable TestDataObject> param);
  void methodWithListNullableDataObjectHandler(Handler<List<@Nullable TestDataObject>> handler);
  void methodWithListNullableDataObjectHandlerAsyncResult(Handler<AsyncResult<List<@Nullable TestDataObject>>> handler);
  List<@Nullable TestDataObject> methodWithListNullableDataObjectReturn();

  // Test List<@Nullable Enum> type
  void methodWithListNullableEnumParam(List<@Nullable TestEnum> param);
  void methodWithListNullableEnumHandler(Handler<List<@Nullable TestEnum>> handler);
  void methodWithListNullableEnumHandlerAsyncResult(Handler<AsyncResult<List<@Nullable TestEnum>>> handler);
  List<@Nullable TestEnum> methodWithListNullableEnumReturn();

  // Test List<@Nullable GenEnum> type
  void methodWithListNullableGenEnumParam(List<@Nullable TestGenEnum> param);
  void methodWithListNullableGenEnumHandler(Handler<List<@Nullable TestGenEnum>> handler);
  void methodWithListNullableGenEnumHandlerAsyncResult(Handler<AsyncResult<List<@Nullable TestGenEnum>>> handler);
  List<@Nullable TestGenEnum> methodWithListNullableGenEnumReturn();

  // Test Set<@Nullable Byte> type
  void methodWithSetNullableByteParam(Set<@Nullable Byte> param);
  void methodWithSetNullableByteHandler(Handler<Set<@Nullable Byte>> handler);
  void methodWithSetNullableByteHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Byte>>> handler);
  Set<@Nullable Byte> methodWithSetNullableByteReturn();

  // Test Set<@Nullable Short> type
  void methodWithSetNullableShortParam(Set<@Nullable Short> param);
  void methodWithSetNullableShortHandler(Handler<Set<@Nullable Short>> handler);
  void methodWithSetNullableShortHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Short>>> handler);
  Set<@Nullable Short> methodWithSetNullableShortReturn();

  // Test Set<@Nullable Integer> type
  void methodWithSetNullableIntegerParam(Set<@Nullable Integer> param);
  void methodWithSetNullableIntegerHandler(Handler<Set<@Nullable Integer>> handler);
  void methodWithSetNullableIntegerHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Integer>>> handler);
  Set<@Nullable Integer> methodWithSetNullableIntegerReturn();

  // Test Set<@Nullable Long> type
  void methodWithSetNullableLongParam(Set<@Nullable Long> param);
  void methodWithSetNullableLongHandler(Handler<Set<@Nullable Long>> handler);
  void methodWithSetNullableLongHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Long>>> handler);
  Set<@Nullable Long> methodWithSetNullableLongReturn();

  // Test Set<@Nullable Boolean> type
  void methodWithSetNullableBooleanParam(Set<@Nullable Boolean> param);
  void methodWithSetNullableBooleanHandler(Handler<Set<@Nullable Boolean>> handler);
  void methodWithSetNullableBooleanHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Boolean>>> handler);
  Set<@Nullable Boolean> methodWithSetNullableBooleanReturn();

  // Test Set<@Nullable Float> type
  void methodWithSetNullableFloatParam(Set<@Nullable Float> param);
  void methodWithSetNullableFloatHandler(Handler<Set<@Nullable Float>> handler);
  void methodWithSetNullableFloatHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Float>>> handler);
  Set<@Nullable Float> methodWithSetNullableFloatReturn();

  // Test Set<@Nullable Double> type
  void methodWithSetNullableDoubleParam(Set<@Nullable Double> param);
  void methodWithSetNullableDoubleHandler(Handler<Set<@Nullable Double>> handler);
  void methodWithSetNullableDoubleHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Double>>> handler);
  Set<@Nullable Double> methodWithSetNullableDoubleReturn();

  // Test Set<@Nullable String> type
  void methodWithSetNullableStringParam(Set<@Nullable String> param);
  void methodWithSetNullableStringHandler(Handler<Set<@Nullable String>> handler);
  void methodWithSetNullableStringHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable String>>> handler);
  Set<@Nullable String> methodWithSetNullableStringReturn();

  // Test Set<@Nullable Character> type
  void methodWithSetNullableCharParam(Set<@Nullable Character> param);
  void methodWithSetNullableCharHandler(Handler<Set<@Nullable Character>> handler);
  void methodWithSetNullableCharHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Character>>> handler);
  Set<@Nullable Character> methodWithSetNullableCharReturn();

  // Test Set<@Nullable JsonObject> type
  void methodWithSetNullableJsonObjectParam(Set<@Nullable JsonObject> param);
  void methodWithSetNullableJsonObjectHandler(Handler<Set<@Nullable JsonObject>> handler);
  void methodWithSetNullableJsonObjectHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable JsonObject>>> handler);
  Set<@Nullable JsonObject> methodWithSetNullableJsonObjectReturn();

  // Test Set<@Nullable String> type
  void methodWithSetNullableJsonArrayParam(Set<@Nullable JsonArray> param);
  void methodWithSetNullableJsonArrayHandler(Handler<Set<@Nullable JsonArray>> handler);
  void methodWithSetNullableJsonArrayHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable JsonArray>>> handler);
  Set<@Nullable JsonArray> methodWithSetNullableJsonArrayReturn();

  // Test Set<@Nullable Api> type
  void methodWithSetNullableApiParam(Set<@Nullable RefedInterface1> param);
  void methodWithSetNullableApiHandler(Handler<Set<@Nullable RefedInterface1>> handler);
  void methodWithSetNullableApiHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable RefedInterface1>>> handler);
  Set<@Nullable RefedInterface1> methodWithSetNullableApiReturn();

  // Test Set<@Nullable DataObject> type
  void methodWithSetNullableDataObjectParam(Set<@Nullable TestDataObject> param);
  void methodWithSetNullableDataObjectHandler(Handler<Set<@Nullable TestDataObject>> handler);
  void methodWithSetNullableDataObjectHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable TestDataObject>>> handler);
  Set<@Nullable TestDataObject> methodWithSetNullableDataObjectReturn();

  // Test Set<@Nullable Enum> type
  void methodWithSetNullableEnumParam(Set<@Nullable TestEnum> param);
  void methodWithSetNullableEnumHandler(Handler<Set<@Nullable TestEnum>> handler);
  void methodWithSetNullableEnumHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable TestEnum>>> handler);
  Set<@Nullable TestEnum> methodWithSetNullableEnumReturn();

  // Test Set<@Nullable GenEnum> type
  void methodWithSetNullableGenEnumParam(Set<@Nullable TestGenEnum> param);
  void methodWithSetNullableGenEnumHandler(Handler<Set<@Nullable TestGenEnum>> handler);
  void methodWithSetNullableGenEnumHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable TestGenEnum>>> handler);
  Set<@Nullable TestGenEnum> methodWithSetNullableGenEnumReturn();

  // Test Map<String, @Nullable Byte> type
  void methodWithMapNullableByteParam(Map<String, @Nullable Byte> param);
  void methodWithMapNullableByteHandler(Handler<Map<String, @Nullable Byte>> handler);
  void methodWithMapNullableByteHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Byte>>> handler);
  Map<String, @Nullable Byte> methodWithMapNullableByteReturn();

  // Test Map<String, @Nullable Short> type
  void methodWithMapNullableShortParam(Map<String, @Nullable Short> param);
  void methodWithMapNullableShortHandler(Handler<Map<String, @Nullable Short>> handler);
  void methodWithMapNullableShortHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Short>>> handler);
  Map<String, @Nullable Short> methodWithMapNullableShortReturn();

  // Test Map<String, @Nullable Integer> type
  void methodWithMapNullableIntegerParam(Map<String, @Nullable Integer> param);
  void methodWithMapNullableIntegerHandler(Handler<Map<String, @Nullable Integer>> handler);
  void methodWithMapNullableIntegerHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Integer>>> handler);
  Map<String, @Nullable Integer> methodWithMapNullableIntegerReturn();

  // Test Map<String, @Nullable Long> type
  void methodWithMapNullableLongParam(Map<String, @Nullable Long> param);
  void methodWithMapNullableLongHandler(Handler<Map<String, @Nullable Long>> handler);
  void methodWithMapNullableLongHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Long>>> handler);
  Map<String, @Nullable Long> methodWithMapNullableLongReturn();

  // Test Map<String, @Nullable Boolean> type
  void methodWithMapNullableBooleanParam(Map<String, @Nullable Boolean> param);
  void methodWithMapNullableBooleanHandler(Handler<Map<String, @Nullable Boolean>> handler);
  void methodWithMapNullableBooleanHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Boolean>>> handler);
  Map<String, @Nullable Boolean> methodWithMapNullableBooleanReturn();

  // Test Map<String, @Nullable Float> type
  void methodWithMapNullableFloatParam(Map<String, @Nullable Float> param);
  void methodWithMapNullableFloatHandler(Handler<Map<String, @Nullable Float>> handler);
  void methodWithMapNullableFloatHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Float>>> handler);
  Map<String, @Nullable Float> methodWithMapNullableFloatReturn();

  // Test Map<String, @Nullable Double> type
  void methodWithMapNullableDoubleParam(Map<String, @Nullable Double> param);
  void methodWithMapNullableDoubleHandler(Handler<Map<String, @Nullable Double>> handler);
  void methodWithMapNullableDoubleHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Double>>> handler);
  Map<String, @Nullable Double> methodWithMapNullableDoubleReturn();

  // Test Map<String, @Nullable String> type
  void methodWithMapNullableStringParam(Map<String, @Nullable String> param);
  void methodWithMapNullableStringHandler(Handler<Map<String, @Nullable String>> handler);
  void methodWithMapNullableStringHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable String>>> handler);
  Map<String, @Nullable String> methodWithMapNullableStringReturn();

  // Test Map<String, @Nullable Character> type
  void methodWithMapNullableCharParam(Map<String, @Nullable Character> param);
  void methodWithMapNullableCharHandler(Handler<Map<String, @Nullable Character>> handler);
  void methodWithMapNullableCharHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Character>>> handler);
  Map<String, @Nullable Character> methodWithMapNullableCharReturn();

  // Test Map<String, @Nullable JsonObject> type
  void methodWithMapNullableJsonObjectParam(Map<String, @Nullable JsonObject> param);
  void methodWithMapNullableJsonObjectHandler(Handler<Map<String, @Nullable JsonObject>> handler);
  void methodWithMapNullableJsonObjectHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable JsonObject>>> handler);
  Map<String, @Nullable JsonObject> methodWithMapNullableJsonObjectReturn();

  // Test Map<String, @Nullable String> type
  void methodWithMapNullableJsonArrayParam(Map<String, @Nullable JsonArray> param);
  void methodWithMapNullableJsonArrayHandler(Handler<Map<String, @Nullable JsonArray>> handler);
  void methodWithMapNullableJsonArrayHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable JsonArray>>> handler);
  Map<String, @Nullable JsonArray> methodWithMapNullableJsonArrayReturn();

  // Test Map<String, @Nullable Api> type
  void methodWithMapNullableApiParam(Map<String, @Nullable RefedInterface1> param);

  // Test @Nullable handlers
  void methodWithNullableHandler(boolean expectNull, @Nullable Handler<String> handler);
  void methodWithNullableHandlerAsyncResult(boolean expectNull, @Nullable Handler<AsyncResult<String>> handler);

}
