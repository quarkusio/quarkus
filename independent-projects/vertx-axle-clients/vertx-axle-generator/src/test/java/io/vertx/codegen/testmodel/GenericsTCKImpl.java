package io.vertx.codegen.testmodel;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GenericsTCKImpl implements GenericsTCK {

  @Override
  public GenericRefedInterface<Byte> methodWithByteParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn((byte)123);
  }

  @Override
  public GenericRefedInterface<Short> methodWithShortParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn((short)1234);
  }

  @Override
  public GenericRefedInterface<Integer> methodWithIntegerParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn(123456);
  }

  @Override
  public GenericRefedInterface<Long> methodWithLongParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn(123456789L);
  }

  @Override
  public GenericRefedInterface<Float> methodWithFloatParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn(0.34F);
  }

  @Override
  public GenericRefedInterface<Double> methodWithDoubleParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn(0.314D);
  }

  @Override
  public GenericRefedInterface<Boolean> methodWithBooleanParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn(true);
  }

  @Override
  public GenericRefedInterface<Character> methodWithCharacterParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn('F');
  }

  @Override
  public GenericRefedInterface<String> methodWithStringParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn("zoumbawe");
  }

  @Override
  public GenericRefedInterface<JsonObject> methodWithJsonObjectParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn(new JsonObject().put("cheese", "stilton"));
  }

  @Override
  public GenericRefedInterface<JsonArray> methodWithJsonArrayParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn(new JsonArray().add("cheese").add("stilton"));
  }

  @Override
  public GenericRefedInterface<TestDataObject> methodWithDataObjectParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn(new TestDataObject().setWibble(3.14).setFoo("foo_value").setBar(123456));
  }

  @Override
  public GenericRefedInterface<TestEnum> methodWithEnumParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn(TestEnum.WESTON);
  }

  @Override
  public GenericRefedInterface<TestGenEnum> methodWithGenEnumParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn(TestGenEnum.LELAND);
  }

  @Override
  public GenericRefedInterface<RefedInterface1> methodWithUserTypeParameterizedReturn() {
    return methodWithClassTypeParameterizedReturn(new RefedInterface1Impl().setString("foo"));
  }

  @Override
  public void methodWithHandlerByteParameterized(Handler<GenericRefedInterface<Byte>> handler) {
    handler.handle(methodWithByteParameterizedReturn());
  }

  @Override
  public void methodWithHandlerShortParameterized(Handler<GenericRefedInterface<Short>> handler) {
    handler.handle(methodWithShortParameterizedReturn());
  }

  @Override
  public void methodWithHandlerIntegerParameterized(Handler<GenericRefedInterface<Integer>> handler) {
    handler.handle(methodWithIntegerParameterizedReturn());
  }

  @Override
  public void methodWithHandlerLongParameterized(Handler<GenericRefedInterface<Long>> handler) {
    handler.handle(methodWithLongParameterizedReturn());
  }

  @Override
  public void methodWithHandlerFloatParameterized(Handler<GenericRefedInterface<Float>> handler) {
    handler.handle(methodWithFloatParameterizedReturn());
  }

  @Override
  public void methodWithHandlerDoubleParameterized(Handler<GenericRefedInterface<Double>> handler) {
    handler.handle(methodWithDoubleParameterizedReturn());
  }

  @Override
  public void methodWithHandlerBooleanParameterized(Handler<GenericRefedInterface<Boolean>> handler) {
    handler.handle(methodWithBooleanParameterizedReturn());
  }

  @Override
  public void methodWithHandlerCharacterParameterized(Handler<GenericRefedInterface<Character>> handler) {
    handler.handle(methodWithCharacterParameterizedReturn());
  }

  @Override
  public void methodWithHandlerStringParameterized(Handler<GenericRefedInterface<String>> handler) {
    handler.handle(methodWithStringParameterizedReturn());
  }

  @Override
  public void methodWithHandlerJsonObjectParameterized(Handler<GenericRefedInterface<JsonObject>> handler) {
    handler.handle(methodWithJsonObjectParameterizedReturn());
  }

  @Override
  public void methodWithHandlerJsonArrayParameterized(Handler<GenericRefedInterface<JsonArray>> handler) {
    handler.handle(methodWithJsonArrayParameterizedReturn());
  }

  @Override
  public void methodWithHandlerDataObjectParameterized(Handler<GenericRefedInterface<TestDataObject>> handler) {
    handler.handle(methodWithDataObjectParameterizedReturn());
  }

  @Override
  public void methodWithHandlerEnumParameterized(Handler<GenericRefedInterface<TestEnum>> handler) {
    handler.handle(methodWithEnumParameterizedReturn());
  }

  @Override
  public void methodWithHandlerGenEnumParameterized(Handler<GenericRefedInterface<TestGenEnum>> handler) {
    handler.handle(methodWithGenEnumParameterizedReturn());
  }

  @Override
  public void methodWithHandlerUserTypeParameterized(Handler<GenericRefedInterface<RefedInterface1>> handler) {
    handler.handle(methodWithUserTypeParameterizedReturn());
  }

  @Override
  public void methodWithHandlerAsyncResultByteParameterized(Handler<AsyncResult<GenericRefedInterface<Byte>>> handler) {
    handler.handle(Future.succeededFuture(methodWithByteParameterizedReturn()));
  }

  @Override
  public void methodWithHandlerAsyncResultShortParameterized(Handler<AsyncResult<GenericRefedInterface<Short>>> handler) {
    handler.handle(Future.succeededFuture(methodWithShortParameterizedReturn()));
  }

  @Override
  public void methodWithHandlerAsyncResultIntegerParameterized(Handler<AsyncResult<GenericRefedInterface<Integer>>> handler) {
    handler.handle(Future.succeededFuture(methodWithIntegerParameterizedReturn()));
  }

  @Override
  public void methodWithHandlerAsyncResultLongParameterized(Handler<AsyncResult<GenericRefedInterface<Long>>> handler) {
    handler.handle(Future.succeededFuture(methodWithLongParameterizedReturn()));
  }

  @Override
  public void methodWithHandlerAsyncResultFloatParameterized(Handler<AsyncResult<GenericRefedInterface<Float>>> handler) {
    handler.handle(Future.succeededFuture(methodWithFloatParameterizedReturn()));
  }

  @Override
  public void methodWithHandlerAsyncResultDoubleParameterized(Handler<AsyncResult<GenericRefedInterface<Double>>> handler) {
    handler.handle(Future.succeededFuture(methodWithDoubleParameterizedReturn()));
  }

  @Override
  public void methodWithHandlerAsyncResultBooleanParameterized(Handler<AsyncResult<GenericRefedInterface<Boolean>>> handler) {
    handler.handle(Future.succeededFuture(methodWithBooleanParameterizedReturn()));
  }

  @Override
  public void methodWithHandlerAsyncResultCharacterParameterized(Handler<AsyncResult<GenericRefedInterface<Character>>> handler) {
    handler.handle(Future.succeededFuture(methodWithCharacterParameterizedReturn()));
  }

  @Override
  public void methodWithHandlerAsyncResultStringParameterized(Handler<AsyncResult<GenericRefedInterface<String>>> handler) {
    handler.handle(Future.succeededFuture(methodWithStringParameterizedReturn()));
  }

  @Override
  public void methodWithHandlerAsyncResultJsonObjectParameterized(Handler<AsyncResult<GenericRefedInterface<JsonObject>>> handler) {
    handler.handle(Future.succeededFuture(methodWithJsonObjectParameterizedReturn()));
  }

  @Override
  public void methodWithHandlerAsyncResultJsonArrayParameterized(Handler<AsyncResult<GenericRefedInterface<JsonArray>>> handler) {
    handler.handle(Future.succeededFuture(methodWithJsonArrayParameterizedReturn()));
  }

  @Override
  public void methodWithHandlerAsyncResultDataObjectParameterized(Handler<AsyncResult<GenericRefedInterface<TestDataObject>>> handler) {
    handler.handle(Future.succeededFuture(methodWithDataObjectParameterizedReturn()));
  }

  @Override
  public void methodWithHandlerAsyncResultEnumParameterized(Handler<AsyncResult<GenericRefedInterface<TestEnum>>> handler) {
    handler.handle(Future.succeededFuture(methodWithEnumParameterizedReturn()));
  }

  @Override
  public void methodWithHandlerAsyncResultGenEnumParameterized(Handler<AsyncResult<GenericRefedInterface<TestGenEnum>>> handler) {
    handler.handle(Future.succeededFuture(methodWithGenEnumParameterizedReturn()));
  }

  @Override
  public void methodWithHandlerAsyncResultUserTypeParameterized(Handler<AsyncResult<GenericRefedInterface<RefedInterface1>>> handler) {
    handler.handle(Future.succeededFuture(methodWithUserTypeParameterizedReturn()));
  }

  @Override
  public void methodWithFunctionParamByteParameterized(Function<GenericRefedInterface<Byte>, String> handler) {
    handler.apply(methodWithByteParameterizedReturn());
  }

  @Override
  public void methodWithFunctionParamShortParameterized(Function<GenericRefedInterface<Short>, String> handler) {
    handler.apply(methodWithShortParameterizedReturn());
  }

  @Override
  public void methodWithFunctionParamIntegerParameterized(Function<GenericRefedInterface<Integer>, String> handler) {
    handler.apply(methodWithIntegerParameterizedReturn());
  }

  @Override
  public void methodWithFunctionParamLongParameterized(Function<GenericRefedInterface<Long>, String> handler) {
    handler.apply(methodWithLongParameterizedReturn());
  }

  @Override
  public void methodWithFunctionParamFloatParameterized(Function<GenericRefedInterface<Float>, String> handler) {
    handler.apply(methodWithFloatParameterizedReturn());
  }

  @Override
  public void methodWithFunctionParamDoubleParameterized(Function<GenericRefedInterface<Double>, String> handler) {
    handler.apply(methodWithDoubleParameterizedReturn());
  }

  @Override
  public void methodWithFunctionParamBooleanParameterized(Function<GenericRefedInterface<Boolean>, String> handler) {
    handler.apply(methodWithBooleanParameterizedReturn());
  }

  @Override
  public void methodWithFunctionParamCharacterParameterized(Function<GenericRefedInterface<Character>, String> handler) {
    handler.apply(methodWithCharacterParameterizedReturn());
  }

  @Override
  public void methodWithFunctionParamStringParameterized(Function<GenericRefedInterface<String>, String> handler) {
    handler.apply(methodWithStringParameterizedReturn());
  }

  @Override
  public void methodWithFunctionParamJsonObjectParameterized(Function<GenericRefedInterface<JsonObject>, String> handler) {
    handler.apply(methodWithJsonObjectParameterizedReturn());
  }

  @Override
  public void methodWithFunctionParamJsonArrayParameterized(Function<GenericRefedInterface<JsonArray>, String> handler) {
    handler.apply(methodWithJsonArrayParameterizedReturn());
  }

  @Override
  public void methodWithFunctionParamDataObjectParameterized(Function<GenericRefedInterface<TestDataObject>, String> handler) {
    handler.apply(methodWithDataObjectParameterizedReturn());
  }

  @Override
  public void methodWithFunctionParamEnumParameterized(Function<GenericRefedInterface<TestEnum>, String> handler) {
    handler.apply(methodWithEnumParameterizedReturn());
  }

  @Override
  public void methodWithFunctionParamGenEnumParameterized(Function<GenericRefedInterface<TestGenEnum>, String> handler) {
    handler.apply(methodWithGenEnumParameterizedReturn());
  }

  @Override
  public void methodWithFunctionParamUserTypeParameterized(Function<GenericRefedInterface<RefedInterface1>, String> handler) {
    handler.apply(methodWithUserTypeParameterizedReturn());
  }

  @Override
  public <U> GenericRefedInterface<U> methodWithClassTypeParameterizedReturn(Class<U> type) {
    if (type == Byte.class) {
      return (GenericRefedInterface<U>) methodWithByteParameterizedReturn();
    }
    if (type == Short.class) {
      return (GenericRefedInterface<U>) methodWithShortParameterizedReturn();
    }
    if (type == Integer.class) {
      return (GenericRefedInterface<U>) methodWithIntegerParameterizedReturn();
    }
    if (type == Long.class) {
      return (GenericRefedInterface<U>) methodWithLongParameterizedReturn();
    }
    if (type == Float.class) {
      return (GenericRefedInterface<U>) methodWithFloatParameterizedReturn();
    }
    if (type == Double.class) {
      return (GenericRefedInterface<U>) methodWithDoubleParameterizedReturn();
    }
    if (type == Boolean.class) {
      return (GenericRefedInterface<U>) methodWithBooleanParameterizedReturn();
    }
    if (type == Character.class) {
      return (GenericRefedInterface<U>) methodWithCharacterParameterizedReturn();
    }
    if (type == String.class) {
      return (GenericRefedInterface<U>) methodWithStringParameterizedReturn();
    }
    if (type == JsonObject.class) {
      return (GenericRefedInterface<U>) methodWithJsonObjectParameterizedReturn();
    }
    if (type == JsonArray.class) {
      return (GenericRefedInterface<U>) methodWithJsonArrayParameterizedReturn();
    }
    if (type == TestDataObject.class) {
      return (GenericRefedInterface<U>) methodWithDataObjectParameterizedReturn();
    }
    if (type == TestEnum.class) {
      return (GenericRefedInterface<U>) methodWithEnumParameterizedReturn();
    }
    if (type == TestGenEnum.class) {
      return (GenericRefedInterface<U>) methodWithGenEnumParameterizedReturn();
    }
    if (type == RefedInterface1.class) {
      return (GenericRefedInterface<U>) methodWithUserTypeParameterizedReturn();
    }
    throw new AssertionError("Unexpected type " + type);
  }

  @Override
  public <U> U methodWithClassTypeReturn(Class<U> type) {
    return methodWithClassTypeParameterizedReturn(type).getValue();
  }

  @Override
  public <U> void methodWithClassTypeParam(Class<U> type, U u) {
    GenericRefedInterface<U> gen = methodWithClassTypeParameterizedReturn(type);
    if (!u.equals(gen.getValue())) {
      throw new AssertionError("Unexpected value " + u + "/" + u.getClass() + " != " + gen.getValue() + "/" + gen.getValue().getClass());
    }
  }

  @Override
  public <U> void methodWithClassTypeHandler(Class<U> type, Handler<U> f) {
    f.handle(methodWithClassTypeReturn(type));
  }

  @Override
  public <U> void methodWithClassTypeHandlerAsyncResult(Class<U> type, Handler<AsyncResult<U>> f) {
    f.handle(Future.succeededFuture(methodWithClassTypeReturn(type)));
  }

  @Override
  public <U> void methodWithClassTypeFunctionParam(Class<U> type, Function<U, String> f) {
    f.apply(methodWithClassTypeReturn(type));
  }

  @Override
  public <U> void methodWithClassTypeFunctionReturn(Class<U> type, Function<String, U> f) {
    methodWithClassTypeParam(type, f.apply("whatever"));
  }

  @Override
  public <U> void methodWithHandlerClassTypeParameterized(Class<U> type, Handler<GenericRefedInterface<U>> handler) {
    handler.handle(methodWithClassTypeParameterizedReturn(type));
  }

  @Override
  public <U> void methodWithHandlerAsyncResultClassTypeParameterized(Class<U> type, Handler<AsyncResult<GenericRefedInterface<U>>> handler) {
    handler.handle(Future.succeededFuture(methodWithClassTypeParameterizedReturn(type)));
  }

  @Override
  public <U> void methodWithFunctionParamClassTypeParameterized(Class<U> type, Function<GenericRefedInterface<U>, String> function) {
    function.apply(methodWithClassTypeParameterizedReturn(type));
  }

  private <U> GenericRefedInterface<U> methodWithClassTypeParameterizedReturn(U val) {
    GenericRefedInterfaceImpl<U> obj = new GenericRefedInterfaceImpl<>();
    obj.setValue(val);
    return obj;
  }

  @Override
  public InterfaceWithApiArg interfaceWithApiArg(RefedInterface1 value) {
    return new InterfaceWithApiArg() {
      private RefedInterface1 val = value;
      @Override
      public void meth() {
      }
      @Override
      public GenericRefedInterface<RefedInterface1> setValue(RefedInterface1 value) {
        val = value;
        return this;
      }
      @Override
      public RefedInterface1 getValue() {
        return val;
      }
    };
  }

  @Override
  public InterfaceWithStringArg interfaceWithStringArg(String value) {
    return new InterfaceWithStringArg() {
      private String val = value;
      @Override
      public void meth() {
      }
      @Override
      public GenericRefedInterface<String> setValue(String value) {
        val = value;
        return this;
      }
      @Override
      public String getValue() {
        return val;
      }
    };
  }

  @Override
  public <T, U> InterfaceWithVariableArg<T, U> interfaceWithVariableArg(T value1, Class<U> type, U value2) {
    return new InterfaceWithVariableArg<T, U>() {
      private T val1 = value1;
      private U val2 = value2;
      @Override
      public void setOtherValue(T value) {
        val1 = value;
      }
      @Override
      public T getOtherValue() {
        return val1;
      }
      @Override
      public GenericRefedInterface<U> setValue(U value) {
        val2 = value;
        return this;
      }
      @Override
      public U getValue() {
        return val2;
      }
    };
  }

  @Override
  public GenericNullableRefedInterface<RefedInterface1> methodWithGenericNullableApiReturn(boolean notNull) {
    return new GenericNullableRefedInterface<RefedInterface1>() {
      @Override
      public RefedInterface1 getValue() {
        return notNull ? new RefedInterface1Impl().setString("the_string_value") : null;
      }
    };
  }

  @Override
  public void methodWithHandlerGenericNullableApi(boolean notNull, Handler<GenericNullableRefedInterface<RefedInterface1>> handler) {
    handler.handle(methodWithGenericNullableApiReturn(notNull));
  }

  @Override
  public void methodWithHandlerAsyncResultGenericNullableApi(boolean notNull, Handler<AsyncResult<GenericNullableRefedInterface<RefedInterface1>>> handler) {
    handler.handle(Future.succeededFuture(methodWithGenericNullableApiReturn(notNull)));
  }

  @Override
  public <T> GenericRefedInterface<T> methodWithParamInferedReturn(GenericRefedInterface<T> param) {
    return param;
  }

  @Override
  public <T> void methodWithHandlerParamInfered(GenericRefedInterface<T> param, Handler<GenericRefedInterface<T>> handler) {
    handler.handle(param);
  }

  @Override
  public <T> void methodWithHandlerAsyncResultParamInfered(GenericRefedInterface<T> param, Handler<AsyncResult<GenericRefedInterface<T>>> handler) {
    handler.handle(Future.succeededFuture(param));
  }
}
