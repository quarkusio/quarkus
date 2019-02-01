package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface Factory {

  static ConcreteHandlerUserType createConcreteHandlerUserType(Handler<RefedInterface1> handler) {
    return handler::handle;
  }

  static AbstractHandlerUserType createAbstractHandlerUserType(Handler<RefedInterface1> handler) {
    return handler::handle;
  }

  static ConcreteHandlerUserTypeExtension createConcreteHandlerUserTypeExtension(Handler<RefedInterface1> handler) {
    return handler::handle;
  }
}
