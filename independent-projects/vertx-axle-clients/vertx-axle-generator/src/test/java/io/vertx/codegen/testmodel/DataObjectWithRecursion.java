package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@DataObject
public class DataObjectWithRecursion {

  private String data;
  private DataObjectWithRecursion next;

  public DataObjectWithRecursion(JsonObject json) {
    data = json.getString("data");
    if (json.getJsonObject("next") != null) {
      next  = new DataObjectWithRecursion(json.getJsonObject("next"));
    }
  }

  public String getData() {
    return data;
  }

  public DataObjectWithRecursion setData(String data) {
    this.data = data;
    return this;
  }

  public DataObjectWithRecursion getNext() {
    return next;
  }

  public DataObjectWithRecursion setNext(DataObjectWithRecursion next) {
    this.next = next;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (data != null) {
      json.put("data", data);
    }
    if (next != null) {
      json.put("next", next.toJson());
    }
    return json;
  }
}
