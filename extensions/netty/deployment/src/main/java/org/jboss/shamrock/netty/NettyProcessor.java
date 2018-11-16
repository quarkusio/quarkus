/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.netty;

import javax.inject.Inject;

import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateConfigBuildItem;

class NettyProcessor {

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @BuildStep
    SubstrateConfigBuildItem build() {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "io.netty.channel.socket.nio.NioSocketChannel"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "io.netty.channel.socket.nio.NioServerSocketChannel"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "java.util.LinkedHashMap"));


        SubstrateConfigBuildItem.Builder builder = SubstrateConfigBuildItem.builder()
                .addNativeImageSystemProperty("io.netty.noUnsafe", "true")
                .addRuntimeInitializedClass("io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator")
                .addRuntimeInitializedClass("io.netty.handler.ssl.ReferenceCountedOpenSslEngine")
                .addRuntimeInitializedClass("io.netty.handler.ssl.util.ThreadLocalInsecureRandom")
                .addRuntimeInitializedClass("io.netty.channel.unix.IovArray")
                .addRuntimeInitializedClass("io.netty.channel.epoll.Native")
                .addRuntimeInitializedClass("io.netty.channel.unix.Errors")
                .addRuntimeInitializedClass("io.netty.channel.unix.FileDescriptor")
                .addNativeImageSystemProperty("io.netty.leakDetection.level", "DISABLED");//TODO: make configurable
        try {
            Class.forName("io.netty.handler.codec.http2.Http2CodecUtil");
            builder
                    .addRuntimeReinitializedClass("io.netty.handler.codec.http2.Http2CodecUtil")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.HttpObjectEncoder")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http2.DefaultHttp2FrameWriter")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder");

        } catch (ClassNotFoundException e) {

        }

        return builder
                .build();
    }

}
