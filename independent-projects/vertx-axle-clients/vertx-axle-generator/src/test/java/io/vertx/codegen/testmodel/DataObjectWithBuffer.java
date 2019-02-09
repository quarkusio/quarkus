package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@DataObject
public class DataObjectWithBuffer {

  private Buffer buffer;

  public DataObjectWithBuffer() {
  }

  public DataObjectWithBuffer(JsonObject json) {
    byte[] buffer = json.getBinary("buffer");
    this.buffer = buffer != null ? Buffer.buffer(buffer) : null;

  }

  public Buffer getBuffer() {
    return buffer;
  }

  public void setBuffer(Buffer buffer) {
    this.buffer = buffer;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (buffer != null) {
      json.put("buffer", buffer.getBytes());
    }
    return json;
  }
}
