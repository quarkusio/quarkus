package io.vertx.codegen.extra;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface MethodWithSingleString {

  void doSomethingWithResult(Handler<AsyncResult<String>> handler);

}
