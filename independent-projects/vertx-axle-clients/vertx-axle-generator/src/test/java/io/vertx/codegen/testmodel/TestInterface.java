package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface TestInterface extends SuperInterface1, SuperInterface2 {

  // Test params

  void methodWithBasicParams(byte b, short s, int i, long l, float f, double d, boolean bool, char ch, String str);

  void methodWithBasicBoxedParams(Byte b, Short s, Integer i, Long l, Float f, Double d, Boolean bool, Character ch);

  void methodWithHandlerBasicTypes(Handler<Byte> byteHandler, Handler<Short> shortHandler, Handler<Integer> intHandler,
                                   Handler<Long> longHandler, Handler<Float> floatHandler, Handler<Double> doubleHandler,
                                   Handler<Boolean> booleanHandler, Handler<Character> charHandler, Handler<String> stringHandler);

  Handler<String> methodWithHandlerStringReturn(String expected);
  <T> Handler<T> methodWithHandlerGenericReturn(Handler<T> handler);
  Handler<RefedInterface1> methodWithHandlerVertxGenReturn(String expected);

  void methodWithHandlerAsyncResultByte(boolean sendFailure, Handler<AsyncResult<Byte>> handler);
  void methodWithHandlerAsyncResultShort(boolean sendFailure, Handler<AsyncResult<Short>> handler);
  void methodWithHandlerAsyncResultInteger(boolean sendFailure, Handler<AsyncResult<Integer>> handler);
  void methodWithHandlerAsyncResultLong(boolean sendFailure, Handler<AsyncResult<Long>> handler);
  void methodWithHandlerAsyncResultFloat(boolean sendFailure, Handler<AsyncResult<Float>> handler);
  void methodWithHandlerAsyncResultDouble(boolean sendFailure, Handler<AsyncResult<Double>> handler);
  void methodWithHandlerAsyncResultBoolean(boolean sendFailure, Handler<AsyncResult<Boolean>> handler);
  void methodWithHandlerAsyncResultCharacter(boolean sendFailure, Handler<AsyncResult<Character>> handler);
  void methodWithHandlerAsyncResultString(boolean sendFailure, Handler<AsyncResult<String>> handler);
  void methodWithHandlerAsyncResultDataObject(boolean sendFailure, Handler<AsyncResult<TestDataObject>> handler);

  Handler<AsyncResult<String>> methodWithHandlerAsyncResultStringReturn(String expected, boolean fail);
  <T> Handler<AsyncResult<T>> methodWithHandlerAsyncResultGenericReturn(Handler<AsyncResult<T>> handler);
  Handler<AsyncResult<RefedInterface1>> methodWithHandlerAsyncResultVertxGenReturn(String expected, boolean fail);

  void methodWithUserTypes(RefedInterface1 refed);

  void methodWithObjectParam(String str, Object obj);

  void methodWithDataObjectParam(TestDataObject dataObject);

  void methodWithHandlerUserTypes(Handler<RefedInterface1> handler);

  void methodWithHandlerAsyncResultUserTypes(Handler<AsyncResult<RefedInterface1>> handler);

  void methodWithConcreteHandlerUserTypeSubtype(ConcreteHandlerUserType handler);

  void methodWithAbstractHandlerUserTypeSubtype(AbstractHandlerUserType handler);

  void methodWithConcreteHandlerUserTypeSubtypeExtension(ConcreteHandlerUserTypeExtension handler);

  void methodWithHandlerVoid(Handler<Void> handler);

  void methodWithHandlerAsyncResultVoid(boolean sendFailure, Handler<AsyncResult<Void>> handler);

  void methodWithHandlerThrowable(Handler<Throwable> handler);

  void methodWithHandlerDataObject(Handler<TestDataObject> handler);

  <U> void methodWithHandlerGenericUserType(U value, Handler<GenericRefedInterface<U>> handler);

  <U> void methodWithHandlerAsyncResultGenericUserType(U value, Handler<AsyncResult<GenericRefedInterface<U>>> handler);

  byte methodWithByteReturn();

  short methodWithShortReturn();

  int methodWithIntReturn();

  long methodWithLongReturn();

  float methodWithFloatReturn();

  double methodWithDoubleReturn();

  boolean methodWithBooleanReturn();

  char methodWithCharReturn();

  String methodWithStringReturn();

  RefedInterface1 methodWithVertxGenReturn();

  RefedInterface1 methodWithVertxGenNullReturn();

  RefedInterface2 methodWithAbstractVertxGenReturn();

  TestDataObject methodWithDataObjectReturn();

  TestDataObject methodWithDataObjectNullReturn();

  <U> GenericRefedInterface<U> methodWithGenericUserTypeReturn(U value);

  String overloadedMethod(String str, Handler<String> handler);

  String overloadedMethod(String str, RefedInterface1 refed);

  String overloadedMethod(String str, RefedInterface1 refed, Handler<String> handler);

  String overloadedMethod(String str, RefedInterface1 refed, long period, Handler<String> handler);

  <U> U methodWithGenericReturn(String type);

  <U> void methodWithGenericParam(String type, U u);

  <U> void methodWithGenericHandler(String type, Handler<U> handler);

  <U> void methodWithGenericHandlerAsyncResult(String type, Handler<AsyncResult<U>> asyncResultHandler);

  @Fluent
  TestInterface fluentMethod(String str);

  static RefedInterface1 staticFactoryMethod(String foo) {
    RefedInterface1 refed = new RefedInterface1Impl();
    refed.setString(foo);
    return refed;
  }

  @CacheReturn
  RefedInterface1 methodWithCachedReturn(String foo);

  @CacheReturn
  int methodWithCachedReturnPrimitive(int arg);

  @CacheReturn
  List<RefedInterface1> methodWithCachedListReturn();

  JsonObject methodWithJsonObjectReturn();

  JsonObject methodWithNullJsonObjectReturn();

  JsonObject methodWithComplexJsonObjectReturn();

  JsonArray methodWithJsonArrayReturn();

  JsonArray methodWithNullJsonArrayReturn();

  JsonArray methodWithComplexJsonArrayReturn();

  void methodWithJsonParams(JsonObject jsonObject, JsonArray jsonArray);

  void methodWithNullJsonParams(@Nullable JsonObject jsonObject, @Nullable JsonArray jsonArray);

  void methodWithHandlerJson(Handler<JsonObject> jsonObjectHandler, Handler<JsonArray> jsonArrayHandler);

  void methodWithHandlerComplexJson(Handler<JsonObject> jsonObjectHandler, Handler<JsonArray> jsonArrayHandler);

  void methodWithHandlerAsyncResultJsonObject(Handler<AsyncResult<JsonObject>> handler);

  void methodWithHandlerAsyncResultNullJsonObject(Handler<AsyncResult<@Nullable JsonObject>> handler);

  void methodWithHandlerAsyncResultComplexJsonObject(Handler<AsyncResult<JsonObject>> handler);

  void methodWithHandlerAsyncResultJsonArray(Handler<AsyncResult<JsonArray>> handler);

  void methodWithHandlerAsyncResultNullJsonArray(Handler<AsyncResult<@Nullable JsonArray>> handler);

  void methodWithHandlerAsyncResultComplexJsonArray(Handler<AsyncResult<JsonArray>> handler);

  String methodWithEnumParam(String strVal, TestEnum weirdo);

  TestEnum methodWithEnumReturn(String strVal);

  String methodWithGenEnumParam(String strVal, TestGenEnum weirdo);

  TestGenEnum methodWithGenEnumReturn(String strVal);

  Throwable methodWithThrowableReturn(String strVal);

  String methodWithThrowableParam(Throwable t);

  int superMethodOverloadedBySubclass(String s);

}
