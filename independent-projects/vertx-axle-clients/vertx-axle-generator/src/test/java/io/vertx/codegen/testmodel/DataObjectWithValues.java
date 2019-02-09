package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Instant;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@DataObject
public class DataObjectWithValues {

  short shortValue;
  int intValue;
  long longValue;
  float floatValue;
  double doubleValue;
  boolean booleanValue;
  Short boxedShortValue;
  Integer boxedIntValue;
  Long boxedLongValue;
  Float boxedFloatValue;
  Double boxedDoubleValue;
  Boolean boxedBooleanValue;
  String stringValue;
  Instant instantValue;
  JsonObject jsonObjectValue;
  JsonArray jsonArrayValue;
  TestDataObject dataObjectValue;
  TestEnum enumValue;
  TestGenEnum genEnumValue;

  public DataObjectWithValues() {
  }

  public DataObjectWithValues(DataObjectWithValues that) {
    throw new UnsupportedOperationException("not used");
  }

  public DataObjectWithValues(JsonObject json) {
    booleanValue = json.getBoolean("booleanValue", false);
    shortValue = (short)(int)json.getInteger("shortValue", -1);
    intValue = json.getInteger("intValue", -1);
    longValue = json.getLong("longValue", -1L);
    floatValue = json.getFloat("floatValue", -1f);
    doubleValue = json.getDouble("doubleValue", -1d);
    boxedBooleanValue = json.getBoolean("boxedBooleanValue", null);
    boxedShortValue = json.getInteger("boxedShortValue") != null ? (short)(int)json.getInteger("boxedShortValue") : null;
    boxedIntValue = json.getInteger("boxedIntValue", null);
    boxedLongValue = json.getLong("boxedLongValue", null);
    boxedFloatValue = json.getFloat("boxedFloatValue", null);
    boxedDoubleValue = json.getDouble("boxedDoubleValue", null);
    stringValue = json.getString("stringValue");
    instantValue = json.getInstant("instantValue");
    jsonObjectValue = json.getJsonObject("jsonObjectValue");
    jsonArrayValue = json.getJsonArray("jsonArrayValue");
    dataObjectValue = json.getJsonObject("dataObjectValue") != null ? new TestDataObject(json.getJsonObject("dataObjectValue")) : null;
    enumValue = json.getString("enumValue") != null ? TestEnum.valueOf(json.getString("enumValue")) : null;
    genEnumValue = json.getString("genEnumValue") != null ? TestGenEnum.valueOf(json.getString("genEnumValue")) : null;
  }

  public DataObjectWithValues setShortValue(short shortValue) {
    this.shortValue = shortValue;
    return this;
  }

  public DataObjectWithValues setIntValue(int intValue) {
    this.intValue = intValue;
    return this;
  }

  public DataObjectWithValues setLongValue(long longValue) {
    this.longValue = longValue;
    return this;
  }

  public DataObjectWithValues setFloatValue(float floatValue) {
    this.floatValue = floatValue;
    return this;
  }

  public DataObjectWithValues setDoubleValue(double doubleValue) {
    this.doubleValue = doubleValue;
    return this;
  }

  public DataObjectWithValues setBooleanValue(boolean booleanValue) {
    this.booleanValue = booleanValue;
    return this;
  }

  public void setBoxedShortValue(Short boxedShortValue) {
    this.boxedShortValue = boxedShortValue;
  }

  public void setBoxedIntValue(Integer boxedIntValue) {
    this.boxedIntValue = boxedIntValue;
  }

  public void setBoxedLongValue(Long boxedLongValue) {
    this.boxedLongValue = boxedLongValue;
  }

  public void setBoxedFloatValue(Float boxedFloatValue) {
    this.boxedFloatValue = boxedFloatValue;
  }

  public void setBoxedDoubleValue(Double boxedDoubleValue) {
    this.boxedDoubleValue = boxedDoubleValue;
  }

  public void setBoxedBooleanValue(Boolean boxedBooleanValue) {
    this.boxedBooleanValue = boxedBooleanValue;
  }

  public DataObjectWithValues setStringValue(String stringValue) {
    this.stringValue = stringValue;
    return this;
  }

  public DataObjectWithValues setInstantValue(Instant instantValue) {
    this.instantValue = instantValue;
    return this;
  }

  public DataObjectWithValues setJsonObjectValue(JsonObject jsonObjectValue) {
    this.jsonObjectValue = jsonObjectValue;
    return this;
  }

  public DataObjectWithValues setJsonArrayValue(JsonArray jsonArrayValue) {
    this.jsonArrayValue = jsonArrayValue;
    return this;
  }

  public DataObjectWithValues setDataObjectValue(TestDataObject dataObjectValue) {
    this.dataObjectValue = dataObjectValue;
    return this;
  }

  public DataObjectWithValues setEnumValue(TestEnum enumValue) {
    this.enumValue = enumValue;
    return this;
  }

  public DataObjectWithValues setGenEnumValue(TestGenEnum genEnumValue) {
    this.genEnumValue = genEnumValue;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (boxedBooleanValue != null) {
      json.put("boxedBooleanValue", boxedBooleanValue);
    }
    if (boxedShortValue != null) {
      json.put("boxedShortValue", (int)boxedShortValue);
    }
    if (boxedIntValue != null) {
      json.put("boxedIntValue", boxedIntValue);
    }
    if (boxedLongValue != null) {
      json.put("boxedLongValue", boxedLongValue);
    }
    if (boxedFloatValue != null) {
      json.put("boxedFloatValue", boxedFloatValue);
    }
    if (boxedDoubleValue != null) {
      json.put("boxedDoubleValue", boxedDoubleValue);
    }
    if (stringValue != null) {
      json.put("stringValue", stringValue);
    }
    if (instantValue != null) {
      json.put("instantValue", instantValue);
    }
    if (jsonObjectValue != null) {
      json.put("jsonObjectValue", jsonObjectValue);
    }
    if (jsonArrayValue != null) {
      json.put("jsonArrayValue", jsonArrayValue);
    }
    if (jsonArrayValue != null) {
      json.put("jsonArrayValue", jsonArrayValue);
    }
    if (enumValue != null) {
      json.put("enumValue", enumValue.name());
    }
    if (genEnumValue != null) {
      json.put("genEnumValue", genEnumValue.name());
    }
    if (dataObjectValue != null) {
      json.put("dataObjectValue", dataObjectValue.toJson());
    }
    return json.
        put("booleanValue", booleanValue).
        put("shortValue", (int)shortValue).
        put("intValue", intValue).
        put("longValue", longValue).
        put("floatValue", floatValue).
        put("doubleValue", doubleValue);
  }
}
