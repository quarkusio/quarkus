package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface FunctionParamTCK {

  List<String> methodWithBasicParam(
      Function<Byte, String> byteFunc,
      Function<Short, String> shortFunc,
      Function<Integer, String> integerFunc,
      Function<Long, String> longFunc,
      Function<Float, String> floatFunc,
      Function<Double, String> doubleFunc,
      Function<Boolean, String> booleanFunc,
      Function<Character, String> charFunc,
      Function<String, String> stringFunc
  );

  List<String> methodWithJsonParam(Function<JsonObject, String> objectFunc, Function<JsonArray, String> arrayFunc);

  String methodWithVoidParam(Function<Void, String> func);
  String methodWithUserTypeParam(RefedInterface1 arg, Function<RefedInterface1, String> func);
  String methodWithObjectParam(Object arg, Function<Object, String> func);
  String methodWithDataObjectParam(Function<TestDataObject, String> func);
  String methodWithEnumParam(Function<TestEnum, String> func);
  String methodWithListParam(Function<List<String>, String> stringFunc);
  String methodWithSetParam(Function<Set<String>, String> func);
  String methodWithMapParam(Function<Map<String, String>, String> func);

  <T> String methodWithGenericParam(T t, Function<T, String> func);
  <T> String methodWithGenericUserTypeParam(T t, Function<GenericRefedInterface<T>, String> func);

  String methodWithBasicReturn(
      Function<String, Byte> byteFunc,
      Function<String, Short> shortFunc,
      Function<String, Integer> integerFunc,
      Function<String, Long> longFunc,
      Function<String, Float> floatFunc,
      Function<String, Double> doubleFunc,
      Function<String, Boolean> booleanFunc,
      Function<String, Character> charFunc,
      Function<String, String> stringFunc
  );

  String methodWithJsonReturn(Function<String, JsonObject> objectFunc, Function<String, JsonArray> arrayFunc);
  String methodWithObjectReturn(Function<Integer, Object> func);
  String methodWithDataObjectReturn(Function<String, TestDataObject> func);
  String methodWithEnumReturn(Function<String, TestEnum> func);
  String methodWithListReturn(Function<String, List<String>> func);
  String methodWithSetReturn(Function<String, Set<String>> func);
  String methodWithMapReturn(Function<String, Map<String, String>> func);
  <T> String methodWithGenericReturn(Function<Integer, T> func);
  <T> String methodWithGenericUserTypeReturn(Function<GenericRefedInterface<T>, GenericRefedInterface<T>> func);

  String methodWithNullableListParam(Function<@Nullable List<String>, String> func);
  String methodWithNullableListReturn(Function<String, @Nullable List<String>> func);

}
