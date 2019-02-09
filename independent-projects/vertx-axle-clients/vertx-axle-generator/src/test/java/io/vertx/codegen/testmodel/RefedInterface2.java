package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen(concrete = false)
public interface RefedInterface2 {

  String getString();

  @Fluent
  RefedInterface2 setString(String str);
}
