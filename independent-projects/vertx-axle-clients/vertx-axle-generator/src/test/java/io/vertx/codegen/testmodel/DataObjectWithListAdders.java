package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Instant;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@DataObject
public class DataObjectWithListAdders {

  DataObjectWithLists value = new DataObjectWithLists();

  public DataObjectWithListAdders() {
  }

  public DataObjectWithListAdders(DataObjectWithListAdders that) {
    throw new UnsupportedOperationException("not used");
  }

  public DataObjectWithListAdders(JsonObject json) {
    value = new DataObjectWithLists(json);
  }

  public DataObjectWithListAdders addShortValue(Short shortValue) {
    value.shortValues.add(shortValue);
    return this;
  }

  public DataObjectWithListAdders addIntegerValue(Integer integerValue) {
    value.integerValues.add(integerValue);
    return this;
  }

  public DataObjectWithListAdders addLongValue(Long longValue) {
    value.longValues.add(longValue);
    return this;
  }

  public DataObjectWithListAdders addFloatValue(Float floatValue) {
    value.floatValues.add(floatValue);
    return this;
  }

  public DataObjectWithListAdders addDoubleValue(Double doubleValue) {
    value.doubleValues.add(doubleValue);
    return this;
  }

  public DataObjectWithListAdders addBooleanValue(Boolean booleanValue) {
    value.booleanValues.add(booleanValue);
    return this;
  }

  public DataObjectWithListAdders addStringValue(String stringValue) {
    value.stringValues.add(stringValue);
    return this;
  }

  public DataObjectWithListAdders addInstantValue(Instant instantValue) {
    value.instantValues.add(instantValue);
    return this;
  }

  public DataObjectWithListAdders addEnumValue(TestEnum enumValue) {
    value.enumValues.add(enumValue);
    return this;
  }

  public DataObjectWithListAdders addGenEnumValue(TestGenEnum genEnumValue) {
    value.genEnumValues.add(genEnumValue);
    return this;
  }

  public DataObjectWithListAdders addDataObjectValue(TestDataObject dataObjectValue) {
    value.dataObjectValues.add(dataObjectValue);
    return this;
  }

  public DataObjectWithListAdders addJsonObjectValue(JsonObject jsonObjectValue) {
    value.jsonObjectValues.add(jsonObjectValue);
    return this;
  }

  public DataObjectWithListAdders addJsonArrayValue(JsonArray jsonArrayValue) {
    value.jsonArrayValues.add(jsonArrayValue);
    return this;
  }

  public JsonObject toJson() {
    return value.toJson();
  }
}
