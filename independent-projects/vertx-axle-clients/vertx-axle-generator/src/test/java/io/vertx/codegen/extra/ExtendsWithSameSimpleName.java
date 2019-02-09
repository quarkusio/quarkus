package io.vertx.codegen.extra;

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.codegen.extra.sub.UseVertxGenNameDeclarationsWithSameSimpleName;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

import java.util.function.Function;

@VertxGen
public interface ExtendsWithSameSimpleName extends
  UseVertxGenNameDeclarationsWithSameSimpleName,
  Handler<UseVertxGenNameDeclarationsWithSameSimpleName>,
  ReadStream<UseVertxGenNameDeclarationsWithSameSimpleName> {

  @CacheReturn
  UseVertxGenNameDeclarationsWithSameSimpleName foo(UseVertxGenNameDeclarationsWithSameSimpleName arg);

  void function(Function<UseVertxGenNameDeclarationsWithSameSimpleName, UseVertxGenNameDeclarationsWithSameSimpleName> function);
  void asyncResult(Handler<AsyncResult<UseVertxGenNameDeclarationsWithSameSimpleName>> handler);

}
