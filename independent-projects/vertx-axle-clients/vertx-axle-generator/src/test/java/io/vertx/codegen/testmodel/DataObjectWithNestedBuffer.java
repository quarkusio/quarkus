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
public class DataObjectWithNestedBuffer {

  private Buffer buffer;
  private DataObjectWithBuffer nested;
  private List<Buffer> buffers;

  public DataObjectWithNestedBuffer() {
  }

  public DataObjectWithNestedBuffer(JsonObject json) {
    byte[] buffer = json.getBinary("buffer");
    this.buffer = buffer != null ? Buffer.buffer(buffer) : null;
    JsonObject nested = json.getJsonObject("nested");
    this.nested = nested != null ? new DataObjectWithBuffer(nested) : null;
    JsonArray buffers_ = json.getJsonArray("buffers");
    if (buffers_ != null) {
      this.buffers = new ArrayList<>();
      for (int i = 0;i < buffers_.size();i++) {
        buffers.add(Buffer.buffer(buffers_.getBinary(i)));
      }
    }

  }

  public Buffer getBuffer() {
    return buffer;
  }

  public void setBuffer(Buffer buffer) {
    this.buffer = buffer;
  }

  public List<Buffer> getBuffers() {
    return buffers;
  }

  public void setBuffers(List<Buffer> buffers) {
    this.buffers = buffers;
  }

  public DataObjectWithBuffer getNested() {
    return nested;
  }

  public void setNested(DataObjectWithBuffer nested) {
    this.nested = nested;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (buffer != null) {
      json.put("buffer", buffer.getBytes());
    }
    if (buffers != null) {
      JsonArray arr = new JsonArray();
      for (Buffer b : buffers) {
        arr.add(b.getBytes());
      }
      json.put("buffers", arr);
    }
    if (nested != null) {
      json.put("nested", nested.toJson());
    }
    return json;
  }
}
