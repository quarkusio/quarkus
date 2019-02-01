package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.VertxGen;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface SuperInterface1 {

  void superMethodWithBasicParams(byte b, short s, int i, long l, float f, double d, boolean bool, char ch, String str);

  int superMethodOverloadedBySubclass();

}
