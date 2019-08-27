/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.quarkus.netty.runtime.virtual;

import java.net.SocketAddress;

import io.netty.channel.Channel;

/**
 * An endpoint in the local transport. Each endpoint is identified by a unique
 * case-insensitive string.
 */
public final class VirtualAddress extends SocketAddress implements Comparable<VirtualAddress> {

    private static final long serialVersionUID = 4644331421130916435L;

    public static final VirtualAddress ANY = new VirtualAddress("ANY");

    private final String id;
    private final String strVal;

    /**
     * Creates a new ephemeral port based on the ID of the specified channel.
     * Note that we prepend an upper-case character so that it never conflicts with
     * the addresses created by a user, which are always lower-cased on construction time.
     */
    VirtualAddress(Channel channel) {
        StringBuilder buf = new StringBuilder(16);
        buf.append("local:E");
        buf.append(Long.toHexString(channel.hashCode() & 0xFFFFFFFFL | 0x100000000L));
        buf.setCharAt(7, ':');
        id = buf.substring(6);
        strVal = buf.toString();
    }

    /**
     * Creates a new instance with the specified ID.
     */
    public VirtualAddress(String id) {
        if (id == null) {
            throw new NullPointerException("id");
        }
        id = id.trim().toLowerCase();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("empty id");
        }
        this.id = id;
        strVal = "local:" + id;
    }

    /**
     * Returns the ID of this address.
     */
    public String id() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VirtualAddress)) {
            return false;
        }

        return id.equals(((VirtualAddress) o).id);
    }

    @Override
    public int compareTo(VirtualAddress o) {
        return id.compareTo(o.id);
    }

    @Override
    public String toString() {
        return strVal;
    }
}
