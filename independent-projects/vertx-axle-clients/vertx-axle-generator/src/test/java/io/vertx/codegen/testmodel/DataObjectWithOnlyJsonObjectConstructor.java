package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * @author <a href="mailto:oreilldf@gmail.com">Dan O'Reilly</a>
 */
@DataObject
public class DataObjectWithOnlyJsonObjectConstructor {
  private String foo;

  public DataObjectWithOnlyJsonObjectConstructor(JsonObject jsonObject) {
    this.foo = jsonObject.getString("foo");
  }

  public JsonObject toJson() {
    return new JsonObject().put("foo", foo);
  }

  public String getFoo() {
    return foo;
  }
}
