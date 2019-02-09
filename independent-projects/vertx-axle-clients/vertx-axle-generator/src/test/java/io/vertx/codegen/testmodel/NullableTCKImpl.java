package io.vertx.codegen.testmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NullableTCKImpl implements NullableTCK {

  @Override
  public boolean methodWithNonNullableByteParam(Byte param) {
    return param == null;
  }

  @Override
  public void methodWithNullableByteParam(boolean expectNull, Byte param) {
    assertEquals(methodWithNullableByteReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableByteHandler(boolean notNull, Handler<@Nullable Byte> handler) {
    handler.handle(methodWithNullableByteReturn(notNull));
  }

  @Override
  public void methodWithNullableByteHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Byte>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableByteReturn(notNull)));
  }

  @Override
  public @Nullable Byte methodWithNullableByteReturn(boolean notNull) {
    return notNull ? (byte)67 : null;
  }

  @Override
  public boolean methodWithNonNullableShortParam(Short param) {
    return param == null;
  }

  @Override
  public void methodWithNullableShortParam(boolean expectNull, Short param) {
    assertEquals(methodWithNullableShortReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableShortHandler(boolean notNull, Handler<@Nullable Short> handler) {
    handler.handle(methodWithNullableShortReturn(notNull));
  }

  @Override
  public void methodWithNullableShortHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Short>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableShortReturn(notNull)));
  }

  @Override
  public @Nullable Short methodWithNullableShortReturn(boolean notNull) {
    return notNull ? (short)1024 : null;
  }

  @Override
  public boolean methodWithNonNullableIntegerParam(Integer param) {
    return param == null;
  }

  @Override
  public void methodWithNullableIntegerParam(boolean expectNull, Integer param) {
    assertEquals(methodWithNullableIntegerReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableIntegerHandler(boolean notNull, Handler<@Nullable Integer> handler) {
    handler.handle(methodWithNullableIntegerReturn(notNull));
  }

  @Override
  public void methodWithNullableIntegerHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Integer>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableIntegerReturn(notNull)));
  }

  @Override
  public @Nullable Integer methodWithNullableIntegerReturn(boolean notNull) {
    return notNull ? 1234567 : null;
  }

  @Override
  public boolean methodWithNonNullableLongParam(Long param) {
    return param == null;
  }

  @Override
  public void methodWithNullableLongParam(boolean expectNull, Long param) {
    assertEquals(methodWithNullableLongReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableLongHandler(boolean notNull, Handler<@Nullable Long> handler) {
    handler.handle(methodWithNullableLongReturn(notNull));
  }

  @Override
  public void methodWithNullableLongHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Long>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableLongReturn(notNull)));
  }

  @Override
  public @Nullable Long methodWithNullableLongReturn(boolean notNull) {
    return notNull ? 9876543210L : null;
  }

  @Override
  public boolean methodWithNonNullableFloatParam(Float param) {
    return param == null;
  }

  @Override
  public void methodWithNullableFloatParam(boolean expectNull, Float param) {
    assertEquals(methodWithNullableFloatReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableFloatHandler(boolean notNull, Handler<@Nullable Float> handler) {
    handler.handle(methodWithNullableFloatReturn(notNull));
  }

  @Override
  public void methodWithNullableFloatHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Float>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableFloatReturn(notNull)));
  }

  @Override
  public @Nullable Float methodWithNullableFloatReturn(boolean notNull) {
    return notNull ? 3.14f : null;
  }

  @Override
  public boolean methodWithNonNullableDoubleParam(Double param) {
    return param == null;
  }

  @Override
  public void methodWithNullableDoubleParam(boolean expectNull, Double param) {
    assertEquals(methodWithNullableDoubleReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableDoubleHandler(boolean notNull, Handler<@Nullable Double> handler) {
    handler.handle(methodWithNullableDoubleReturn(notNull));
  }

  @Override
  public void methodWithNullableDoubleHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Double>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableDoubleReturn(notNull)));
  }

  @Override
  public @Nullable Double methodWithNullableDoubleReturn(boolean notNull) {
    return notNull ? 3.1415926D : null;
  }

  @Override
  public boolean methodWithNonNullableBooleanParam(Boolean param) {
    return param == null;
  }

  @Override
  public void methodWithNullableBooleanParam(boolean expectNull, Boolean param) {
    assertEquals(methodWithNullableBooleanReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableBooleanHandler(boolean notNull, Handler<@Nullable Boolean> handler) {
    handler.handle(methodWithNullableBooleanReturn(notNull));
  }

  @Override
  public void methodWithNullableBooleanHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Boolean>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableBooleanReturn(notNull)));
  }

  @Override
  public @Nullable Boolean methodWithNullableBooleanReturn(boolean notNull) {
    return notNull ? true : null;
  }

  @Override
  public boolean methodWithNonNullableStringParam(String param) {
    return param == null;
  }

  @Override
  public void methodWithNullableStringParam(boolean expectNull, @Nullable String param) {
    assertEquals(methodWithNullableStringReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableStringHandler(boolean notNull, Handler<@Nullable String> handler) {
    handler.handle(methodWithNullableStringReturn(notNull));
  }

  @Override
  public void methodWithNullableStringHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable String>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableStringReturn(notNull)));
  }

  @Override
  public @Nullable String methodWithNullableStringReturn(boolean notNull) {
    return notNull ? "the_string_value" : null;
  }

  @Override
  public boolean methodWithNonNullableCharParam(Character param) {
    return param == null;
  }

  @Override
  public void methodWithNullableCharParam(boolean expectNull, Character param) {
    assertEquals(methodWithNullableCharReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableCharHandler(boolean notNull, Handler<@Nullable Character> handler) {
    handler.handle(methodWithNullableCharReturn(notNull));
  }

  @Override
  public void methodWithNullableCharHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Character>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableCharReturn(notNull)));
  }

  @Override
  public @Nullable Character methodWithNullableCharReturn(boolean notNull) {
    return notNull ? 'f' : null;
  }

  @Override
  public boolean methodWithNonNullableJsonObjectParam(JsonObject param) {
    return param == null;
  }

  @Override
  public void methodWithNullableJsonObjectParam(boolean expectNull, JsonObject param) {
    assertEquals(methodWithNullableJsonObjectReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableJsonObjectHandler(boolean notNull, Handler<@Nullable JsonObject> handler) {
    handler.handle(methodWithNullableJsonObjectReturn(notNull));
  }

  @Override
  public void methodWithNullableJsonObjectHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable JsonObject>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableJsonObjectReturn(notNull)));
  }

  @Override
  public @Nullable JsonObject methodWithNullableJsonObjectReturn(boolean notNull) {
    if (notNull) {
      return new JsonObject().put("foo", "wibble").put("bar", 3);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableJsonArrayParam(JsonArray param) {
    return param == null;
  }

  @Override
  public void methodWithNullableJsonArrayParam(boolean expectNull, JsonArray param) {
    assertEquals(methodWithNullableJsonArrayReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableJsonArrayHandler(boolean notNull, Handler<@Nullable JsonArray> handler) {
    handler.handle(methodWithNullableJsonArrayReturn(notNull));
  }

  @Override
  public void methodWithNullableJsonArrayHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable JsonArray>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableJsonArrayReturn(notNull)));
  }

  @Override
  public @Nullable JsonArray methodWithNullableJsonArrayReturn(boolean notNull) {
    return notNull ? new JsonArray().add("one").add("two").add("three") : null;
  }

  @Override
  public boolean methodWithNonNullableApiParam(RefedInterface1 param) {
    return param == null;
  }

  @Override
  public void methodWithNullableApiParam(boolean expectNull, RefedInterface1 param) {
    assertEquals(methodWithNullableApiReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableApiHandler(boolean notNull, Handler<RefedInterface1> handler) {
    handler.handle(methodWithNullableApiReturn(notNull));
  }

  @Override
  public void methodWithNullableApiHandlerAsyncResult(boolean notNull, Handler<AsyncResult<RefedInterface1>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableApiReturn(notNull)));
  }

  @Override
  public @Nullable RefedInterface1 methodWithNullableApiReturn(boolean notNull) {
    return notNull ? new RefedInterface1Impl().setString("lovely_dae") : null;
  }

  @Override
  public boolean methodWithNonNullableDataObjectParam(TestDataObject param) {
    return param == null;
  }

  @Override
  public void methodWithNullableDataObjectParam(boolean expectNull, TestDataObject param) {
    if (expectNull) {
      assertNull(param);
    } else {
      assertEquals(methodWithNullableDataObjectReturn(true).toJson(), param.toJson());
    }
  }

  @Override
  public void methodWithNullableDataObjectHandler(boolean notNull, Handler<TestDataObject> handler) {
    handler.handle(methodWithNullableDataObjectReturn(notNull));
  }

  @Override
  public void methodWithNullableDataObjectHandlerAsyncResult(boolean notNull, Handler<AsyncResult<TestDataObject>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableDataObjectReturn(notNull)));
  }

  @Override
  public @Nullable TestDataObject methodWithNullableDataObjectReturn(boolean notNull) {
    return notNull ? new TestDataObject().setFoo("foo_value").setBar(12345).setWibble(3.5) : null;
  }

  @Override
  public boolean methodWithNonNullableEnumParam(TestEnum param) {
    return param == null;
  }

  @Override
  public void methodWithNullableEnumParam(boolean expectNull, TestEnum param) {
    assertEquals(methodWithNullableEnumReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableEnumHandler(boolean notNull, Handler<TestEnum> handler) {
    handler.handle(methodWithNullableEnumReturn(notNull));
  }

  @Override
  public void methodWithNullableEnumHandlerAsyncResult(boolean notNull, Handler<AsyncResult<TestEnum>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableEnumReturn(notNull)));
  }

  @Override
  public @Nullable TestEnum methodWithNullableEnumReturn(boolean notNull) {
    return notNull ? TestEnum.TIM : null;
  }

  @Override
  public boolean methodWithNonNullableGenEnumParam(TestGenEnum param) {
    return param == null;
  }

  @Override
  public void methodWithNullableGenEnumParam(boolean expectNull, TestGenEnum param) {
    assertEquals(methodWithNullableGenEnumReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableGenEnumHandler(boolean notNull, Handler<TestGenEnum> handler) {
    handler.handle(methodWithNullableGenEnumReturn(notNull));
  }

  @Override
  public void methodWithNullableGenEnumHandlerAsyncResult(boolean notNull, Handler<AsyncResult<TestGenEnum>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableGenEnumReturn(notNull)));
  }

  @Override
  public @Nullable TestGenEnum methodWithNullableGenEnumReturn(boolean notNull) {
    return notNull ? TestGenEnum.MIKE : null;
  }

  @Override
  public <T> void methodWithNullableTypeVariableParam(boolean expectNull, T param) {
    if (expectNull) {
      assertNull(param);
    } else {
      assertNotNull(param);
    }
  }

  @Override
  public <T> void methodWithNullableTypeVariableHandler(boolean notNull, T value, Handler<T> handler) {
    if (notNull) {
      handler.handle(value);
    } else {
      handler.handle(null);
    }
  }

  @Override
  public <T> void methodWithNullableTypeVariableHandlerAsyncResult(boolean notNull, T value, Handler<AsyncResult<T>> handler) {
    if (notNull) {
      handler.handle(Future.succeededFuture(value));
    } else {
      handler.handle(Future.succeededFuture(null));
    }
}

  @Override
  public <T> T methodWithNullableTypeVariableReturn(boolean notNull, T value) {
    return notNull ? value : null;
  }

  @Override
  public void methodWithNullableObjectParam(boolean expectNull, Object param) {
    assertEquals(expectNull ? null : "object_param", param);
  }

  @Override
  public boolean methodWithNonNullableListByteParam(List<Byte> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListByteParam(boolean expectNull, List<Byte> param) {
    assertEquals(methodWithNullableListByteReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableListByteHandler(boolean notNull, Handler<@Nullable List<Byte>> handler) {
    handler.handle(methodWithNullableListByteReturn(notNull));
  }

  @Override
  public void methodWithNullableListByteHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Byte>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListByteReturn(notNull)));
  }

  @Override
  public @Nullable List<Byte> methodWithNullableListByteReturn(boolean notNull) {
    if (notNull) {
      return Arrays.asList((byte)12, (byte)24, (byte)-12);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableListShortParam(List<Short> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListShortParam(boolean expectNull, List<Short> param) {
    assertEquals(methodWithNullableListShortReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableListShortHandler(boolean notNull, Handler<@Nullable List<Short>> handler) {
    handler.handle(methodWithNullableListShortReturn(notNull));
  }

  @Override
  public void methodWithNullableListShortHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Short>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListShortReturn(notNull)));
  }

  @Override
  public @Nullable List<Short> methodWithNullableListShortReturn(boolean notNull) {
    if (notNull) {
      return Arrays.asList((short)520, (short)1040, (short)-520);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableListIntegerParam(List<Integer> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListIntegerParam(boolean expectNull, List<Integer> param) {
    assertEquals(methodWithNullableListIntegerReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableListIntegerHandler(boolean notNull, Handler<@Nullable List<Integer>> handler) {
    handler.handle(methodWithNullableListIntegerReturn(notNull));
  }

  @Override
  public void methodWithNullableListIntegerHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Integer>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListIntegerReturn(notNull)));
  }

  @Override
  public @Nullable List<Integer> methodWithNullableListIntegerReturn(boolean notNull) {
    if (notNull) {
      return Arrays.asList(12345, 54321, -12345);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableListLongParam(List<Long> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListLongParam(boolean expectNull, List<Long> param) {
    assertEquals(methodWithNullableListLongReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableListLongHandler(boolean notNull, Handler<@Nullable List<Long>> handler) {
    handler.handle(methodWithNullableListLongReturn(notNull));
  }

  @Override
  public void methodWithNullableListLongHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Long>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListLongReturn(notNull)));
  }

  @Override
  public @Nullable List<Long> methodWithNullableListLongReturn(boolean notNull) {
    if (notNull) {
      return Arrays.asList(123456789L, 987654321L, -123456789L);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableListFloatParam(List<Float> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListFloatParam(boolean expectNull, List<Float> param) {
    assertEquals(methodWithNullableListFloatReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableListFloatHandler(boolean notNull, Handler<@Nullable List<Float>> handler) {
    handler.handle(methodWithNullableListFloatReturn(notNull));
  }

  @Override
  public void methodWithNullableListFloatHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Float>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListFloatReturn(notNull)));
  }

  @Override
  public @Nullable List<Float> methodWithNullableListFloatReturn(boolean notNull) {
    if (notNull) {
      return Arrays.asList(1.1f, 2.2f, 3.3f);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableListDoubleParam(List<Double> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListDoubleParam(boolean expectNull, List<Double> param) {
    assertEquals(methodWithNullableListDoubleReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableListDoubleHandler(boolean notNull, Handler<@Nullable List<Double>> handler) {
    handler.handle(methodWithNullableListDoubleReturn(notNull));
  }

  @Override
  public void methodWithNullableListDoubleHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Double>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListDoubleReturn(notNull)));
  }

  @Override
  public @Nullable List<Double> methodWithNullableListDoubleReturn(boolean notNull) {
    if (notNull) {
      return Arrays.asList(1.11d, 2.22d, 3.33d);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableListBooleanParam(List<Boolean> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListBooleanParam(boolean expectNull, List<Boolean> param) {
    assertEquals(methodWithNullableListBooleanReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableListBooleanHandler(boolean notNull, Handler<@Nullable List<Boolean>> handler) {
    handler.handle(methodWithNullableListBooleanReturn(notNull));
  }

  @Override
  public void methodWithNullableListBooleanHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Boolean>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListBooleanReturn(notNull)));
  }

  @Override
  public @Nullable List<Boolean> methodWithNullableListBooleanReturn(boolean notNull) {
    if (notNull) {
      return Arrays.asList(true, false, true);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableListStringParam(List<String> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListStringParam(boolean expectNull, List<String> param) {
    assertEquals(methodWithNullableListStringReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableListStringHandler(boolean notNull, Handler<@Nullable List<String>> handler) {
    handler.handle(methodWithNullableListStringReturn(notNull));
  }

  @Override
  public void methodWithNullableListStringHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<String>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListStringReturn(notNull)));
  }

  @Override
  public @Nullable List<String> methodWithNullableListStringReturn(boolean notNull) {
    if (notNull) {
      return Arrays.asList("first", "second", "third");
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableListCharParam(List<Character> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListCharParam(boolean expectNull, List<Character> param) {
    assertEquals(methodWithNullableListCharReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableListCharHandler(boolean notNull, Handler<@Nullable List<Character>> handler) {
    handler.handle(methodWithNullableListCharReturn(notNull));
  }

  @Override
  public void methodWithNullableListCharHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<Character>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListCharReturn(notNull)));
  }

  @Override
  public @Nullable List<Character> methodWithNullableListCharReturn(boolean notNull) {
    if (notNull) {
      return Arrays.asList('x', 'y', 'z');
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableListJsonObjectParam(List<JsonObject> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListJsonObjectParam(boolean expectNull, List<JsonObject> param) {
    assertEquals(methodWithNullableListJsonObjectReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableListJsonObjectHandler(boolean notNull, Handler<@Nullable List<JsonObject>> handler) {
    handler.handle(methodWithNullableListJsonObjectReturn(notNull));
  }

  @Override
  public void methodWithNullableListJsonObjectHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<JsonObject>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListJsonObjectReturn(notNull)));
  }

  @Override
  public @Nullable List<JsonObject> methodWithNullableListJsonObjectReturn(boolean notNull) {
    if (notNull) {
      return Arrays.asList(new JsonObject().put("foo", "bar"), new JsonObject().put("juu", 3));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableListJsonArrayParam(List<JsonArray> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListJsonArrayParam(boolean expectNull, List<JsonArray> param) {
    assertEquals(methodWithNullableListJsonArrayReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableListJsonArrayHandler(boolean notNull, Handler<@Nullable List<JsonArray>> handler) {
    handler.handle(methodWithNullableListJsonArrayReturn(notNull));
  }

  @Override
  public void methodWithNullableListJsonArrayHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<JsonArray>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListJsonArrayReturn(notNull)));
  }

  @Override
  public @Nullable List<JsonArray> methodWithNullableListJsonArrayReturn(boolean notNull) {
    if (notNull) {
      return Arrays.asList(new JsonArray().add("foo").add("bar"), new JsonArray().add("juu"));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableListApiParam(List<RefedInterface1> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListApiParam(boolean expectNull, List<RefedInterface1> param) {
    assertEquals(methodWithNullableListApiReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableListApiHandler(boolean notNull, Handler<@Nullable List<RefedInterface1>> handler) {
    handler.handle(methodWithNullableListApiReturn(notNull));
  }

  @Override
  public void methodWithNullableListApiHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<RefedInterface1>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListApiReturn(notNull)));
  }

  @Override
  public @Nullable List<RefedInterface1> methodWithNullableListApiReturn(boolean notNull) {
    return notNull ? Arrays.asList(new RefedInterface1Impl().setString("refed_is_here")) : null;
  }

  @Override
  public boolean methodWithNonNullableListDataObjectParam(List<TestDataObject> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListDataObjectParam(boolean expectNull, List<TestDataObject> param) {
    if (expectNull) {
      assertEquals(null, param);
    } else {
      assertEquals(methodWithNullableListDataObjectReturn(true).stream().map(TestDataObject::toJson).collect(Collectors.toList()), param.stream().map(TestDataObject::toJson).collect(Collectors.toList()));
    }
  }

  @Override
  public void methodWithNullableListDataObjectHandler(boolean notNull, Handler<@Nullable List<TestDataObject>> handler) {
    handler.handle(methodWithNullableListDataObjectReturn(notNull));
  }

  @Override
  public void methodWithNullableListDataObjectHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<TestDataObject>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListDataObjectReturn(notNull)));
  }

  @Override
  public @Nullable List<TestDataObject> methodWithNullableListDataObjectReturn(boolean notNull) {
    return notNull ? Arrays.asList(new TestDataObject().setFoo("foo_value").setBar(12345).setWibble(5.6)) : null;
  }

  @Override
  public boolean methodWithNonNullableListEnumParam(List<TestEnum> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListEnumParam(boolean expectNull, List<TestEnum> param) {
    assertEquals(methodWithNullableListEnumReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableListEnumHandler(boolean notNull, Handler<@Nullable List<TestEnum>> handler) {
    handler.handle(methodWithNullableListEnumReturn(notNull));
  }

  @Override
  public void methodWithNullableListEnumHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<TestEnum>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListEnumReturn(notNull)));
  }

  @Override
  public @Nullable List<TestEnum> methodWithNullableListEnumReturn(boolean notNull) {
    return notNull ? Arrays.asList(TestEnum.TIM,TestEnum.JULIEN) : null;
  }

  @Override
  public boolean methodWithNonNullableListGenEnumParam(List<TestGenEnum> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableListGenEnumParam(boolean expectNull, List<TestGenEnum> param) {
    assertEquals(methodWithNullableListGenEnumReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableListGenEnumHandler(boolean notNull, Handler<@Nullable List<TestGenEnum>> handler) {
    handler.handle(methodWithNullableListGenEnumReturn(notNull));
  }

  @Override
  public void methodWithNullableListGenEnumHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable List<TestGenEnum>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableListGenEnumReturn(notNull)));
  }

  @Override
  public @Nullable List<TestGenEnum> methodWithNullableListGenEnumReturn(boolean notNull) {
    return notNull ? Arrays.asList(TestGenEnum.BOB,TestGenEnum.LELAND) : null;
  }

  @Override
  public boolean methodWithNonNullableSetByteParam(Set<Byte> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetByteParam(boolean expectNull, Set<Byte> param) {
    assertEquals(methodWithNullableSetByteReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableSetByteHandler(boolean notNull, Handler<@Nullable Set<Byte>> handler) {
    handler.handle(methodWithNullableSetByteReturn(notNull));
  }

  @Override
  public void methodWithNullableSetByteHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Byte>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetByteReturn(notNull)));
  }

  @Override
  public @Nullable Set<Byte> methodWithNullableSetByteReturn(boolean notNull) {
    if (notNull) {
      return new LinkedHashSet<>(Arrays.asList((byte)12, (byte)24, (byte)-12));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableSetShortParam(Set<Short> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetShortParam(boolean expectNull, Set<Short> param) {
    assertEquals(methodWithNullableSetShortReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableSetShortHandler(boolean notNull, Handler<@Nullable Set<Short>> handler) {
    handler.handle(methodWithNullableSetShortReturn(notNull));
  }

  @Override
  public void methodWithNullableSetShortHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Short>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetShortReturn(notNull)));
  }

  @Override
  public @Nullable Set<Short> methodWithNullableSetShortReturn(boolean notNull) {
    if (notNull) {
      return new LinkedHashSet<>(Arrays.asList((short)520, (short)1040, (short)-520));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableSetIntegerParam(Set<Integer> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetIntegerParam(boolean expectNull, Set<Integer> param) {
    assertEquals(methodWithNullableSetIntegerReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableSetIntegerHandler(boolean notNull, Handler<@Nullable Set<Integer>> handler) {
    handler.handle(methodWithNullableSetIntegerReturn(notNull));
  }

  @Override
  public void methodWithNullableSetIntegerHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Integer>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetIntegerReturn(notNull)));
  }

  @Override
  public @Nullable Set<Integer> methodWithNullableSetIntegerReturn(boolean notNull) {
    if (notNull) {
      return new LinkedHashSet<>(Arrays.asList(12345, 54321, -12345));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableSetLongParam(Set<Long> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetLongParam(boolean expectNull, Set<Long> param) {
    assertEquals(methodWithNullableSetLongReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableSetLongHandler(boolean notNull, Handler<@Nullable Set<Long>> handler) {
    handler.handle(methodWithNullableSetLongReturn(notNull));
  }

  @Override
  public void methodWithNullableSetLongHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Long>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetLongReturn(notNull)));
  }

  @Override
  public @Nullable Set<Long> methodWithNullableSetLongReturn(boolean notNull) {
    if (notNull) {
      return new LinkedHashSet<>(Arrays.asList(123456789L, 987654321L, -123456789L));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableSetFloatParam(Set<Float> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetFloatParam(boolean expectNull, Set<Float> param) {
    assertEquals(methodWithNullableSetFloatReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableSetFloatHandler(boolean notNull, Handler<@Nullable Set<Float>> handler) {
    handler.handle(methodWithNullableSetFloatReturn(notNull));
  }

  @Override
  public void methodWithNullableSetFloatHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Float>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetFloatReturn(notNull)));
  }

  @Override
  public @Nullable Set<Float> methodWithNullableSetFloatReturn(boolean notNull) {
    if (notNull) {
      return new LinkedHashSet<>(Arrays.asList(1.1f, 2.2f, 3.3f));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableSetDoubleParam(Set<Double> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetDoubleParam(boolean expectNull, Set<Double> param) {
    assertEquals(methodWithNullableSetDoubleReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableSetDoubleHandler(boolean notNull, Handler<@Nullable Set<Double>> handler) {
    handler.handle(methodWithNullableSetDoubleReturn(notNull));
  }

  @Override
  public void methodWithNullableSetDoubleHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Double>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetDoubleReturn(notNull)));
  }

  @Override
  public @Nullable Set<Double> methodWithNullableSetDoubleReturn(boolean notNull) {
    if (notNull) {
      return new LinkedHashSet<>(Arrays.asList(1.11d, 2.22d, 3.33d));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableSetBooleanParam(Set<Boolean> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetBooleanParam(boolean expectNull, Set<Boolean> param) {
    assertEquals(methodWithNullableSetBooleanReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableSetBooleanHandler(boolean notNull, Handler<@Nullable Set<Boolean>> handler) {
    handler.handle(methodWithNullableSetBooleanReturn(notNull));
  }

  @Override
  public void methodWithNullableSetBooleanHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Boolean>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetBooleanReturn(notNull)));
  }

  @Override
  public @Nullable Set<Boolean> methodWithNullableSetBooleanReturn(boolean notNull) {
    if (notNull) {
      return new LinkedHashSet<>(Arrays.asList(true, false));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableSetStringParam(Set<String> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetStringParam(boolean expectNull, Set<String> param) {
    assertEquals(methodWithNullableSetStringReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableSetStringHandler(boolean notNull, Handler<@Nullable Set<String>> handler) {
    handler.handle(methodWithNullableSetStringReturn(notNull));
  }

  @Override
  public void methodWithNullableSetStringHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<String>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetStringReturn(notNull)));
  }

  @Override
  public @Nullable Set<String> methodWithNullableSetStringReturn(boolean notNull) {
    if (notNull) {
      return new LinkedHashSet<>(Arrays.asList("first", "second", "third"));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableSetCharParam(Set<Character> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetCharParam(boolean expectNull, Set<Character> param) {
    assertEquals(methodWithNullableSetCharReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableSetCharHandler(boolean notNull, Handler<@Nullable Set<Character>> handler) {
    handler.handle(methodWithNullableSetCharReturn(notNull));
  }

  @Override
  public void methodWithNullableSetCharHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<Character>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetCharReturn(notNull)));
  }

  @Override
  public @Nullable Set<Character> methodWithNullableSetCharReturn(boolean notNull) {
    if (notNull) {
      return new LinkedHashSet<>(Arrays.asList('x', 'y', 'z'));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableSetJsonObjectParam(Set<JsonObject> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetJsonObjectParam(boolean expectNull, Set<JsonObject> param) {
    assertEquals(methodWithNullableSetJsonObjectReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableSetJsonObjectHandler(boolean notNull, Handler<@Nullable Set<JsonObject>> handler) {
    handler.handle(methodWithNullableSetJsonObjectReturn(notNull));
  }

  @Override
  public void methodWithNullableSetJsonObjectHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<JsonObject>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetJsonObjectReturn(notNull)));
  }

  @Override
  public @Nullable Set<JsonObject> methodWithNullableSetJsonObjectReturn(boolean notNull) {
    if (notNull) {
      return new LinkedHashSet<>(Arrays.asList(new JsonObject().put("foo", "bar"), new JsonObject().put("juu", 3)));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableSetJsonArrayParam(Set<JsonArray> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetJsonArrayParam(boolean expectNull, Set<JsonArray> param) {
    assertEquals(methodWithNullableSetJsonArrayReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableSetJsonArrayHandler(boolean notNull, Handler<@Nullable Set<JsonArray>> handler) {
    handler.handle(methodWithNullableSetJsonArrayReturn(notNull));
  }

  @Override
  public void methodWithNullableSetJsonArrayHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<JsonArray>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetJsonArrayReturn(notNull)));
  }

  @Override
  public @Nullable Set<JsonArray> methodWithNullableSetJsonArrayReturn(boolean notNull) {
    if (notNull) {
      return new LinkedHashSet<>(Arrays.asList(new JsonArray().add("foo").add("bar"), new JsonArray().add("juu")));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableSetApiParam(Set<RefedInterface1> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetApiParam(boolean expectNull, Set<RefedInterface1> param) {
    assertEquals(methodWithNullableSetApiReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableSetApiHandler(boolean notNull, Handler<@Nullable Set<RefedInterface1>> handler) {
    handler.handle(methodWithNullableSetApiReturn(notNull));
  }

  @Override
  public void methodWithNullableSetApiHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<RefedInterface1>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetApiReturn(notNull)));
  }

  @Override
  public @Nullable Set<RefedInterface1> methodWithNullableSetApiReturn(boolean notNull) {
    return notNull ? new LinkedHashSet<>(Arrays.asList(new RefedInterface1Impl().setString("refed_is_here"))) : null;
  }

  @Override
  public boolean methodWithNonNullableSetDataObjectParam(Set<TestDataObject> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetDataObjectParam(boolean expectNull, Set<TestDataObject> param) {
    if (expectNull) {
      assertEquals(null, param);
    } else {
      assertEquals(methodWithNullableSetDataObjectReturn(true).stream().map(TestDataObject::toJson).collect(Collectors.toSet()), param.stream().map(TestDataObject::toJson).collect(Collectors.toSet()));
    }
  }

  @Override
  public void methodWithNullableSetDataObjectHandler(boolean notNull, Handler<@Nullable Set<TestDataObject>> handler) {
    handler.handle(methodWithNullableSetDataObjectReturn(notNull));
  }

  @Override
  public void methodWithNullableSetDataObjectHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<TestDataObject>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetDataObjectReturn(notNull)));
  }

  @Override
  public @Nullable Set<TestDataObject> methodWithNullableSetDataObjectReturn(boolean notNull) {
    return notNull ? new LinkedHashSet<>(Arrays.asList(new TestDataObject().setFoo("foo_value").setBar(12345).setWibble(5.6))) : null;
  }

  @Override
  public boolean methodWithNonNullableSetEnumParam(Set<TestEnum> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetEnumParam(boolean expectNull, Set<TestEnum> param) {
    assertEquals(methodWithNullableSetEnumReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableSetEnumHandler(boolean notNull, Handler<@Nullable Set<TestEnum>> handler) {
    handler.handle(methodWithNullableSetEnumReturn(notNull));
  }

  @Override
  public void methodWithNullableSetEnumHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<TestEnum>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetEnumReturn(notNull)));
  }

  @Override
  public @Nullable Set<TestEnum> methodWithNullableSetEnumReturn(boolean notNull) {
    return notNull ? new LinkedHashSet<>(Arrays.asList(TestEnum.TIM,TestEnum.JULIEN)) : null;
  }

  @Override
  public boolean methodWithNonNullableSetGenEnumParam(Set<TestGenEnum> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableSetGenEnumParam(boolean expectNull, Set<TestGenEnum> param) {
    assertEquals(methodWithNullableSetGenEnumReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableSetGenEnumHandler(boolean notNull, Handler<@Nullable Set<TestGenEnum>> handler) {
    handler.handle(methodWithNullableSetGenEnumReturn(notNull));
  }

  @Override
  public void methodWithNullableSetGenEnumHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Set<TestGenEnum>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableSetGenEnumReturn(notNull)));
  }

  @Override
  public @Nullable Set<TestGenEnum> methodWithNullableSetGenEnumReturn(boolean notNull) {
    return notNull ? new LinkedHashSet<>(Arrays.asList(TestGenEnum.BOB,TestGenEnum.LELAND)) : null;
  }

  @Override
  public boolean methodWithNonNullableMapByteParam(Map<String, Byte> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableMapByteParam(boolean expectNull, Map<String, Byte> param) {
    assertEquals(methodWithNullableMapByteReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableMapByteHandler(boolean notNull, Handler<@Nullable Map<String, Byte>> handler) {
    handler.handle(methodWithNullableMapByteReturn(notNull));
  }

  @Override
  public void methodWithNullableMapByteHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Byte>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableMapByteReturn(notNull)));
  }

  @Override
  public @Nullable Map<String, Byte> methodWithNullableMapByteReturn(boolean notNull) {
    if (notNull) {
      return map((byte)1, (byte)2, (byte)3);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableMapShortParam(Map<String, Short> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableMapShortParam(boolean expectNull, Map<String, Short> param) {
    assertEquals(methodWithNullableMapShortReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableMapShortHandler(boolean notNull, Handler<@Nullable Map<String, Short>> handler) {
    handler.handle(methodWithNullableMapShortReturn(notNull));
  }

  @Override
  public void methodWithNullableMapShortHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Short>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableMapShortReturn(notNull)));
  }

  @Override
  public @Nullable Map<String, Short> methodWithNullableMapShortReturn(boolean notNull) {
    if (notNull) {
      return map((short) 1, (short) 2, (short) 3);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableMapIntegerParam(Map<String, Integer> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableMapIntegerParam(boolean expectNull, Map<String, Integer> param) {
    assertEquals(methodWithNullableMapIntegerReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableMapIntegerHandler(boolean notNull, Handler<@Nullable Map<String, Integer>> handler) {
    handler.handle(methodWithNullableMapIntegerReturn(notNull));
  }

  @Override
  public void methodWithNullableMapIntegerHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Integer>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableMapIntegerReturn(notNull)));
  }

  @Override
  public @Nullable Map<String, Integer> methodWithNullableMapIntegerReturn(boolean notNull) {
    if (notNull) {
      return map(1, 2, 3);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableMapLongParam(Map<String, Long> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableMapLongParam(boolean expectNull, Map<String, Long> param) {
    assertEquals(methodWithNullableMapLongReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableMapLongHandler(boolean notNull, Handler<@Nullable Map<String, Long>> handler) {
    handler.handle(methodWithNullableMapLongReturn(notNull));
  }

  @Override
  public void methodWithNullableMapLongHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Long>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableMapLongReturn(notNull)));
  }

  @Override
  public @Nullable Map<String, Long> methodWithNullableMapLongReturn(boolean notNull) {
    if (notNull) {
      return map(1L, 2L, 3L);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableMapFloatParam(Map<String, Float> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableMapFloatParam(boolean expectNull, Map<String, Float> param) {
    assertEquals(methodWithNullableMapFloatReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableMapFloatHandler(boolean notNull, Handler<@Nullable Map<String, Float>> handler) {
    handler.handle(methodWithNullableMapFloatReturn(notNull));
  }

  @Override
  public void methodWithNullableMapFloatHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Float>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableMapFloatReturn(notNull)));
  }

  @Override
  public @Nullable Map<String, Float> methodWithNullableMapFloatReturn(boolean notNull) {
    if (notNull) {
      return map(1.1f, 2.2f, 3.3f);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableMapDoubleParam(Map<String, Double> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableMapDoubleParam(boolean expectNull, Map<String, Double> param) {
    assertEquals(methodWithNullableMapDoubleReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableMapDoubleHandler(boolean notNull, Handler<@Nullable Map<String, Double>> handler) {
    handler.handle(methodWithNullableMapDoubleReturn(notNull));
  }

  @Override
  public void methodWithNullableMapDoubleHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Double>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableMapDoubleReturn(notNull)));
  }

  @Override
  public @Nullable Map<String, Double> methodWithNullableMapDoubleReturn(boolean notNull) {
    if (notNull) {
      return map(1.11d, 2.22d, 3.33d);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableMapBooleanParam(Map<String, Boolean> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableMapBooleanParam(boolean expectNull, Map<String, Boolean> param) {
    assertEquals(methodWithNullableMapBooleanReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableMapBooleanHandler(boolean notNull, Handler<@Nullable Map<String, Boolean>> handler) {
    handler.handle(methodWithNullableMapBooleanReturn(notNull));
  }

  @Override
  public void methodWithNullableMapBooleanHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Boolean>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableMapBooleanReturn(notNull)));
  }

  @Override
  public @Nullable Map<String, Boolean> methodWithNullableMapBooleanReturn(boolean notNull) {
    if (notNull) {
      return map(true, false, true);
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableMapStringParam(Map<String, String> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableMapStringParam(boolean expectNull, Map<String, String> param) {
    assertEquals(methodWithNullableMapStringReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableMapStringHandler(boolean notNull, Handler<@Nullable Map<String, String>> handler) {
    handler.handle(methodWithNullableMapStringReturn(notNull));
  }

  @Override
  public void methodWithNullableMapStringHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, String>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableMapStringReturn(notNull)));
  }

  @Override
  public @Nullable Map<String, String> methodWithNullableMapStringReturn(boolean notNull) {
    if (notNull) {
      return map("first", "second", "third");
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableMapCharParam(Map<String, Character> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableMapCharParam(boolean expectNull, Map<String, Character> param) {
    assertEquals(methodWithNullableMapCharReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableMapCharHandler(boolean notNull, Handler<@Nullable Map<String, Character>> handler) {
    handler.handle(methodWithNullableMapCharReturn(notNull));
  }

  @Override
  public void methodWithNullableMapCharHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, Character>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableMapCharReturn(notNull)));
  }

  @Override
  public @Nullable Map<String, Character> methodWithNullableMapCharReturn(boolean notNull) {
    if (notNull) {
      return map('x', 'y', 'z');
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableMapJsonObjectParam(Map<String, JsonObject> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableMapJsonObjectParam(boolean expectNull, Map<String, JsonObject> param) {
    assertEquals(methodWithNullableMapJsonObjectReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableMapJsonObjectHandler(boolean notNull, Handler<@Nullable Map<String, JsonObject>> handler) {
    handler.handle(methodWithNullableMapJsonObjectReturn(notNull));
  }

  @Override
  public void methodWithNullableMapJsonObjectHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, JsonObject>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableMapJsonObjectReturn(notNull)));
  }

  @Override
  public @Nullable Map<String, JsonObject> methodWithNullableMapJsonObjectReturn(boolean notNull) {
    if (notNull) {
      return map(new JsonObject().put("foo", "bar"), new JsonObject().put("juu", 3));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableMapJsonArrayParam(Map<String, JsonArray> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableMapJsonArrayParam(boolean expectNull, Map<String, JsonArray> param) {
    assertEquals(methodWithNullableMapJsonArrayReturn(!expectNull), param);
  }

  @Override
  public void methodWithNullableMapJsonArrayHandler(boolean notNull, Handler<@Nullable Map<String, JsonArray>> handler) {
    handler.handle(methodWithNullableMapJsonArrayReturn(notNull));
  }

  @Override
  public void methodWithNullableMapJsonArrayHandlerAsyncResult(boolean notNull, Handler<AsyncResult<@Nullable Map<String, JsonArray>>> handler) {
    handler.handle(Future.succeededFuture(methodWithNullableMapJsonArrayReturn(notNull)));
  }

  @Override
  public @Nullable Map<String, JsonArray> methodWithNullableMapJsonArrayReturn(boolean notNull) {
    if (notNull) {
      return map(new JsonArray().add("foo").add("bar"), new JsonArray().add("juu"));
    } else {
      return null;
    }
  }

  @Override
  public boolean methodWithNonNullableMapApiParam(Map<String, RefedInterface1> param) {
    return param == null;
  }

  @Override
  public void methodWithNullableMapApiParam(boolean expectNull, Map<String, RefedInterface1> param) {
    assertEquals(methodWithNullableMapApiReturn(!expectNull), param);
  }

  private Map<String, RefedInterface1> methodWithNullableMapApiReturn(boolean notNull) {
    return notNull ? map(new RefedInterface1Impl().setString("refed_is_here")) : null;
  }

  @Override
  public void methodWithListNullableByteParam(List<@Nullable Byte> param) {
    assertEquals(param, methodWithListNullableByteReturn());
  }

  @Override
  public void methodWithListNullableByteHandler(Handler<List<@Nullable Byte>> handler) {
    handler.handle(methodWithListNullableByteReturn());
  }

  @Override
  public void methodWithListNullableByteHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Byte>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableByteReturn()));
  }

  @Override
  public List<@Nullable Byte> methodWithListNullableByteReturn() {
    ArrayList<Byte> ret = new ArrayList<>();
    ret.add((byte)12);
    ret.add(null);
    ret.add((byte) 24);
    return ret;
  }

  @Override
  public void methodWithListNullableShortParam(List<@Nullable Short> param) {
    assertEquals(param, methodWithListNullableShortReturn());
  }

  @Override
  public void methodWithListNullableShortHandler(Handler<List<@Nullable Short>> handler) {
    handler.handle(methodWithListNullableShortReturn());
  }

  @Override
  public void methodWithListNullableShortHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Short>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableShortReturn()));
  }

  @Override
  public List<@Nullable Short> methodWithListNullableShortReturn() {
    ArrayList<Short> ret = new ArrayList<>();
    ret.add((short)520);
    ret.add(null);
    ret.add((short) 1040);
    return ret;
  }

  @Override
  public void methodWithListNullableIntegerParam(List<@Nullable Integer> param) {
    assertEquals(param, methodWithListNullableIntegerReturn());
  }

  @Override
  public void methodWithListNullableIntegerHandler(Handler<List<@Nullable Integer>> handler) {
    handler.handle(methodWithListNullableIntegerReturn());
  }

  @Override
  public void methodWithListNullableIntegerHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Integer>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableIntegerReturn()));
  }

  @Override
  public List<@Nullable Integer> methodWithListNullableIntegerReturn() {
    ArrayList<Integer> ret = new ArrayList<>();
    ret.add(12345);
    ret.add(null);
    ret.add(54321);
    return ret;
  }

  @Override
  public void methodWithListNullableLongParam(List<@Nullable Long> param) {
    assertEquals(param, methodWithListNullableLongReturn());
  }

  @Override
  public void methodWithListNullableLongHandler(Handler<List<@Nullable Long>> handler) {
    handler.handle(methodWithListNullableLongReturn());
  }

  @Override
  public void methodWithListNullableLongHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Long>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableLongReturn()));
  }

  @Override
  public List<@Nullable Long> methodWithListNullableLongReturn() {
    ArrayList<Long> ret = new ArrayList<>();
    ret.add(123456789L);
    ret.add(null);
    ret.add(987654321L);
    return ret;
  }

  @Override
  public void methodWithListNullableFloatParam(List<@Nullable Float> param) {
    assertEquals(param, methodWithListNullableFloatReturn());
  }

  @Override
  public void methodWithListNullableFloatHandler(Handler<List<@Nullable Float>> handler) {
    handler.handle(methodWithListNullableFloatReturn());
  }

  @Override
  public void methodWithListNullableFloatHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Float>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableFloatReturn()));
  }

  @Override
  public List<@Nullable Float> methodWithListNullableFloatReturn() {
    ArrayList<Float> ret = new ArrayList<>();
    ret.add(1.1f);
    ret.add(null);
    ret.add(3.3f);
    return ret;
  }

  @Override
  public void methodWithListNullableDoubleParam(List<@Nullable Double> param) {
    assertEquals(param, methodWithListNullableDoubleReturn());
  }

  @Override
  public void methodWithListNullableDoubleHandler(Handler<List<@Nullable Double>> handler) {
    handler.handle(methodWithListNullableDoubleReturn());
  }

  @Override
  public void methodWithListNullableDoubleHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Double>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableDoubleReturn()));
  }

  @Override
  public List<@Nullable Double> methodWithListNullableDoubleReturn() {
    ArrayList<Double> ret = new ArrayList<>();
    ret.add(1.11);
    ret.add(null);
    ret.add(3.33);
    return ret;
  }

  @Override
  public void methodWithListNullableBooleanParam(List<@Nullable Boolean> param) {
    assertEquals(param, methodWithListNullableBooleanReturn());
  }

  @Override
  public void methodWithListNullableBooleanHandler(Handler<List<@Nullable Boolean>> handler) {
    handler.handle(methodWithListNullableBooleanReturn());
  }

  @Override
  public void methodWithListNullableBooleanHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Boolean>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableBooleanReturn()));
  }

  @Override
  public List<@Nullable Boolean> methodWithListNullableBooleanReturn() {
    ArrayList<Boolean> ret = new ArrayList<>();
    ret.add(true);
    ret.add(null);
    ret.add(false);
    return ret;
  }

  @Override
  public void methodWithListNullableCharParam(List<@Nullable Character> param) {
    assertEquals(param, methodWithListNullableCharReturn());
  }

  @Override
  public void methodWithListNullableCharHandler(Handler<List<@Nullable Character>> handler) {
    handler.handle(methodWithListNullableCharReturn());
  }

  @Override
  public void methodWithListNullableCharHandlerAsyncResult(Handler<AsyncResult<List<@Nullable Character>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableCharReturn()));
  }

  @Override
  public List<@Nullable Character> methodWithListNullableCharReturn() {
    ArrayList<Character> ret = new ArrayList<>();
    ret.add('F');
    ret.add(null);
    ret.add('R');
    return ret;
  }

  @Override
  public void methodWithListNullableStringParam(List<@Nullable String> param) {
    assertEquals(param, methodWithListNullableStringReturn());
  }

  @Override
  public void methodWithListNullableStringHandler(Handler<List<@Nullable String>> handler) {
    handler.handle(methodWithListNullableStringReturn());
  }

  @Override
  public void methodWithListNullableStringHandlerAsyncResult(Handler<AsyncResult<List<@Nullable String>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableStringReturn()));
  }

  @Override
  public List<@Nullable String> methodWithListNullableStringReturn() {
    ArrayList<String> ret = new ArrayList<>();
    ret.add("first");
    ret.add(null);
    ret.add("third");
    return ret;
  }


  @Override
  public void methodWithListNullableJsonObjectParam(List<@Nullable JsonObject> param) {
    assertEquals(param, methodWithListNullableJsonObjectReturn());
  }

  @Override
  public void methodWithListNullableJsonObjectHandler(Handler<List<@Nullable JsonObject>> handler) {
    handler.handle(methodWithListNullableJsonObjectReturn());
  }

  @Override
  public void methodWithListNullableJsonObjectHandlerAsyncResult(Handler<AsyncResult<List<@Nullable JsonObject>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableJsonObjectReturn()));
  }

  @Override
  public List<@Nullable JsonObject> methodWithListNullableJsonObjectReturn() {
    ArrayList<JsonObject> ret = new ArrayList<>();
    ret.add(new JsonObject().put("foo", "bar"));
    ret.add(null);
    ret.add(new JsonObject().put("juu", 3));
    return ret;
  }

  @Override
  public void methodWithListNullableJsonArrayParam(List<@Nullable JsonArray> param) {
    assertEquals(param, methodWithListNullableJsonArrayReturn());
  }

  @Override
  public void methodWithListNullableJsonArrayHandler(Handler<List<@Nullable JsonArray>> handler) {
    handler.handle(methodWithListNullableJsonArrayReturn());
  }

  @Override
  public void methodWithListNullableJsonArrayHandlerAsyncResult(Handler<AsyncResult<List<@Nullable JsonArray>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableJsonArrayReturn()));
  }

  @Override
  public List<@Nullable JsonArray> methodWithListNullableJsonArrayReturn() {
    ArrayList<JsonArray> ret = new ArrayList<>();
    ret.add(new JsonArray().add("foo").add("bar"));
    ret.add(null);
    ret.add(new JsonArray().add("juu"));
    return ret;
  }

  @Override
  public void methodWithListNullableApiParam(List<@Nullable RefedInterface1> param) {
    assertEquals(param, methodWithListNullableApiReturn());
  }

  @Override
  public void methodWithListNullableApiHandler(Handler<List<@Nullable RefedInterface1>> handler) {
    handler.handle(methodWithListNullableApiReturn());
  }

  @Override
  public void methodWithListNullableApiHandlerAsyncResult(Handler<AsyncResult<List<@Nullable RefedInterface1>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableApiReturn()));
  }

  @Override
  public List<@Nullable RefedInterface1> methodWithListNullableApiReturn() {
    ArrayList<RefedInterface1> ret = new ArrayList<>();
    ret.add(new RefedInterface1Impl().setString("first"));
    ret.add(null);
    ret.add(new RefedInterface1Impl().setString("third"));
    return ret;
  }

  @Override
  public void methodWithListNullableDataObjectParam(List<@Nullable TestDataObject> param) {
    Function<@Nullable TestDataObject, JsonObject> conv = obj -> (obj != null) ? obj.toJson() : null;
    assertEquals(param.stream().map(conv).collect(Collectors.toList()), methodWithListNullableDataObjectReturn().stream().map(conv).collect(Collectors.toList()));
  }

  @Override
  public void methodWithListNullableDataObjectHandler(Handler<List<@Nullable TestDataObject>> handler) {
    handler.handle(methodWithListNullableDataObjectReturn());
  }

  @Override
  public void methodWithListNullableDataObjectHandlerAsyncResult(Handler<AsyncResult<List<@Nullable TestDataObject>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableDataObjectReturn()));
  }

  @Override
  public List<@Nullable TestDataObject> methodWithListNullableDataObjectReturn() {
    ArrayList<TestDataObject> ret = new ArrayList<>();
    ret.add(new TestDataObject().setFoo("first").setBar(1).setWibble(1.1));
    ret.add(null);
    ret.add(new TestDataObject().setFoo("third").setBar(3).setWibble(3.3));
    return ret;
  }

  @Override
  public void methodWithListNullableEnumParam(List<@Nullable TestEnum> param) {
    assertEquals(param, methodWithListNullableEnumReturn());
  }

  @Override
  public void methodWithListNullableEnumHandler(Handler<List<@Nullable TestEnum>> handler) {
    handler.handle(methodWithListNullableEnumReturn());
  }

  @Override
  public void methodWithListNullableEnumHandlerAsyncResult(Handler<AsyncResult<List<@Nullable TestEnum>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableEnumReturn()));
  }

  @Override
  public List<@Nullable TestEnum> methodWithListNullableEnumReturn() {
    ArrayList<TestEnum> ret = new ArrayList<>();
    ret.add(TestEnum.TIM);
    ret.add(null);
    ret.add(TestEnum.JULIEN);
    return ret;
  }

  @Override
  public void methodWithListNullableGenEnumParam(List<@Nullable TestGenEnum> param) {
    assertEquals(param, methodWithListNullableGenEnumReturn());
  }

  @Override
  public void methodWithListNullableGenEnumHandler(Handler<List<@Nullable TestGenEnum>> handler) {
    handler.handle(methodWithListNullableGenEnumReturn());
  }

  @Override
  public void methodWithListNullableGenEnumHandlerAsyncResult(Handler<AsyncResult<List<@Nullable TestGenEnum>>> handler) {
    handler.handle(Future.succeededFuture(methodWithListNullableGenEnumReturn()));
  }

  @Override
  public List<@Nullable TestGenEnum> methodWithListNullableGenEnumReturn() {
    ArrayList<TestGenEnum> ret = new ArrayList<>();
    ret.add(TestGenEnum.BOB);
    ret.add(null);
    ret.add(TestGenEnum.LELAND);
    return ret;
  }

  @Override
  public void methodWithSetNullableByteParam(Set<@Nullable Byte> param) {
    assertEquals(param, methodWithSetNullableByteReturn());
  }

  @Override
  public void methodWithSetNullableByteHandler(Handler<Set<@Nullable Byte>> handler) {
    handler.handle(methodWithSetNullableByteReturn());
  }

  @Override
  public void methodWithSetNullableByteHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Byte>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableByteReturn()));
  }

  @Override
  public Set<@Nullable Byte> methodWithSetNullableByteReturn() {
    LinkedHashSet<Byte> ret = new LinkedHashSet<>();
    ret.add((byte)12);
    ret.add(null);
    ret.add((byte) 24);
    return ret;
  }

  @Override
  public void methodWithSetNullableShortParam(Set<@Nullable Short> param) {
    assertEquals(param, methodWithSetNullableShortReturn());
  }

  @Override
  public void methodWithSetNullableShortHandler(Handler<Set<@Nullable Short>> handler) {
    handler.handle(methodWithSetNullableShortReturn());
  }

  @Override
  public void methodWithSetNullableShortHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Short>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableShortReturn()));
  }

  @Override
  public Set<@Nullable Short> methodWithSetNullableShortReturn() {
    LinkedHashSet<Short> ret = new LinkedHashSet<>();
    ret.add((short)520);
    ret.add(null);
    ret.add((short) 1040);
    return ret;
  }

  @Override
  public void methodWithSetNullableIntegerParam(Set<@Nullable Integer> param) {
    assertEquals(param, methodWithSetNullableIntegerReturn());
  }

  @Override
  public void methodWithSetNullableIntegerHandler(Handler<Set<@Nullable Integer>> handler) {
    handler.handle(methodWithSetNullableIntegerReturn());
  }

  @Override
  public void methodWithSetNullableIntegerHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Integer>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableIntegerReturn()));
  }

  @Override
  public Set<@Nullable Integer> methodWithSetNullableIntegerReturn() {
    LinkedHashSet<Integer> ret = new LinkedHashSet<>();
    ret.add(12345);
    ret.add(null);
    ret.add(54321);
    return ret;
  }

  @Override
  public void methodWithSetNullableLongParam(Set<@Nullable Long> param) {
    assertEquals(param, methodWithSetNullableLongReturn());
  }

  @Override
  public void methodWithSetNullableLongHandler(Handler<Set<@Nullable Long>> handler) {
    handler.handle(methodWithSetNullableLongReturn());
  }

  @Override
  public void methodWithSetNullableLongHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Long>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableLongReturn()));
  }

  @Override
  public Set<@Nullable Long> methodWithSetNullableLongReturn() {
    LinkedHashSet<Long> ret = new LinkedHashSet<>();
    ret.add(123456789L);
    ret.add(null);
    ret.add(987654321L);
    return ret;
  }

  @Override
  public void methodWithSetNullableFloatParam(Set<@Nullable Float> param) {
    assertEquals(param, methodWithSetNullableFloatReturn());
  }

  @Override
  public void methodWithSetNullableFloatHandler(Handler<Set<@Nullable Float>> handler) {
    handler.handle(methodWithSetNullableFloatReturn());
  }

  @Override
  public void methodWithSetNullableFloatHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Float>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableFloatReturn()));
  }

  @Override
  public Set<@Nullable Float> methodWithSetNullableFloatReturn() {
    LinkedHashSet<Float> ret = new LinkedHashSet<>();
    ret.add(1.1f);
    ret.add(null);
    ret.add(3.3f);
    return ret;
  }

  @Override
  public void methodWithSetNullableDoubleParam(Set<@Nullable Double> param) {
    assertEquals(param, methodWithSetNullableDoubleReturn());
  }

  @Override
  public void methodWithSetNullableDoubleHandler(Handler<Set<@Nullable Double>> handler) {
    handler.handle(methodWithSetNullableDoubleReturn());
  }

  @Override
  public void methodWithSetNullableDoubleHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Double>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableDoubleReturn()));
  }

  @Override
  public Set<@Nullable Double> methodWithSetNullableDoubleReturn() {
    LinkedHashSet<Double> ret = new LinkedHashSet<>();
    ret.add(1.11);
    ret.add(null);
    ret.add(3.33);
    return ret;
  }

  @Override
  public void methodWithSetNullableBooleanParam(Set<@Nullable Boolean> param) {
    assertEquals(param, methodWithSetNullableBooleanReturn());
  }

  @Override
  public void methodWithSetNullableBooleanHandler(Handler<Set<@Nullable Boolean>> handler) {
    handler.handle(methodWithSetNullableBooleanReturn());
  }

  @Override
  public void methodWithSetNullableBooleanHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Boolean>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableBooleanReturn()));
  }

  @Override
  public Set<@Nullable Boolean> methodWithSetNullableBooleanReturn() {
    LinkedHashSet<Boolean> ret = new LinkedHashSet<>();
    ret.add(true);
    ret.add(null);
    ret.add(false);
    return ret;
  }

  @Override
  public void methodWithSetNullableCharParam(Set<@Nullable Character> param) {
    assertEquals(param, methodWithSetNullableCharReturn());
  }

  @Override
  public void methodWithSetNullableCharHandler(Handler<Set<@Nullable Character>> handler) {
    handler.handle(methodWithSetNullableCharReturn());
  }

  @Override
  public void methodWithSetNullableCharHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable Character>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableCharReturn()));
  }

  @Override
  public Set<@Nullable Character> methodWithSetNullableCharReturn() {
    LinkedHashSet<Character> ret = new LinkedHashSet<>();
    ret.add('F');
    ret.add(null);
    ret.add('R');
    return ret;
  }

  @Override
  public void methodWithSetNullableStringParam(Set<@Nullable String> param) {
    assertEquals(param, methodWithSetNullableStringReturn());
  }

  @Override
  public void methodWithSetNullableStringHandler(Handler<Set<@Nullable String>> handler) {
    handler.handle(methodWithSetNullableStringReturn());
  }

  @Override
  public void methodWithSetNullableStringHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable String>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableStringReturn()));
  }

  @Override
  public Set<@Nullable String> methodWithSetNullableStringReturn() {
    LinkedHashSet<String> ret = new LinkedHashSet<>();
    ret.add("first");
    ret.add(null);
    ret.add("third");
    return ret;
  }


  @Override
  public void methodWithSetNullableJsonObjectParam(Set<@Nullable JsonObject> param) {
    assertEquals(param, methodWithSetNullableJsonObjectReturn());
  }

  @Override
  public void methodWithSetNullableJsonObjectHandler(Handler<Set<@Nullable JsonObject>> handler) {
    handler.handle(methodWithSetNullableJsonObjectReturn());
  }

  @Override
  public void methodWithSetNullableJsonObjectHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable JsonObject>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableJsonObjectReturn()));
  }

  @Override
  public Set<@Nullable JsonObject> methodWithSetNullableJsonObjectReturn() {
    LinkedHashSet<JsonObject> ret = new LinkedHashSet<>();
    ret.add(new JsonObject().put("foo", "bar"));
    ret.add(null);
    ret.add(new JsonObject().put("juu", 3));
    return ret;
  }

  @Override
  public void methodWithSetNullableJsonArrayParam(Set<@Nullable JsonArray> param) {
    assertEquals(param, methodWithSetNullableJsonArrayReturn());
  }

  @Override
  public void methodWithSetNullableJsonArrayHandler(Handler<Set<@Nullable JsonArray>> handler) {
    handler.handle(methodWithSetNullableJsonArrayReturn());
  }

  @Override
  public void methodWithSetNullableJsonArrayHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable JsonArray>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableJsonArrayReturn()));
  }

  @Override
  public Set<@Nullable JsonArray> methodWithSetNullableJsonArrayReturn() {
    LinkedHashSet<JsonArray> ret = new LinkedHashSet<>();
    ret.add(new JsonArray().add("foo").add("bar"));
    ret.add(null);
    ret.add(new JsonArray().add("juu"));
    return ret;
  }

  @Override
  public void methodWithSetNullableApiParam(Set<@Nullable RefedInterface1> param) {
    assertEquals(param, methodWithSetNullableApiReturn());
  }

  @Override
  public void methodWithSetNullableApiHandler(Handler<Set<@Nullable RefedInterface1>> handler) {
    handler.handle(methodWithSetNullableApiReturn());
  }

  @Override
  public void methodWithSetNullableApiHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable RefedInterface1>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableApiReturn()));
  }

  @Override
  public Set<@Nullable RefedInterface1> methodWithSetNullableApiReturn() {
    LinkedHashSet<RefedInterface1> ret = new LinkedHashSet<>();
    ret.add(new RefedInterface1Impl().setString("first"));
    ret.add(null);
    ret.add(new RefedInterface1Impl().setString("third"));
    return ret;
  }

  @Override
  public void methodWithSetNullableDataObjectParam(Set<@Nullable TestDataObject> param) {
    Function<@Nullable TestDataObject, JsonObject> conv = obj -> (obj != null) ? obj.toJson() : null;
    assertEquals(param.stream().map(conv).collect(Collectors.toSet()), methodWithSetNullableDataObjectReturn().stream().map(conv).collect(Collectors.toSet()));
  }

  @Override
  public void methodWithSetNullableDataObjectHandler(Handler<Set<@Nullable TestDataObject>> handler) {
    handler.handle(methodWithSetNullableDataObjectReturn());
  }

  @Override
  public void methodWithSetNullableDataObjectHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable TestDataObject>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableDataObjectReturn()));
  }

  @Override
  public Set<@Nullable TestDataObject> methodWithSetNullableDataObjectReturn() {
    LinkedHashSet<TestDataObject> ret = new LinkedHashSet<>();
    ret.add(new TestDataObject().setFoo("first").setBar(1).setWibble(1.1));
    ret.add(null);
    ret.add(new TestDataObject().setFoo("third").setBar(3).setWibble(3.3));
    return ret;
  }

  @Override
  public void methodWithSetNullableEnumParam(Set<@Nullable TestEnum> param) {
    assertEquals(param, methodWithSetNullableEnumReturn());
  }

  @Override
  public void methodWithSetNullableEnumHandler(Handler<Set<@Nullable TestEnum>> handler) {
    handler.handle(methodWithSetNullableEnumReturn());
  }

  @Override
  public void methodWithSetNullableEnumHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable TestEnum>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableEnumReturn()));
  }

  @Override
  public Set<@Nullable TestEnum> methodWithSetNullableEnumReturn() {
    LinkedHashSet<TestEnum> ret = new LinkedHashSet<>();
    ret.add(TestEnum.TIM);
    ret.add(null);
    ret.add(TestEnum.JULIEN);
    return ret;
  }

  @Override
  public void methodWithSetNullableGenEnumParam(Set<@Nullable TestGenEnum> param) {
    assertEquals(param, methodWithSetNullableGenEnumReturn());
  }

  @Override
  public void methodWithSetNullableGenEnumHandler(Handler<Set<@Nullable TestGenEnum>> handler) {
    handler.handle(methodWithSetNullableGenEnumReturn());
  }

  @Override
  public void methodWithSetNullableGenEnumHandlerAsyncResult(Handler<AsyncResult<Set<@Nullable TestGenEnum>>> handler) {
    handler.handle(Future.succeededFuture(methodWithSetNullableGenEnumReturn()));
  }

  @Override
  public Set<@Nullable TestGenEnum> methodWithSetNullableGenEnumReturn() {
    LinkedHashSet<TestGenEnum> ret = new LinkedHashSet<>();
    ret.add(TestGenEnum.BOB);
    ret.add(null);
    ret.add(TestGenEnum.LELAND);
    return ret;
  }

  @Override
  public void methodWithMapNullableByteParam(Map<String, @Nullable Byte> param) {
    assertEquals(new HashMap<>(param), methodWithMapNullableByteReturn());
  }

  @Override
  public void methodWithMapNullableByteHandler(Handler<Map<String, @Nullable Byte>> handler) {
    handler.handle(methodWithMapNullableByteReturn());
  }

  @Override
  public void methodWithMapNullableByteHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Byte>>> handler) {
    handler.handle(Future.succeededFuture(methodWithMapNullableByteReturn()));
  }

  @Override
  public Map<String, @Nullable Byte> methodWithMapNullableByteReturn() {
    return map((byte)12, null, (byte) 24);
  }

  @Override
  public void methodWithMapNullableShortParam(Map<String, @Nullable Short> param) {
    assertEquals(new HashMap<>(param), methodWithMapNullableShortReturn());
  }

  @Override
  public void methodWithMapNullableShortHandler(Handler<Map<String, @Nullable Short>> handler) {
    handler.handle(methodWithMapNullableShortReturn());
  }

  @Override
  public void methodWithMapNullableShortHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Short>>> handler) {
    handler.handle(Future.succeededFuture(methodWithMapNullableShortReturn()));
  }

  @Override
  public Map<String, @Nullable Short> methodWithMapNullableShortReturn() {
    return map((short)520, null, (short) 1040);
  }

  @Override
  public void methodWithMapNullableIntegerParam(Map<String, @Nullable Integer> param) {
    assertEquals(new HashMap<>(param), methodWithMapNullableIntegerReturn());
  }

  @Override
  public void methodWithMapNullableIntegerHandler(Handler<Map<String, @Nullable Integer>> handler) {
    handler.handle(methodWithMapNullableIntegerReturn());
  }

  @Override
  public void methodWithMapNullableIntegerHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Integer>>> handler) {
    handler.handle(Future.succeededFuture(methodWithMapNullableIntegerReturn()));
  }

  @Override
  public Map<String, @Nullable Integer> methodWithMapNullableIntegerReturn() {
    return map(12345,null,54321);
  }

  @Override
  public void methodWithMapNullableLongParam(Map<String, @Nullable Long> param) {
    assertEquals(new HashMap<>(param), methodWithMapNullableLongReturn());
  }

  @Override
  public void methodWithMapNullableLongHandler(Handler<Map<String, @Nullable Long>> handler) {
    handler.handle(methodWithMapNullableLongReturn());
  }

  @Override
  public void methodWithMapNullableLongHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Long>>> handler) {
    handler.handle(Future.succeededFuture(methodWithMapNullableLongReturn()));
  }

  @Override
  public Map<String, @Nullable Long> methodWithMapNullableLongReturn() {
    return map(123456789L,null,987654321L);
  }

  @Override
  public void methodWithMapNullableFloatParam(Map<String, @Nullable Float> param) {
    assertEquals(new HashMap<>(param), methodWithMapNullableFloatReturn());
  }

  @Override
  public void methodWithMapNullableFloatHandler(Handler<Map<String, @Nullable Float>> handler) {
    handler.handle(methodWithMapNullableFloatReturn());
  }

  @Override
  public void methodWithMapNullableFloatHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Float>>> handler) {
    handler.handle(Future.succeededFuture(methodWithMapNullableFloatReturn()));
  }

  @Override
  public Map<String, @Nullable Float> methodWithMapNullableFloatReturn() {
    return map(1.1f, null,3.3f);
  }

  @Override
  public void methodWithMapNullableDoubleParam(Map<String, @Nullable Double> param) {
    assertEquals(new HashMap<>(param), methodWithMapNullableDoubleReturn());
  }

  @Override
  public void methodWithMapNullableDoubleHandler(Handler<Map<String, @Nullable Double>> handler) {
    handler.handle(methodWithMapNullableDoubleReturn());
  }

  @Override
  public void methodWithMapNullableDoubleHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Double>>> handler) {
    handler.handle(Future.succeededFuture(methodWithMapNullableDoubleReturn()));
  }

  @Override
  public Map<String, @Nullable Double> methodWithMapNullableDoubleReturn() {
    return map(1.11, null,3.33);
  }

  @Override
  public void methodWithMapNullableBooleanParam(Map<String, @Nullable Boolean> param) {
    assertEquals(new HashMap<>(param), methodWithMapNullableBooleanReturn());
  }

  @Override
  public void methodWithMapNullableBooleanHandler(Handler<Map<String, @Nullable Boolean>> handler) {
    handler.handle(methodWithMapNullableBooleanReturn());
  }

  @Override
  public void methodWithMapNullableBooleanHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Boolean>>> handler) {
    handler.handle(Future.succeededFuture(methodWithMapNullableBooleanReturn()));
  }

  @Override
  public Map<String, @Nullable Boolean> methodWithMapNullableBooleanReturn() {
    return map(true,null,false);
  }

  @Override
  public void methodWithMapNullableCharParam(Map<String, @Nullable Character> param) {
    assertEquals(new HashMap<>(param), methodWithMapNullableCharReturn());
  }

  @Override
  public void methodWithMapNullableCharHandler(Handler<Map<String, @Nullable Character>> handler) {
    handler.handle(methodWithMapNullableCharReturn());
  }

  @Override
  public void methodWithMapNullableCharHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable Character>>> handler) {
    handler.handle(Future.succeededFuture(methodWithMapNullableCharReturn()));
  }

  @Override
  public Map<String, @Nullable Character> methodWithMapNullableCharReturn() {
    return map('F', null,'R');
  }

  @Override
  public void methodWithMapNullableStringParam(Map<String, @Nullable String> param) {
    assertEquals(new HashMap<>(param), methodWithMapNullableStringReturn());
  }

  @Override
  public void methodWithMapNullableStringHandler(Handler<Map<String, @Nullable String>> handler) {
    handler.handle(methodWithMapNullableStringReturn());
  }

  @Override
  public void methodWithMapNullableStringHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable String>>> handler) {
    handler.handle(Future.succeededFuture(methodWithMapNullableStringReturn()));
  }

  @Override
  public Map<String, @Nullable String> methodWithMapNullableStringReturn() {
    return map("first", null, "third");
  }


  @Override
  public void methodWithMapNullableJsonObjectParam(Map<String, @Nullable JsonObject> param) {
    assertEquals(new HashMap<>(param), methodWithMapNullableJsonObjectReturn());
  }

  @Override
  public void methodWithMapNullableJsonObjectHandler(Handler<Map<String, @Nullable JsonObject>> handler) {
    handler.handle(methodWithMapNullableJsonObjectReturn());
  }

  @Override
  public void methodWithMapNullableJsonObjectHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable JsonObject>>> handler) {
    handler.handle(Future.succeededFuture(methodWithMapNullableJsonObjectReturn()));
  }

  @Override
  public Map<String, @Nullable JsonObject> methodWithMapNullableJsonObjectReturn() {
    return map(new JsonObject().put("foo", "bar"),null,new JsonObject().put("juu", 3));
  }

  @Override
  public void methodWithMapNullableJsonArrayParam(Map<String, @Nullable JsonArray> param) {
    assertEquals(new HashMap<>(param), methodWithMapNullableJsonArrayReturn());
  }

  @Override
  public void methodWithMapNullableJsonArrayHandler(Handler<Map<String, @Nullable JsonArray>> handler) {
    handler.handle(methodWithMapNullableJsonArrayReturn());
  }

  @Override
  public void methodWithMapNullableJsonArrayHandlerAsyncResult(Handler<AsyncResult<Map<String, @Nullable JsonArray>>> handler) {
    handler.handle(Future.succeededFuture(methodWithMapNullableJsonArrayReturn()));
  }

  @Override
  public Map<String, @Nullable JsonArray> methodWithMapNullableJsonArrayReturn() {
    return map(new JsonArray().add("foo").add("bar"), null, new JsonArray().add("juu"));
  }

  @Override
  public void methodWithMapNullableApiParam(Map<String, @Nullable RefedInterface1> param) {
    assertEquals(new HashMap<>(param), methodWithMapNullableApiReturn());
  }

  private Map<String, @Nullable RefedInterface1> methodWithMapNullableApiReturn() {
    return map(new RefedInterface1Impl().setString("first"), null, new RefedInterface1Impl().setString("third"));
  }

  @Override
  public void methodWithNullableHandler(boolean expectNull, Handler<String> handler) {
    if (expectNull) {
      assertNull(handler);
    } else {
      handler.handle(methodWithNullableStringReturn(true));
    }
  }

  @Override
  public void methodWithNullableHandlerAsyncResult(boolean expectNull, Handler<AsyncResult<String>> handler) {
    if (expectNull) {
      assertNull(handler);
    } else {
      handler.handle(Future.succeededFuture(methodWithNullableStringReturn(true)));
    }
  }

  private static <V> Map<String, V> map(V... vs) {
    Map<String, V> map = new HashMap<>();
    Stream.of(vs).forEach(v -> {
      map.put("" + (1 + map.size()), v);
    });
    return map;
  }
}
