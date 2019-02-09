package io.vertx.codegen.testmodel;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class RefedInterface2Impl implements RefedInterface2 {

  private String str;

  @Override
  public String getString() {
    return str;
  }

  @Override
  public RefedInterface2 setString(String str) {
    this.str = str;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    return ((RefedInterface2Impl) obj).str.equals(str);
  }

  @Override
  public String toString() {
    return "TestInterface2[str=" + str + "]";
  }
}
