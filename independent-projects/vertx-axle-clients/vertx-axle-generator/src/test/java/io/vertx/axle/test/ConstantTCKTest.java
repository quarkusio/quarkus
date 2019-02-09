package io.vertx.axle.test;

import io.vertx.axle.codegen.testmodel.ConstantTCK;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ConstantTCKTest {

  @Test
  public void testBasic() {
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BYTE, ConstantTCK.BYTE);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_BYTE, ConstantTCK.BOXED_BYTE);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.SHORT, ConstantTCK.SHORT);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_SHORT, ConstantTCK.BOXED_SHORT);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.INT, ConstantTCK.INT);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_INT, ConstantTCK.BOXED_INT);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.LONG, ConstantTCK.LONG);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_LONG, ConstantTCK.BOXED_LONG);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.FLOAT, ConstantTCK.FLOAT, 0.1);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_FLOAT, ConstantTCK.BOXED_FLOAT, 0.1);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.DOUBLE, ConstantTCK.DOUBLE, 0.1);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_DOUBLE, ConstantTCK.BOXED_DOUBLE, 0.1);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOOLEAN, ConstantTCK.BOOLEAN);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_BOOLEAN, ConstantTCK.BOXED_BOOLEAN);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.CHAR, ConstantTCK.CHAR);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_CHAR, ConstantTCK.BOXED_CHAR);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.STRING, ConstantTCK.STRING);
  }

  @Test
  public void testVertxGen() {
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.VERTX_GEN, ConstantTCK.VERTX_GEN.getDelegate());
  }

  @Test
  public void testJson() {
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.JSON_OBJECT, ConstantTCK.JSON_OBJECT);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.JSON_ARRAY, ConstantTCK.JSON_ARRAY);
  }

  @Test
  public void testDataObject() {
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.DATA_OBJECT, ConstantTCK.DATA_OBJECT);
  }

  @Test
  public void testEnum() {
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.ENUM, ConstantTCK.ENUM);
  }

  @Test
  public void testObject() {
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.OBJECT, ConstantTCK.OBJECT);
  }

  @Test
  public void testThrowable() {
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.THROWABLE, ConstantTCK.THROWABLE);
  }

  @Test
  public void testNullable() {
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.NULLABLE_NON_NULL, ConstantTCK.NULLABLE_NON_NULL.getDelegate());
    assertNull(ConstantTCK.NULLABLE_NULL);
  }

  @Test
  public void testList() {
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.BYTE_LIST, ConstantTCK.BYTE_LIST);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.SHORT_LIST, ConstantTCK.SHORT_LIST);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.INT_LIST, ConstantTCK.INT_LIST);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.LONG_LIST, ConstantTCK.LONG_LIST);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.FLOAT_LIST, ConstantTCK.FLOAT_LIST);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.DOUBLE_LIST, ConstantTCK.DOUBLE_LIST);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.CHAR_LIST, ConstantTCK.CHAR_LIST);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.STRING_LIST, ConstantTCK.STRING_LIST);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.VERTX_GEN_LIST.size(), ConstantTCK.VERTX_GEN_LIST.size());
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.VERTX_GEN_LIST.get(0), ConstantTCK.VERTX_GEN_LIST.get(0).getDelegate());
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.JSON_OBJECT_LIST, ConstantTCK.JSON_OBJECT_LIST);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.JSON_ARRAY_LIST, ConstantTCK.JSON_ARRAY_LIST);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.DATA_OBJECT_LIST, ConstantTCK.DATA_OBJECT_LIST);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.ENUM_LIST, ConstantTCK.ENUM_LIST);
  }

  @Test
  public void testSet() {
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.BYTE_SET, ConstantTCK.BYTE_SET);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.SHORT_SET, ConstantTCK.SHORT_SET);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.INT_SET, ConstantTCK.INT_SET);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.LONG_SET, ConstantTCK.LONG_SET);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.FLOAT_SET, ConstantTCK.FLOAT_SET);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.DOUBLE_SET, ConstantTCK.DOUBLE_SET);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.CHAR_SET, ConstantTCK.CHAR_SET);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.STRING_SET, ConstantTCK.STRING_SET);
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.VERTX_GEN_SET.size(), ConstantTCK.VERTX_GEN_SET.size());
    assertEquals(io.vertx.codegen.testmodel.ConstantTCK.VERTX_GEN_SET.iterator().next(), ConstantTCK.VERTX_GEN_SET.iterator().next().getDelegate());
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.JSON_OBJECT_SET, ConstantTCK.JSON_OBJECT_SET);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.JSON_ARRAY_SET, ConstantTCK.JSON_ARRAY_SET);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.DATA_OBJECT_SET, ConstantTCK.DATA_OBJECT_SET);
    assertSame(io.vertx.codegen.testmodel.ConstantTCK.ENUM_SET, ConstantTCK.ENUM_SET);
  }

  private <V> V checkMap(Map<String, V> map) {
    assertEquals(1, map.size());
    return map.get("foo");
  }

  @Test
  public void testMap() {
    assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.BYTE_MAP), ConstantTCK.BOXED_BYTE);
    assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.SHORT_MAP), ConstantTCK.BOXED_SHORT);
    assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.INT_MAP), ConstantTCK.BOXED_INT);
    assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.LONG_MAP), ConstantTCK.BOXED_LONG);
    assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.FLOAT_MAP), ConstantTCK.BOXED_FLOAT);
    assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.DOUBLE_MAP), ConstantTCK.BOXED_DOUBLE);
    assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.CHAR_MAP), ConstantTCK.BOXED_CHAR);
    assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.BOOLEAN_MAP), ConstantTCK.BOXED_BOOLEAN);
    assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.STRING_MAP), ConstantTCK.STRING);
    assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.JSON_OBJECT_MAP), ConstantTCK.JSON_OBJECT);
    assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.JSON_ARRAY_MAP), ConstantTCK.JSON_ARRAY);
  }
}
