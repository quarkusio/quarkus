package io.vertx.codegen.extra;

import io.vertx.codegen.annotations.VertxGen;

/**
 * Special case : we need to generate {@code Maybe<Void>} because of covariant return type and because
 * {@code Completable} does not extend {@code Maybe}.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface MethodWithNullableTypeVariableParamByVoidArg extends MethodWithNullableTypeVariable<Void> {
}
