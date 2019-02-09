package io.vertx.lang.axle;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Helper {

  /**
   * Unwrap the type used in RxJava.
   *
   * @param type the type to unwrap
   * @return the unwrapped type
   */
  public static Class unwrap(Class<?> type) {
    if (type != null) {
      Gen rxgen = type.getAnnotation(Gen.class);
      if (rxgen != null) {
        return rxgen.value();
      }
    }
    return type;
  }
}
