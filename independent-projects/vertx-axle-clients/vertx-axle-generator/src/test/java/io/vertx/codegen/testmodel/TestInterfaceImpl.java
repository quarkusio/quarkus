package io.vertx.codegen.testmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class TestInterfaceImpl implements TestInterface {

  private static <T> T assertInstanceOf(Class<T> expectedType, Object obj) {
    if (expectedType.isInstance(obj)) {
      return expectedType.cast(obj);
    } else {
      throw new AssertionError("Was expecting " + obj + " to be an instance of " + expectedType);
    }
  }

  @Override
  public void methodWithBasicParams(byte b, short s, int i, long l, float f, double d, boolean bool, char ch, String str) {
    assertEquals((byte) 123, b);
    assertEquals((short) 12345, s);
    assertEquals(1234567, i);
    assertEquals(1265615234l, l);
    assertEquals(12.345f, f, 0);
    assertEquals(12.34566d, d, 0);
    assertTrue(bool);
    assertEquals('X', ch);
    assertEquals("foobar", str);
  }

  @Override
  public void methodWithBasicBoxedParams(Byte b, Short s, Integer i, Long l, Float f, Double d, Boolean bool, Character ch) {
    assertEquals((byte) 123, b.byteValue());
    assertEquals((short) 12345, s.shortValue());
    assertEquals(1234567, i.intValue());
    assertEquals(1265615234l, l.longValue());
    assertEquals(12.345f, f.floatValue(), 0);
    assertEquals(12.34566d, d.doubleValue(), 0);
    assertTrue(bool);
    assertEquals('X', ch.charValue());
  }

  @Override
  public void methodWithHandlerBasicTypes(Handler<Byte> byteHandler, Handler<Short> shortHandler,
                                          Handler<Integer> intHandler, Handler<Long> longHandler,
                                          Handler<Float> floatHandler, Handler<Double> doubleHandler,
                                          Handler<Boolean> booleanHandler, Handler<Character> charHandler,
                                          Handler<String> stringHandler) {
    byteHandler.handle((byte) 123);
    shortHandler.handle((short) 12345);
    intHandler.handle(1234567);
    longHandler.handle(1265615234l);
    floatHandler.handle(12.345f);
    doubleHandler.handle(12.34566d);
    booleanHandler.handle(true);
    charHandler.handle('X');
    stringHandler.handle("quux!");
  }

  @Override
  public Handler<String> methodWithHandlerStringReturn(String expected) {
    return event -> assertEquals(expected, event);
  }

  @Override
  public <T> Handler<T> methodWithHandlerGenericReturn(Handler<T> handler) {
    // Does a bidirectionnal conversion on purpose
    return handler::handle;
  }

  @Override
  public Handler<RefedInterface1> methodWithHandlerVertxGenReturn(String expected) {
    return event -> assertEquals(expected, event.getString());
  }

  @Override
  public void methodWithHandlerAsyncResultByte(boolean sendFailure, Handler<AsyncResult<Byte>> handler) {
    if (sendFailure) {
      Exception e = new Exception("foobar!");
      handler.handle(Future.failedFuture(e));
    } else {
      handler.handle(Future.succeededFuture((byte) 123));
    }
  }

  @Override
  public void methodWithHandlerAsyncResultShort(boolean sendFailure, Handler<AsyncResult<Short>> handler) {
    if (sendFailure) {
      Exception e = new Exception("foobar!");
      handler.handle(Future.failedFuture(e));
    } else {
      handler.handle(Future.succeededFuture((short) 12345));
    }
  }

  @Override
  public void methodWithHandlerAsyncResultInteger(boolean sendFailure, Handler<AsyncResult<Integer>> handler) {
    if (sendFailure) {
      Exception e = new Exception("foobar!");
      handler.handle(Future.failedFuture(e));
    } else {
      handler.handle(Future.succeededFuture(1234567));
    }
  }

  @Override
  public void methodWithHandlerAsyncResultLong(boolean sendFailure, Handler<AsyncResult<Long>> handler) {
    if (sendFailure) {
      Exception e = new Exception("foobar!");
      handler.handle(Future.failedFuture(e));
    } else {
      handler.handle(Future.succeededFuture(1265615234l));
    }
  }

  @Override
  public void methodWithHandlerAsyncResultFloat(boolean sendFailure, Handler<AsyncResult<Float>> handler) {
    if (sendFailure) {
      Exception e = new Exception("foobar!");
      handler.handle(Future.failedFuture(e));
    } else {
      handler.handle(Future.succeededFuture(12.345f));
    }
  }

  @Override
  public void methodWithHandlerAsyncResultDouble(boolean sendFailure, Handler<AsyncResult<Double>> handler) {
    if (sendFailure) {
      Exception e = new Exception("foobar!");
      handler.handle(Future.failedFuture(e));
    } else {
      handler.handle(Future.succeededFuture(12.34566d));
    }
  }

  @Override
  public void methodWithHandlerAsyncResultBoolean(boolean sendFailure, Handler<AsyncResult<Boolean>> handler) {
    if (sendFailure) {
      Exception e = new Exception("foobar!");
      handler.handle(Future.failedFuture(e));
    } else {
      handler.handle(Future.succeededFuture(true));
    }
  }

  @Override
  public void methodWithHandlerAsyncResultCharacter(boolean sendFailure, Handler<AsyncResult<Character>> handler) {
    if (sendFailure) {
      Exception e = new Exception("foobar!");
      handler.handle(Future.failedFuture(e));
    } else {
      handler.handle(Future.succeededFuture('X'));
    }
  }

  @Override
  public void methodWithHandlerAsyncResultString(boolean sendFailure, Handler<AsyncResult<String>> handler) {
    if (sendFailure) {
      Exception e = new Exception("foobar!");
      handler.handle(Future.failedFuture(e));
    } else {
      handler.handle(Future.succeededFuture("quux!"));
    }
  }

  @Override
  public void methodWithHandlerAsyncResultDataObject(boolean sendFailure, Handler<AsyncResult<TestDataObject>> handler) {
    if (sendFailure) {
      Exception e = new Exception("foobar!");
      handler.handle(Future.failedFuture(e));
    } else {
      handler.handle(Future.succeededFuture(new TestDataObject().setFoo("foo").setBar(123)));
    }
  }

  @Override
  public Handler<AsyncResult<String>> methodWithHandlerAsyncResultStringReturn(String expected, boolean fail) {
    return ar -> {
      if (!fail) {
        assertTrue(ar.succeeded());
        assertEquals(expected, ar.result());
      } else {
        assertEquals(false, ar.succeeded());
        assertEquals(expected, ar.cause().getMessage());
      }
    };
  }

  @Override
  public <T> Handler<AsyncResult<T>> methodWithHandlerAsyncResultGenericReturn(Handler<AsyncResult<T>> handler) {
    // Does a bidirectionnal conversion on purpose
    return handler::handle;
  }

  @Override
  public Handler<AsyncResult<RefedInterface1>> methodWithHandlerAsyncResultVertxGenReturn(String expected, boolean fail) {
    return ar -> {
      if (!fail) {
        assertTrue(ar.succeeded());
        assertEquals(expected, ar.result().getString());
      } else {
        assertEquals(false, ar.succeeded());
        assertEquals(expected, ar.cause().getMessage());
      }
    };
  }

  @Override
  public void methodWithUserTypes(RefedInterface1 refed) {
    assertEquals("aardvarks", refed.getString());
  }

  @Override
  public void methodWithObjectParam(String str, Object obj) {
    switch (str) {
      case "null":
        assertNull(obj);
        break;
      case "string":
        assertTrue(obj instanceof String);
        String s = (String) obj;
        assertEquals("wibble", s);
        break;
      case "true":
        assertTrue(obj instanceof Boolean);
        Boolean t = (Boolean) obj;
        assertEquals(true, t);
        break;
      case "false":
        assertTrue(obj instanceof Boolean);
        Boolean f = (Boolean) obj;
        assertEquals(false, f);
        break;
      case "long":
        assertTrue(obj instanceof Number);
        Number l = (Number) obj;
        assertEquals(123, l.longValue());
        break;
      case "double":
        assertTrue(obj instanceof Number);
        Number n = (Number) obj;
        assertEquals(123.456, n.doubleValue(), 0);
        break;
      case "JsonObject" : {
        assertTrue(obj instanceof JsonObject);
        JsonObject jsonObject = (JsonObject)obj;
        assertEquals("hello", jsonObject.getString("foo"));
        assertEquals(123, jsonObject.getInteger("bar").intValue());
        break;
      }
      case "JsonArray": {
        assertTrue(obj instanceof JsonArray);
        JsonArray arr = (JsonArray)obj;
        assertEquals(3, arr.size());
        assertEquals("foo", arr.getString(0));
        assertEquals("bar", arr.getString(1));
        assertEquals("wib", arr.getString(2));
        break;
      }
      default: fail("invalid type");
    }
  }

  @Override
  public void methodWithDataObjectParam(TestDataObject dataObject) {
    assertEquals("hello", dataObject.getFoo());
    assertEquals(123, dataObject.getBar());
    assertEquals(1.23, dataObject.getWibble(), 0);
  }

  @Override
  public void methodWithHandlerUserTypes(Handler<RefedInterface1> handler) {
    RefedInterface1 refed = new RefedInterface1Impl();
    refed.setString("echidnas");
    handler.handle(refed);
  }

  @Override
  public void methodWithHandlerAsyncResultUserTypes(Handler<AsyncResult<RefedInterface1>> handler) {
    RefedInterface1 refed = new RefedInterface1Impl();
    refed.setString("cheetahs");
    handler.handle(Future.succeededFuture(refed));
  }

  @Override
  public void methodWithConcreteHandlerUserTypeSubtype(ConcreteHandlerUserType handler) {
    RefedInterface1 refed = new RefedInterface1Impl();
    refed.setString("echidnas");
    handler.handle(refed);
  }

  @Override
  public void methodWithAbstractHandlerUserTypeSubtype(AbstractHandlerUserType handler) {
    RefedInterface1 refed = new RefedInterface1Impl();
    refed.setString("echidnas");
    handler.handle(refed);
  }

  @Override
  public void methodWithConcreteHandlerUserTypeSubtypeExtension(ConcreteHandlerUserTypeExtension handler) {
    RefedInterface1 refed = new RefedInterface1Impl();
    refed.setString("echidnas");
    handler.handle(refed);
  }

  @Override
  public void methodWithHandlerVoid(Handler<Void> handler) {
    handler.handle(null);
  }

  @Override
  public void methodWithHandlerAsyncResultVoid(boolean sendFailure, Handler<AsyncResult<Void>> handler) {
    if (sendFailure) {
      handler.handle(Future.failedFuture(new VertxException("foo!")));
    } else {
      handler.handle(Future.succeededFuture((Void) null));
    }
  }

  @Override
  public void methodWithHandlerThrowable(Handler<Throwable> handler) {
    handler.handle(new VertxException("cheese!"));
  }

  @Override
  public void methodWithHandlerDataObject(Handler<TestDataObject> handler) {
    handler.handle(methodWithDataObjectReturn());
  }

  @Override
  public <U> void methodWithHandlerGenericUserType(U value, Handler<GenericRefedInterface<U>> handler) {
    handler.handle(methodWithGenericUserTypeReturn(value));
  }

  @Override
  public <U> void methodWithHandlerAsyncResultGenericUserType(U value, Handler<AsyncResult<GenericRefedInterface<U>>> handler) {
    handler.handle(Future.succeededFuture(methodWithGenericUserTypeReturn(value)));
  }

  @Override
  public <U> GenericRefedInterface<U> methodWithGenericUserTypeReturn(U value) {
    GenericRefedInterfaceImpl<U> userObj = new GenericRefedInterfaceImpl<>();
    userObj.setValue(value);
    return userObj;
  }

  @Override
  public byte methodWithByteReturn() {
    return (byte) 123;
  }

  @Override
  public short methodWithShortReturn() {
    return (short)12345;
  }

  @Override
  public int methodWithIntReturn() {
    return 12345464;
  }

  @Override
  public long methodWithLongReturn() {
    return 65675123l;
  }

  @Override
  public float methodWithFloatReturn() {
    return 1.23f;
  }

  @Override
  public double methodWithDoubleReturn() {
    return 3.34535d;
  }

  @Override
  public boolean methodWithBooleanReturn() {
    return true;
  }

  @Override
  public char methodWithCharReturn() {
    return 'Y';
  }

  @Override
  public String methodWithStringReturn() {
    return "orangutan";
  }

  @Override
  public RefedInterface1 methodWithVertxGenReturn() {
    RefedInterface1 refed = new RefedInterface1Impl();
    refed.setString("chaffinch");
    return refed;
  }

  @Override
  public RefedInterface1 methodWithVertxGenNullReturn() {
    return null;
  }

  @Override
  public RefedInterface2 methodWithAbstractVertxGenReturn() {
    RefedInterface2 refed = new RefedInterface2Impl();
    refed.setString("abstractchaffinch");
    return refed;
  }

  @Override
  public TestDataObject methodWithDataObjectReturn() {
    return new TestDataObject().setFoo("foo").setBar(123);
  }

  @Override
  public TestDataObject methodWithDataObjectNullReturn() {
    return null;
  }

  @Override
  public String overloadedMethod(String str, RefedInterface1 refed) {
    assertEquals("cat", str);
    assertEquals("dog", refed.getString());
    return "meth1";
  }

  @Override
  public String overloadedMethod(String str, RefedInterface1 refed, long period, Handler<String> handler) {
    assertEquals("cat", str);
    assertEquals("dog", refed.getString());
    assertEquals(12345l, period);
    assertNotNull(handler);
    handler.handle("giraffe");
    return "meth2";
  }

  @Override
  public String overloadedMethod(String str, Handler<String> handler) {
    assertEquals("cat", str);
    assertNotNull(handler);
    handler.handle("giraffe");
    return "meth3";
  }

  @Override
  public String overloadedMethod(String str, RefedInterface1 refed, Handler<String> handler) {
    assertEquals("cat", str);
    assertEquals("dog", refed.getString());
    assertNotNull(handler);
    handler.handle("giraffe");
    return "meth4";
  }

  @Override
  public void superMethodWithBasicParams(byte b, short s, int i, long l, float f, double d, boolean bool, char ch, String str) {
    assertEquals((byte) 123, b);
    assertEquals((short) 12345, s);
    assertEquals(1234567, i);
    assertEquals(1265615234l, l);
    assertEquals(12.345f, f, 0);
    assertEquals(12.34566d, d, 0);
    assertTrue(bool);
    assertEquals('X', ch);
    assertEquals("foobar", str);
  }

  @Override
  public void otherSuperMethodWithBasicParams(byte b, short s, int i, long l, float f, double d, boolean bool, char ch, String str) {
    superMethodWithBasicParams(b, s, i, l, f, d, bool, ch, str);
  }

  @Override
  public <U> U methodWithGenericReturn(String type) {
    switch (type) {
      case "Boolean": {
        return (U) Boolean.valueOf(true);
      }
      case "Byte": {
        return (U) Byte.valueOf((byte)123);
      }
      case "Short": {
        return (U) Short.valueOf((short)12345);
      }
      case "Integer": {
        return (U) Integer.valueOf(1234567);
      }
      case "Long": {
        return (U) Long.valueOf(1265615234);
      }
      case "Float": {
        return (U) Float.valueOf(12.345f);
      }
      case "Double": {
        return (U) Double.valueOf(12.34566d);
      }
      case "Character": {
        return (U) Character.valueOf('x');
      }
      case "String": {
        return (U) "foo";
      }
      case "Ref": {
        return (U) new RefedInterface1Impl().setString("bar");
      }
      case "JsonObject": {
        return (U) (new JsonObject().put("foo", "hello").put("bar", 123));
      }
      case "JsonObjectLong": {
        // Some languages will convert to Long
        return (U) (new JsonObject().put("foo", "hello").put("bar", 123L));
      }
      case "JsonObjectComplex": {
        return (U) (new JsonObject().put("outer", new JsonObject().put("foo", "hello")).put("bar", new JsonArray().add("this").add("that")));
      }
      case "JsonArray": {
        return (U) (new JsonArray().add("foo").add("bar").add("wib"));
      }
      default:
        throw new AssertionError("Unexpected " + type);
    }
  }

  @Override
  public <U> void methodWithGenericParam(String type, U u) {
    Object expected = methodWithGenericReturn(type);
    assertEquals(expected.getClass(), u.getClass());
    assertEquals(expected, u);
  }

  @Override
  public <U> void methodWithGenericHandler(String type, Handler<U> handler) {
    U value = methodWithGenericReturn(type);
    handler.handle(value);
  }

  @Override
  public <U> void methodWithGenericHandlerAsyncResult(String type, Handler<AsyncResult<U>> asyncResultHandler) {
    U value = methodWithGenericReturn(type);
    asyncResultHandler.handle(Future.succeededFuture(value));
  }

  @Override
  public TestInterface fluentMethod(String str) {
    assertEquals("bar", str);
    return this;
  }

  @Override
  public RefedInterface1 methodWithCachedReturn(String foo) {
    RefedInterface1 refed = new RefedInterface1Impl();
    refed.setString(foo);
    return refed;
  }

  @Override
  public int methodWithCachedReturnPrimitive(int arg) {
    return arg;
  }

  @Override
  public List<RefedInterface1> methodWithCachedListReturn() {
    return Arrays.asList(new RefedInterface1Impl().setString("foo"), new RefedInterface1Impl().setString("bar"));
  }

  @Override
  public JsonObject methodWithJsonObjectReturn() {
    return new JsonObject().put("cheese", "stilton");
  }

  @Override
  public JsonObject methodWithNullJsonObjectReturn() {
    return null;
  }

  @Override
  public JsonObject methodWithComplexJsonObjectReturn() {
    return new JsonObject().put("outer", new JsonObject().put("socks", "tartan")).put("list", new JsonArray().add("yellow").add("blue"));
  }

  @Override
  public JsonArray methodWithJsonArrayReturn() {
    return new JsonArray().add("socks").add("shoes");
  }

  @Override
  public JsonArray methodWithNullJsonArrayReturn() {
    return null;
  }

  @Override
  public JsonArray methodWithComplexJsonArrayReturn() {
    return new JsonArray().add(new JsonObject().put("foo", "hello")).add(new JsonObject().put("bar", "bye"));
  }

  @Override
  public void methodWithJsonParams(JsonObject jsonObject, JsonArray jsonArray) {
    assertNotNull(jsonObject);
    assertNotNull(jsonArray);
    assertEquals("lion", jsonObject.getString("cat"));
    assertEquals("cheddar", jsonObject.getString("cheese"));
    assertEquals("house", jsonArray.getString(0));
    assertEquals("spider", jsonArray.getString(1));
  }

  @Override
  public void methodWithNullJsonParams(JsonObject jsonObject, JsonArray jsonArray) {
    assertNull(jsonObject);
    assertNull(jsonArray);
  }

  @Override
  public void methodWithHandlerJson(Handler<JsonObject> jsonObjectHandler, Handler<JsonArray> jsonArrayHandler) {
    assertNotNull(jsonObjectHandler);
    assertNotNull(jsonArrayHandler);
    jsonObjectHandler.handle(new JsonObject().put("cheese", "stilton"));
    jsonArrayHandler.handle(new JsonArray().add("socks").add("shoes"));
  }

  @Override
  public void methodWithHandlerComplexJson(Handler<JsonObject> jsonObjectHandler, Handler<JsonArray> jsonArrayHandler) {
    assertNotNull(jsonObjectHandler);
    assertNotNull(jsonArrayHandler);
    jsonObjectHandler.handle(new JsonObject().put("outer", new JsonObject().put("socks", "tartan")).put("list", new JsonArray().add("yellow").add("blue")));
    jsonArrayHandler.handle(new JsonArray().add(new JsonArray().add(new JsonObject().put("foo", "hello"))).add(new JsonArray().add(new JsonObject().put("bar", "bye"))));
  }

  @Override
  public void methodWithHandlerAsyncResultJsonObject(Handler<AsyncResult<JsonObject>> handler) {
    assertNotNull(handler);
    handler.handle(Future.succeededFuture(new JsonObject().put("cheese", "stilton")));
  }

  @Override
  public void methodWithHandlerAsyncResultNullJsonObject(Handler<AsyncResult<JsonObject>> handler) {
    assertNotNull(handler);
    handler.handle(Future.succeededFuture(null));
  }

  @Override
  public void methodWithHandlerAsyncResultComplexJsonObject(Handler<AsyncResult<JsonObject>> handler) {
    assertNotNull(handler);
    handler.handle(Future.succeededFuture(new JsonObject().put("outer", new JsonObject().put("socks", "tartan")).put("list", new JsonArray().add("yellow").add("blue"))));
  }

  @Override
  public void methodWithHandlerAsyncResultJsonArray(Handler<AsyncResult<JsonArray>> handler) {
    assertNotNull(handler);
    handler.handle(Future.succeededFuture(new JsonArray().add("socks").add("shoes")));
  }

  @Override
  public void methodWithHandlerAsyncResultNullJsonArray(Handler<AsyncResult<JsonArray>> handler) {
    assertNotNull(handler);
    handler.handle(Future.succeededFuture(null));
  }

  @Override
  public void methodWithHandlerAsyncResultComplexJsonArray(Handler<AsyncResult<JsonArray>> handler) {
    assertNotNull(handler);
    handler.handle(Future.succeededFuture(new JsonArray().add(new JsonObject().put("foo", "hello")).add(new JsonObject().put("bar", "bye"))));
  }

  @Override
  public String methodWithEnumParam(String strVal, TestEnum weirdo) {
    return strVal + weirdo;
  }

  @Override
  public TestEnum methodWithEnumReturn(String strVal) {
    return TestEnum.valueOf(strVal);
  }

  @Override
  public String methodWithGenEnumParam(String strVal, TestGenEnum weirdo) {
    return strVal + weirdo;
  }

  @Override
  public TestGenEnum methodWithGenEnumReturn(String strVal) {
    return TestGenEnum.valueOf(strVal);
  }

  @Override
  public Throwable methodWithThrowableReturn(String strVal) {
    return new Exception(strVal);
  }

  @Override
  public String methodWithThrowableParam(Throwable t) {
    return t.getMessage();
  }

  @Override
  public int superMethodOverloadedBySubclass(String s) {
    return 1;
  }

  @Override
  public int superMethodOverloadedBySubclass() {
    return 0;
  }

}
