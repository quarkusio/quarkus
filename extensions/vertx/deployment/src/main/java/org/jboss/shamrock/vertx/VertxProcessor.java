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

package org.jboss.shamrock.vertx;

import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.ExecutionTime;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.arc.deployment.AdditionalBeanBuildItem;
import org.jboss.shamrock.arc.deployment.BeanContainerListenerBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateConfigBuildItem;
import org.jboss.shamrock.vertx.runtime.VertxConfiguration;
import org.jboss.shamrock.vertx.runtime.VertxProducer;
import org.jboss.shamrock.vertx.runtime.VertxTemplate;

class VertxProcessor {

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @BuildStep
    SubstrateConfigBuildItem build() {

        // This one may not be required after Vert.x 3.6.0 lands
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "io.netty.channel.socket.nio.NioSocketChannel"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "java.util.LinkedHashMap"));

        return SubstrateConfigBuildItem.builder()
                .addNativeImageSystemProperty("io.netty.noUnsafe", "true")
                .addNativeImageSystemProperty("vertx.disableDnsResolver", "true")
                .addRuntimeReinitializedClass("io.netty.handler.codec.http2.Http2CodecUtil")
                .addRuntimeInitializedClass("io.netty.handler.codec.http.HttpObjectEncoder")
                .addRuntimeInitializedClass("io.netty.handler.codec.http2.DefaultHttp2FrameWriter")
                .addRuntimeInitializedClass("io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder")
                .addRuntimeInitializedClass("io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator")
                .addRuntimeInitializedClass("io.netty.handler.ssl.ReferenceCountedOpenSslEngine")
                .addNativeImageSystemProperty("io.netty.leakDetection.level", "DISABLED") //TODO: make configurable
                .build();
    }


    /**
     * The Vert.x configuration, if set.
     */
    @ConfigProperty(name = "shamrock.vertx")
    Optional<VertxConfiguration> vertxConfig;

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return new AdditionalBeanBuildItem(VertxProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    BeanContainerListenerBuildItem build(VertxTemplate template, BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.VERTX));
        return vertxConfig
                .map(conf -> new BeanContainerListenerBuildItem(template.configureVertx(conf)))
                .orElseGet(() -> new BeanContainerListenerBuildItem(template.configureVertx(null)));
    }






}
