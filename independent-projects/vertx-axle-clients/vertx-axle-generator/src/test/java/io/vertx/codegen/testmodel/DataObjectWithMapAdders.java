package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Instant;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@DataObject
public class DataObjectWithMapAdders {

  DataObjectWithMaps value = new DataObjectWithMaps();

  public DataObjectWithMapAdders() {
  }

  public DataObjectWithMapAdders(DataObjectWithMapAdders that) {
    throw new UnsupportedOperationException("not used");
  }

  public DataObjectWithMapAdders(JsonObject json) {
    value = new DataObjectWithMaps(json);
  }

  public DataObjectWithMapAdders addShortValue(String key, Short shortValue) {
    value.shortValues.put(key, shortValue);
    return this;
  }

  public DataObjectWithMapAdders addIntegerValue(String key, Integer integerValue) {
    value.integerValues.put(key, integerValue);
    return this;
  }

  public DataObjectWithMapAdders addLongValue(String key, Long longValue) {
    value.longValues.put(key, longValue);
    return this;
  }

  public DataObjectWithMapAdders addFloatValue(String key, Float floatValue) {
    value.floatValues.put(key, floatValue);
    return this;
  }

  public DataObjectWithMapAdders addDoubleValue(String key, Double doubleValue) {
    value.doubleValues.put(key, doubleValue);
    return this;
  }

  public DataObjectWithMapAdders addBooleanValue(String key, Boolean booleanValue) {
    value.booleanValues.put(key, booleanValue);
    return this;
  }

  public DataObjectWithMapAdders addStringValue(String key, String stringValue) {
    value.stringValues.put(key, stringValue);
    return this;
  }

  public DataObjectWithMapAdders addInstantValue(String key, Instant instantValue) {
    value.instantValues.put(key, instantValue);
    return this;
  }

  public DataObjectWithMapAdders addEnumValue(String key, TestEnum enumValue) {
    value.enumValues.put(key, enumValue);
    return this;
  }

  public DataObjectWithMapAdders addGenEnumValue(String key,TestGenEnum genEnumValue) {
    value.genEnumValues.put(key, genEnumValue);
    return this;
  }

  public DataObjectWithMapAdders addDataObjectValue(String key, TestDataObject dataObjectValue) {
    value.dataObjectValues.put(key, dataObjectValue);
    return this;
  }

  public DataObjectWithMapAdders addJsonObjectValue(String key, JsonObject jsonObjectValue) {
    value.jsonObjectValues.put(key, jsonObjectValue);
    return this;
  }

  public DataObjectWithMapAdders addJsonArrayValue(String key, JsonArray jsonArrayValue) {
    value.jsonArrayValues.put(key, jsonArrayValue);
    return this;
  }

  public JsonObject toJson() {
    return value.toJson();
  }
}
