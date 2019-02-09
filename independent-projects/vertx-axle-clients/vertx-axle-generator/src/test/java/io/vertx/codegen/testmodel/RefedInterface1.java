package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations
.VertxGen;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface RefedInterface1 {

  String getString();

  @Fluent
  RefedInterface1 setString(String str);
}
