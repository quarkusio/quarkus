/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.quarkus.netty.runtime.virtual;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;

final class VirtualChannelRegistry {

    private static final ConcurrentMap<VirtualAddress, Channel> boundChannels = PlatformDependent.newConcurrentHashMap();

    static VirtualAddress register(
            Channel channel, VirtualAddress oldLocalAddress, SocketAddress localAddress) {
        if (oldLocalAddress != null) {
            throw new ChannelException("already bound");
        }
        if (!(localAddress instanceof VirtualAddress)) {
            throw new ChannelException("unsupported address type: " + StringUtil.simpleClassName(localAddress));
        }

        VirtualAddress addr = (VirtualAddress) localAddress;
        if (VirtualAddress.ANY.equals(addr)) {
            addr = new VirtualAddress(channel);
        }

        Channel boundChannel = boundChannels.putIfAbsent(addr, channel);
        if (boundChannel != null) {
            throw new ChannelException("address already in use by: " + boundChannel);
        }
        return addr;
    }

    static Channel get(SocketAddress localAddress) {
        return boundChannels.get(localAddress);
    }

    static void unregister(VirtualAddress localAddress) {
        boundChannels.remove(localAddress);
    }

    private VirtualChannelRegistry() {
        // Unused
    }
}
