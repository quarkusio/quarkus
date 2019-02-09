package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@DataObject
public class DataObjectWithLists {

  private static <T> JsonArray toArray(List<T> list) {
    JsonArray array = new JsonArray();
    array.getList().addAll(list);
    return array;
  }

  private static <T> List<T> fromArray(JsonObject obj, String name) {
    return fromArray(obj, name, o->(T)o);
  }

  private static <T> List<T> fromArray(JsonObject obj, String name, Function<Object, T> converter) {
    JsonArray array = obj.getJsonArray(name);
    if (array != null) {
      return array.stream().map(converter).collect(Collectors.toList());
    } else {
      return null;
    }
  }

  List<Short> shortValues = new ArrayList<>();
  List<Integer> integerValues = new ArrayList<>();
  List<Long> longValues = new ArrayList<>();
  List<Float> floatValues = new ArrayList<>();
  List<Double> doubleValues = new ArrayList<>();
  List<Boolean> booleanValues = new ArrayList<>();
  List<String> stringValues = new ArrayList<>();
  List<Instant> instantValues = new ArrayList<>();
  List<JsonObject> jsonObjectValues = new ArrayList<>();
  List<JsonArray> jsonArrayValues = new ArrayList<>();
  List<TestDataObject> dataObjectValues = new ArrayList<>();
  List<TestEnum> enumValues = new ArrayList<>();
  List<TestGenEnum> genEnumValues = new ArrayList<>();

  public DataObjectWithLists() {
  }

  public DataObjectWithLists(DataObjectWithLists that) {
    throw new UnsupportedOperationException("not used");
  }

  public DataObjectWithLists(JsonObject json) {
    booleanValues = fromArray(json, "booleanValues");
    shortValues = fromArray(json, "shortValues", o -> Short.parseShort(o.toString()));
    integerValues = fromArray(json, "integerValues");
    longValues = fromArray(json, "longValues", o -> Long.parseLong(o.toString()));
    floatValues = fromArray(json, "floatValues", o -> Float.parseFloat(o.toString()));
    doubleValues = fromArray(json, "doubleValues");
    stringValues = fromArray(json, "stringValues");
    instantValues = fromArray(json, "instantValues", o -> Instant.parse(o.toString()));
    jsonObjectValues = fromArray(json, "jsonObjectValues", o -> (JsonObject) o);
    jsonArrayValues = fromArray(json, "jsonArrayValues", o -> (JsonArray) o);
    dataObjectValues = fromArray(json, "dataObjectValues", o -> new TestDataObject((JsonObject) o));
    enumValues = fromArray(json, "enumValues", o -> TestEnum.valueOf(o.toString()));
    genEnumValues = fromArray(json, "genEnumValues", o -> TestGenEnum.valueOf(o.toString()));
  }

  public DataObjectWithLists setShortValues(List<Short> shortValues) {
    this.shortValues = shortValues;
    return this;
  }

  public DataObjectWithLists setIntegerValues(List<Integer> integerValues) {
    this.integerValues = integerValues;
    return this;
  }

  public DataObjectWithLists setLongValues(List<Long> longValues) {
    this.longValues = longValues;
    return this;
  }

  public DataObjectWithLists setFloatValues(List<Float> floatValues) {
    this.floatValues = floatValues;
    return this;
  }

  public DataObjectWithLists setDoubleValues(List<Double> doubleValues) {
    this.doubleValues = doubleValues;
    return this;
  }

  public DataObjectWithLists setBooleanValues(List<Boolean> booleanValues) {
    this.booleanValues = booleanValues;
    return this;
  }

  public DataObjectWithLists setStringValues(List<String> stringValue) {
    this.stringValues = stringValue;
    return this;
  }

  public DataObjectWithLists setInstantValues(List<Instant> instantValues) {
    this.instantValues = instantValues;
    return this;
  }

  public DataObjectWithLists setEnumValues(List<TestEnum> enumValues) {
    this.enumValues = enumValues;
    return this;
  }

  public DataObjectWithLists setGenEnumValues(List<TestGenEnum> genEnumValues) {
    this.genEnumValues = genEnumValues;
    return this;
  }

  public DataObjectWithLists setDataObjectValues(List<TestDataObject> dataObjectValues) {
    this.dataObjectValues = dataObjectValues;
    return this;
  }

  public DataObjectWithLists setJsonObjectValues(List<JsonObject> jsonObjectValues) {
    this.jsonObjectValues = jsonObjectValues;
    return this;
  }

  public DataObjectWithLists setJsonArrayValues(List<JsonArray> jsonArrayValues) {
    this.jsonArrayValues = jsonArrayValues;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (booleanValues != null) {
      json.put("booleanValues", toArray(booleanValues));
    }
    if (shortValues != null) {
      json.put("shortValues", toArray(shortValues));
    }
    if (integerValues != null) {
      json.put("integerValues", toArray(integerValues));
    }
    if (longValues != null) {
      json.put("longValues", toArray(longValues));
    }
    if (floatValues != null) {
      json.put("floatValues", toArray(floatValues));
    }
    if (doubleValues != null) {
      json.put("doubleValues", toArray(doubleValues));
    }
    if (stringValues != null) {
      json.put("stringValues", toArray(stringValues));
    }
    if (instantValues != null) {
      json.put("instantValues", toArray(instantValues.stream().map(Instant::toString).collect(Collectors.toList())));
    }
    if (jsonObjectValues != null) {
      json.put("jsonObjectValues", toArray(jsonObjectValues));
    }
    if (jsonArrayValues != null) {
      json.put("jsonArrayValues", toArray(jsonArrayValues));
    }
    if (dataObjectValues != null) {
      json.put("dataObjectValues", toArray(dataObjectValues.stream().map(o -> o.toJson().getMap()).collect(Collectors.toList())));
    }
    if (enumValues != null) {
      json.put("enumValues", toArray(enumValues));
    }
    if (genEnumValues != null) {
      json.put("genEnumValues", toArray(genEnumValues));
    }
    return json;
  }
}
