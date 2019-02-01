package io.vertx.axle.test;

import io.vertx.axle.codegen.extra.MethodWithMultiCallback;
import io.vertx.axle.codegen.extra.MethodWithNullableTypeVariableParamByVoidArg;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.testmodel.NullableTCKImpl;
import io.vertx.codegen.testmodel.TestInterfaceImpl;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.axle.codegen.testmodel.NullableTCK;
import io.vertx.axle.codegen.testmodel.TestInterface;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ApiTest {

  @Test
  public void testConsumer() {
    TestInterface obj = new TestInterface(new TestInterfaceImpl());
    obj.methodWithHandlerDataObject(cons -> {

    });
  }

  @Test
  public void testSingle() {
    TestInterface obj = new TestInterface(new TestInterfaceImpl());
    CompletionStage<String> fut = obj.methodWithHandlerAsyncResultString(false);
    AtomicInteger result = new AtomicInteger();
    AtomicInteger fail = new AtomicInteger();
    fut.whenComplete((res, err) -> {
      if (res != null) {
        result.getAndIncrement();
      }
      if (err != null) {
        fail.getAndIncrement();
      }
    });
    assertEquals(1, result.get());
    assertEquals(0, fail.get());
  }

  @Test
  public void testCompletable() {
    TestInterface obj = new TestInterface(new TestInterfaceImpl());
    CompletionStage<Void> failure = obj.methodWithHandlerAsyncResultVoid(true);
    AtomicInteger count = new AtomicInteger();
    failure.whenComplete((res, err) -> {
      assertNull(res);
      assertNotNull(err);
      count.incrementAndGet();
    });
    assertEquals(1, count.getAndSet(0));
    CompletionStage<Void> success = obj.methodWithHandlerAsyncResultVoid(false);
    success.whenComplete((res, err) -> {
      assertNull(res);
      assertNull(err);
      count.incrementAndGet();
    });
    assertEquals(1, count.get());
  }

  @Test
  public void testMaybe() {
    NullableTCK obj = new NullableTCK(new NullableTCKImpl());
    List<String> result = new ArrayList<>();
    List<Throwable> failure = new ArrayList<>();
    AtomicInteger completions = new AtomicInteger();
    CompletionStage<String> maybeNotNull = obj.methodWithNullableStringHandlerAsyncResult(true);
    maybeNotNull.whenComplete((res, err) -> {
      if (res != null) {
        result.add(res);
      }
      if (err != null) {
        failure.add(err);
      }
      completions.incrementAndGet();
    });
    assertEquals(Collections.singletonList("the_string_value"), result);
    assertEquals(Collections.emptyList(), failure);
    assertEquals(1, completions.get());
    result.clear();
    completions.set(0);
    maybeNotNull = obj.methodWithNullableStringHandlerAsyncResult(false);
    maybeNotNull.whenComplete((res, err) -> {
      if (res != null) {
        result.add(res);
      }
      if (err != null) {
        failure.add(err);
      }
      completions.incrementAndGet();
    });
    assertEquals(Collections.emptyList(), result);
    assertEquals(Collections.emptyList(), failure);
    assertEquals(1, completions.get());
    result.clear();
  }

  @Test
  public void testMultiCompletions() {
    MethodWithMultiCallback objectMethodWithMultiCompletable = MethodWithMultiCallback.newInstance(new io.vertx.codegen.extra.MethodWithMultiCallback() {
      @Override
      public void multiCompletable(Handler<AsyncResult<Void>> handler) {
        handler.handle(Future.succeededFuture());
        handler.handle(Future.succeededFuture());
      }
      @Override
      public void multiMaybe(Handler<AsyncResult<@Nullable String>> handler) {
        handler.handle(Future.succeededFuture());
        handler.handle(Future.succeededFuture("foo"));
      }
      @Override
      public void multiSingle(Handler<AsyncResult<String>> handler) {
        handler.handle(Future.succeededFuture("foo"));
        handler.handle(Future.succeededFuture("foo"));
      }
    });
    AtomicInteger count = new AtomicInteger();
    objectMethodWithMultiCompletable.multiCompletable().whenComplete((res, err) -> count.incrementAndGet());
    assertEquals(1, count.getAndSet(0));
    objectMethodWithMultiCompletable.multiMaybe().whenComplete((res, err) -> count.incrementAndGet());
    assertEquals(1, count.getAndSet(0));
    objectMethodWithMultiCompletable.multiSingle().whenComplete((res, err) -> count.incrementAndGet());
    assertEquals(1, count.getAndSet(0));
  }

  @Test
  public void testNullableTypeVariableParamByVoidArg() {
    MethodWithNullableTypeVariableParamByVoidArg abc = MethodWithNullableTypeVariableParamByVoidArg.newInstance(handler -> handler.handle(Future.succeededFuture()));
    CompletionStage<Void> maybe = abc.doSomethingWithMaybeResult();
    AtomicInteger count = new AtomicInteger();
    maybe.whenComplete((res, err) -> {
      assertNull(res);
      assertNull(err);
      count.incrementAndGet();
    });
    assertEquals(1, count.get());
  }
}
