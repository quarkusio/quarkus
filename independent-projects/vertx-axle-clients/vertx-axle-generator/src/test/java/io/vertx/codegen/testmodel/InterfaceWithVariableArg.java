package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.VertxGen;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface InterfaceWithVariableArg<T, U> extends GenericRefedInterface<U> {

  void setOtherValue(T value);

  T getOtherValue();

}
