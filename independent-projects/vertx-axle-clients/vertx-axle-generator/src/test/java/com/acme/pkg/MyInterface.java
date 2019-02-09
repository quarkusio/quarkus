package com.acme.pkg;

import com.acme.pkg.sub.SubInterface;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.codegen.testmodel.TestInterface;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface MyInterface {

  static MyInterface create() {
    return new MyInterfaceImpl();
  }

  SubInterface sub();

  TestInterface method();

}
