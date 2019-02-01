package io.vertx.axle.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.vertx.core.buffer.Buffer;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BufferTest {

  ObjectMapper mapper;

  @Before
  public void setUp() throws Exception {
    mapper = new ObjectMapper(new YAMLFactory());
  }

  @Test
  public void testClusterSerializable() throws Exception {
    io.vertx.axle.core.buffer.Buffer buff = io.vertx.axle.core.buffer.Buffer.buffer("hello-world");
    Buffer actual = Buffer.buffer();
    buff.writeToBuffer(actual);
    Buffer expected = Buffer.buffer();
    Buffer.buffer("hello-world").writeToBuffer(expected);
    assertEquals(expected, actual);
    buff = io.vertx.axle.core.buffer.Buffer.buffer("hello-world");
    assertEquals(expected.length(), buff.readFromBuffer(0, expected));
    assertEquals("hello-world", buff.toString());
  }
}
