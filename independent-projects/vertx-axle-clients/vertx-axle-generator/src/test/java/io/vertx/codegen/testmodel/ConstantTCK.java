package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

@VertxGen
public interface ConstantTCK {

  /**
   * Some doc.
   */
  byte BYTE = (byte) 123;
  Byte BOXED_BYTE = BYTE;
  short SHORT = (short)12345;
  Short BOXED_SHORT = SHORT;
  int INT = 12345464;
  Integer BOXED_INT = INT;
  long LONG = 65675123L;
  Long BOXED_LONG = LONG;
  float FLOAT = 1.23f;
  Float BOXED_FLOAT = FLOAT;
  double DOUBLE = 3.34535d;
  Double BOXED_DOUBLE = DOUBLE;
  boolean BOOLEAN = true;
  Boolean BOXED_BOOLEAN = BOOLEAN;
  char CHAR = 'Y';
  Character BOXED_CHAR = CHAR;
  String STRING = "orangutan";
  RefedInterface1 VERTX_GEN = new RefedInterface1Impl().setString("chaffinch");
  TestDataObject DATA_OBJECT = new TestDataObject().setFoo("foo").setBar(123);
  JsonObject JSON_OBJECT = new JsonObject().put("cheese", "stilton");
  JsonArray JSON_ARRAY = new JsonArray().add("socks").add("shoes");
  TestEnum ENUM = TestEnum.JULIEN;
  Throwable THROWABLE = new Exception("test");
  Object OBJECT = 4;

  @Nullable  RefedInterface1 NULLABLE_NON_NULL = VERTX_GEN;
  @Nullable RefedInterface1 NULLABLE_NULL = null;

  List<Byte> BYTE_LIST = Collections.singletonList(BYTE);
  List<Short> SHORT_LIST = Collections.singletonList(SHORT);
  List<Integer> INT_LIST = Collections.singletonList(INT);
  List<Long> LONG_LIST = Collections.singletonList(LONG);
  List<Float> FLOAT_LIST = Collections.singletonList(FLOAT);
  List<Double> DOUBLE_LIST = Collections.singletonList(DOUBLE);
  List<Boolean> BOOLEAN_LIST = Collections.singletonList(BOOLEAN);
  List<Character> CHAR_LIST = Collections.singletonList(CHAR);
  List<String> STRING_LIST = Collections.singletonList(STRING);
  List<RefedInterface1> VERTX_GEN_LIST = Collections.singletonList(VERTX_GEN);
  List<JsonObject> JSON_OBJECT_LIST = Collections.singletonList(JSON_OBJECT);
  List<JsonArray> JSON_ARRAY_LIST = Collections.singletonList(JSON_ARRAY);
  List<TestDataObject> DATA_OBJECT_LIST = Collections.singletonList(DATA_OBJECT);
  List<TestEnum> ENUM_LIST = Collections.singletonList(ENUM);

  Set<Byte> BYTE_SET = Collections.singleton(BYTE);
  Set<Short> SHORT_SET = Collections.singleton(SHORT);
  Set<Integer> INT_SET = Collections.singleton(INT);
  Set<Long> LONG_SET = Collections.singleton(LONG);
  Set<Float> FLOAT_SET = Collections.singleton(FLOAT);
  Set<Double> DOUBLE_SET = Collections.singleton(DOUBLE);
  Set<Boolean> BOOLEAN_SET = Collections.singleton(BOOLEAN);
  Set<Character> CHAR_SET = Collections.singleton(CHAR);
  Set<String> STRING_SET = Collections.singleton(STRING);
  Set<RefedInterface1> VERTX_GEN_SET = Collections.singleton(VERTX_GEN);
  Set<JsonObject> JSON_OBJECT_SET = Collections.singleton(JSON_OBJECT);
  Set<JsonArray> JSON_ARRAY_SET = Collections.singleton(JSON_ARRAY);
  Set<TestDataObject> DATA_OBJECT_SET = Collections.singleton(DATA_OBJECT);
  Set<TestEnum> ENUM_SET = Collections.singleton(ENUM);

  Map<String, Byte> BYTE_MAP = Collections.singletonMap("foo", BYTE);
  Map<String, Short> SHORT_MAP = Collections.singletonMap("foo", SHORT);
  Map<String, Integer> INT_MAP = Collections.singletonMap("foo", INT);
  Map<String, Long> LONG_MAP = Collections.singletonMap("foo", LONG);
  Map<String, Float> FLOAT_MAP = Collections.singletonMap("foo", FLOAT);
  Map<String, Double> DOUBLE_MAP = Collections.singletonMap("foo", DOUBLE);
  Map<String, Boolean> BOOLEAN_MAP = Collections.singletonMap("foo", BOOLEAN);
  Map<String, Character> CHAR_MAP = Collections.singletonMap("foo", CHAR);
  Map<String, String> STRING_MAP = Collections.singletonMap("foo", STRING);
  Map<String, JsonObject> JSON_OBJECT_MAP = Collections.singletonMap("foo", JSON_OBJECT);
  Map<String, JsonArray> JSON_ARRAY_MAP = Collections.singletonMap("foo", JSON_ARRAY);
}
