/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.axle.test;

import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.axle.codegen.extra.AnotherInterface;
import io.vertx.axle.core.buffer.Buffer;
import io.vertx.axle.core.net.SocketAddress;
import org.junit.Test;

import java.util.stream.Stream;

import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

/**
 * @author Thomas Segismont
 */
public class EqualityTest {

  @Test
  public void testBufferEquality() {
    Buffer buf1 = Buffer.buffer("The quick brown fox jumps over the lazy dog");
    Buffer buf2 = buf1.copy();
    assertNotSame(buf1, buf2);
    assertEquals(buf1, buf2);
  }

  @Test
  public void testBufferSet() {
    Buffer buf1 = Buffer.buffer("The quick brown fox jumps over the lazy dog");
    Buffer buf2 = buf1.copy();
    assertEquals(1, Stream.of(buf1, buf2).collect(toSet()).size());
  }

  @Test
  public void testSocketAddressEquality() {
    SocketAddress address1 = SocketAddress.newInstance(new SocketAddressImpl(8888, "guest"));
    SocketAddress address2 = SocketAddress.newInstance(new SocketAddressImpl(8888, "guest"));
    assertNotSame(address1, address2);
    assertEquals(address1, address2);
  }

  @Test
  public void testSocketAddressSet() {
    SocketAddress address1 = SocketAddress.newInstance(new SocketAddressImpl(8888, "guest"));
    SocketAddress address2 = SocketAddress.newInstance(new SocketAddressImpl(8888, "guest"));
    assertEquals(1, Stream.of(address1, address2).collect(toSet()).size());
  }

  @Test
  public void testAnotherInterfaceEquality() {
    AnotherInterface ai1 = AnotherInterface.create();
    AnotherInterface ai2 = AnotherInterface.create();
    assertNotSame(ai1, ai2);
    assertNotEquals(ai1, ai2);
  }

  @Test
  public void testAnotherInterfaceSet() {
    AnotherInterface ai1 = AnotherInterface.create();
    AnotherInterface ai2 = AnotherInterface.create();
    assertEquals(2, Stream.of(ai1, ai2).collect(toSet()).size());
  }
}
