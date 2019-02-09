package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface GenericRefedInterface<T> {

  @Fluent
  GenericRefedInterface<T> setValue(T value);

  T getValue();

}
