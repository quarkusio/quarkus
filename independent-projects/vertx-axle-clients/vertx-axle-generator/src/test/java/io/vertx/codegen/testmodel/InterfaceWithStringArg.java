package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.VertxGen;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface InterfaceWithStringArg extends GenericRefedInterface<String> {

  void meth();

}
