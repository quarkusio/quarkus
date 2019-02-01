package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class FunctionParamTCKImpl implements FunctionParamTCK {

  @Override
  public List<String> methodWithBasicParam(Function<Byte, String> byteFunc, Function<Short, String> shortFunc, Function<Integer, String> integerFunc, Function<Long, String> longFunc, Function<Float, String> floatFunc, Function<Double, String> doubleFunc, Function<Boolean, String> booleanFunc, Function<Character, String> charFunc, Function<String, String> stringFunc) {
    return Arrays.asList(
        byteFunc.apply((byte)100),
        shortFunc.apply((short)1000),
        integerFunc.apply(100000),
        longFunc.apply(10000000000L),
        floatFunc.apply(3.5f),
        doubleFunc.apply(0.01),
        booleanFunc.apply(true),
        charFunc.apply('F'),
        stringFunc.apply("wibble")
    );
  }

  @Override
  public List<String> methodWithJsonParam(Function<JsonObject, String> objectFunc, Function<JsonArray, String> arrayFunc) {
    return Arrays.asList(
        objectFunc.apply(new JsonObject().put("one", 1).put("two", 2).put("three", 3)),
        arrayFunc.apply(new JsonArray().add("one").add("two").add("three"))
    );
  }

  @Override
  public String methodWithDataObjectParam(Function<TestDataObject, String> func) {
    return func.apply(new TestDataObject().setFoo("foo_value").setBar(3).setWibble(0.01));
  }

  @Override
  public String methodWithEnumParam(Function<TestEnum, String> func) {
    return func.apply(TestEnum.TIM);
  }

  @Override
  public String methodWithVoidParam(Function<Void, String> func) {
    return func.apply(null);
  }

  @Override
  public String methodWithUserTypeParam(RefedInterface1 arg, Function<RefedInterface1, String> func) {
    return func.apply(arg);
  }

  @Override
  public String methodWithObjectParam(Object arg, Function<Object, String> func) {
    return func.apply(arg);
  }

  @Override
  public String methodWithListParam(Function<List<String>, String> func) {
    return func.apply(Arrays.asList("one", "two", "three"));
  }

  @Override
  public String methodWithSetParam(Function<Set<String>, String> func) {
    return func.apply(new LinkedHashSet<>(Arrays.asList("one", "two", "three")));
  }

  @Override
  public String methodWithMapParam(Function<Map<String, String>, String> func) {
    return func.apply(Arrays.asList("one", "two", "three").stream().collect(Collectors.toMap(s -> s, s -> s)));
  }

  @Override
  public <T> String methodWithGenericParam(T t, Function<T, String> func) {
    return func.apply(t);
  }

  @Override
  public <T> String methodWithGenericUserTypeParam(T t, Function<GenericRefedInterface<T>, String> func) {
    GenericRefedInterfaceImpl<T> userObj = new GenericRefedInterfaceImpl<>();
    userObj.setValue(t);
    return func.apply(userObj);
  }

  // Return

  @Override
  public String methodWithBasicReturn(Function<String, Byte> byteFunc, Function<String, Short> shortFunc, Function<String, Integer> integerFunc, Function<String, Long> longFunc, Function<String, Float> floatFunc, Function<String, Double> doubleFunc, Function<String, Boolean> booleanFunc, Function<String, Character> charFunc, Function<String, String> stringFunc) {
    assertEquals(10, (int)(byte)byteFunc.apply("whatever"));
    assertEquals(1000, (int)(short)shortFunc.apply("whatever"));
    assertEquals(100000, (int)integerFunc.apply("whatever"));
    assertEquals(10000000000L, (long)longFunc.apply("whatever"));
    assertEquals(0.01f, floatFunc.apply("whatever"), 0.001);
    assertEquals(0.00001D, doubleFunc.apply("whatever"), 0.000001);
    assertEquals(true, booleanFunc.apply("whatever"));
    assertEquals('C', (char)charFunc.apply("whatever"));
    assertEquals("the-return", stringFunc.apply("whatever"));
    return "ok";
  }

  @Override
  public String methodWithJsonReturn(Function<String, JsonObject> objectFunc, Function<String, JsonArray> arrayFunc) {
    assertEquals(new JsonObject().put("foo", "foo_value").put("bar", 10).put("wibble", 0.1), objectFunc.apply("whatever"));
    assertEquals(new JsonArray().add("one").add("two").add("three"), arrayFunc.apply("whatever"));
    return "ok";
  }

  @Override
  public String methodWithObjectReturn(Function<Integer, Object> func) {
    assertEquals("the-string", func.apply(0));
    assertEquals(123, ((Number)func.apply(1)).intValue());
    assertEquals(true, func.apply(2));
    assertEquals(new JsonObject().put("foo", "foo_value"), func.apply(3));
    assertEquals(new JsonArray().add("foo").add("bar"), func.apply(4));
    return "ok";
  }

  @Override
  public String methodWithDataObjectReturn(Function<String, TestDataObject> func) {
    TestDataObject val = func.apply("whatever");
    assertEquals("wasabi", val.getFoo());
    assertEquals(6, val.getBar());
    assertEquals(0.01D, val.getWibble(), 0.001D);
    return "ok";
  }

  @Override
  public String methodWithEnumReturn(Function<String, TestEnum> func) {
    assertEquals(TestEnum.NICK,func.apply("whatever"));
    return "ok";
  }

  @Override
  public String methodWithListReturn(Function<String, List<String>> func) {
    assertEquals(Arrays.asList("one", "two", "three"),func.apply("whatever"));
    return "ok";
  }

  @Override
  public String methodWithSetReturn(Function<String, Set<String>> func) {
    assertEquals(new HashSet<>(Arrays.asList("one", "two", "three")),func.apply("whatever"));
    return "ok";
  }

  @Override
  public String methodWithMapReturn(Function<String, Map<String, String>> func) {
    Map<String, String> expected = new HashMap<>();
    expected.put("one", "one");
    expected.put("two", "two");
    expected.put("three", "three");
    assertEquals(expected,func.apply("whatever"));
    return "ok";
  }

  @Override
  public <T> String methodWithGenericReturn(Function<Integer, T> func) {
    return methodWithObjectReturn(func::apply);
  }

  @Override
  public <T> String methodWithGenericUserTypeReturn(Function<GenericRefedInterface<T>, GenericRefedInterface<T>> func) {
    GenericRefedInterfaceImpl<T> impl = new GenericRefedInterfaceImpl<>();
    assertEquals(impl , func.apply(impl));
    return "ok";
  }

  @Override
  public String methodWithNullableListParam(Function<@Nullable List<String>, String> func) {
    return func.apply(null);
  }

  @Override
  public String methodWithNullableListReturn(Function<String, @Nullable List<String>> func) {
    assertEquals(null, func.apply("whatever"));
    return "ok";
  }
}
