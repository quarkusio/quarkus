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

package org.jboss.shamrock.infinispan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;
import org.infinispan.commons.util.Util;
import org.infinispan.protean.runtime.InfinispanClientProducer;
import org.infinispan.protean.runtime.InfinispanConfiguration;
import org.infinispan.protean.runtime.InfinispanTemplate;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Type;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.annotations.ExecutionTime;
import org.jboss.shamrock.deployment.annotations.Record;
import org.jboss.shamrock.arc.deployment.AdditionalBeanBuildItem;
import org.jboss.shamrock.arc.deployment.BeanContainerListenerBuildItem;
import org.jboss.shamrock.arc.deployment.UnremovableBeanBuildItem;
import org.jboss.shamrock.deployment.ApplicationArchive;
import org.jboss.shamrock.deployment.builditem.ApplicationArchivesBuildItem;
import org.jboss.shamrock.deployment.builditem.ApplicationIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.HotDeploymentConfigFileBuildItem;
import org.jboss.shamrock.deployment.builditem.SystemPropertyBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;

class InfinispanClientProcessor {
    private static final Log log = LogFactory.getLog(InfinispanClientProcessor.class);

    private static final String META_INF = "META-INF";
    private static final String HOTROD_CLIENT_PROPERTIES = META_INF + File.separator + "/hotrod-client.properties";
    private static final String PROTO_EXTENSION = ".proto";

    @BuildStep
    PropertiesBuildItem setup(ApplicationArchivesBuildItem applicationArchivesBuildItem,
          BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
          BuildProducer<HotDeploymentConfigFileBuildItem> hotDeployment,
          BuildProducer<SystemPropertyBuildItem> systemProperties,
          BuildProducer<AdditionalBeanBuildItem> additionalBeans,
          ApplicationIndexBuildItem applicationIndexBuildItem) throws ClassNotFoundException, IOException {

        additionalBeans.produce(new AdditionalBeanBuildItem(InfinispanClientProducer.class));
        systemProperties.produce(new SystemPropertyBuildItem("io.netty.noUnsafe", "true"));
        hotDeployment.produce(new HotDeploymentConfigFileBuildItem(HOTROD_CLIENT_PROPERTIES));

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream stream = cl.getResourceAsStream(HOTROD_CLIENT_PROPERTIES);
        Properties properties;
        if (stream == null) {
            properties = null;
            log.couldNotFindPropertiesFile(HOTROD_CLIENT_PROPERTIES);
        } else {
            try {
                properties = loadFromStream(stream);
                log.debugf("Found HotRod properties of %s",  properties);
            } finally {
                Util.close(stream);
            }

            InfinispanClientProducer.replaceProperties(properties);

            // We use caffeine for bounded near cache - so register that reflection if we have a bounded near cache
            if (properties.containsKey(ConfigurationProperties.NEAR_CACHE_MAX_ENTRIES)) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "com.github.benmanes.caffeine.cache.SSMS"));
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "com.github.benmanes.caffeine.cache.PSMS"));
            }

            Object marshaller = properties.get(ConfigurationProperties.MARSHALLER);

            if (InfinispanClientProducer.isProtoBufAvailable(marshaller)) {
               ApplicationArchive applicationArchive = applicationArchivesBuildItem.getRootArchive();
               // If we have properties file we may have to care about
               Path metaPath = applicationArchive.getChildPath(META_INF);

               Iterator<Path> protoFiles = Files.list(metaPath)
                     .filter(Files::isRegularFile)
                     .filter(p -> p.toString().endsWith(PROTO_EXTENSION))
                     .iterator();
               // We monitor the entire meta inf directory if properties are available
               if (protoFiles.hasNext()) {
                  // Protean doesn't currently support hot deployment watching directories
//                hotDeployment.produce(new HotDeploymentConfigFileBuildItem(META_INF));
               }

               while (protoFiles.hasNext()) {
                  Path path = protoFiles.next();
                  byte[] bytes = Files.readAllBytes(path);
                  // This uses the default file encoding - should we enforce UTF-8?
                  properties.put(InfinispanClientProducer.PROTOBUF_FILE_PREFIX + path.getFileName().toString(),
                        new String(bytes));
               }

               InfinispanClientProducer.handleQueryRequirements(properties);
            }
        }

        // Add any user project listeners to allow reflection in native code
        Index index = applicationIndexBuildItem.getIndex();
        List<AnnotationInstance> listenerInstances = index.getAnnotations(
              DotName.createSimple("org.infinispan.client.hotrod.annotation.ClientListener"));
        for (AnnotationInstance instance : listenerInstances) {
            AnnotationTarget target = instance.target();
            if (target.kind() == AnnotationTarget.Kind.CLASS) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, target.asClass().name().toString()));
            }
        }

        // This is required for netty to work properly
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "io.netty.channel.socket.nio.NioSocketChannel"));
        // We use reflection to have continuous queries work
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "org.infinispan.client.hotrod.event.ContinuousQueryImpl$ClientEntryListener"));
        // We use reflection to allow for near cache invalidations
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "org.infinispan.client.hotrod.near.NearCacheService$InvalidatedNearCacheListener"));
        // This is required when a cache is clustered to tell us topology
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "org.infinispan.client.hotrod.impl.consistenthash.SegmentConsistentHash"));

        return new PropertiesBuildItem(properties);
    }

    private Properties loadFromStream(InputStream stream) {
        Properties properties = new Properties();
        try {
            properties.load(stream);
        } catch (IOException e) {
            throw new HotRodClientException("Issues configuring from client hotrod-client.properties", e);
        }
        return properties;
    }


    /**
     * The Infinispan client configuration, if set.
     */
    InfinispanConfiguration infinispanClient;

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    BeanContainerListenerBuildItem build(InfinispanTemplate template, PropertiesBuildItem builderBuildItem) {
        Properties properties = builderBuildItem.getProperties();
        InfinispanConfiguration conf = infinispanClient;
        final Optional<String> serverList = conf.serverList;
        if (serverList.isPresent()) {
            log.debugf("Applying micro profile configuration on top of hotrod properties: %s", conf);
            if (properties == null) {
                properties = new Properties();
            }
            properties.put(ConfigurationProperties.SERVER_LIST, serverList);
        }
        return new BeanContainerListenerBuildItem(template.configureInfinispan(properties));
    }

    private static final Set<DotName> UNREMOVABLE_BEANS = Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(
                DotName.createSimple("org.infinispan.protostream.MessageMarshaller"),
                DotName.createSimple("org.infinispan.protostream.FileDescriptorSource")
          )));

    @BuildStep
    UnremovableBeanBuildItem ensureBeanLookupAvailable() {
        return new UnremovableBeanBuildItem(beanInfo -> {
                Set<Type> types = beanInfo.getTypes();
                for (Type t : types) {
                    if (UNREMOVABLE_BEANS.contains(t.name())) {
                        return true;
                    }
                }

                return false;
        });
    }
}
