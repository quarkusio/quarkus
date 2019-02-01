package io.vertx.codegen.extra;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface MethodWithNullableTypeVariable<T> {
  void doSomethingWithMaybeResult(Handler<AsyncResult<@Nullable T>> handler);
}
