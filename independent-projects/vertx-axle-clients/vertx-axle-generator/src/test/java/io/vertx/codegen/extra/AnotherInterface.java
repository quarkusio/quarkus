package io.vertx.codegen.extra;

import io.vertx.codegen.annotations.VertxGen;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@VertxGen
public interface AnotherInterface {

  static AnotherInterface create() {
    return new AnotherInterfaceImpl();
  }


  <T> T methodWithClassParam(Class<T> tClass);

}
