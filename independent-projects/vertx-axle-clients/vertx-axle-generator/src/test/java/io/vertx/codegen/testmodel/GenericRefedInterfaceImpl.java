package io.vertx.codegen.testmodel;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GenericRefedInterfaceImpl<T> implements GenericRefedInterface<T> {

  private T value;

  @Override
  public GenericRefedInterface<T> setValue(T value) {
    this.value = value;
    return this;
  }

  @Override
  public T getValue() {
    return value;
  }
}
