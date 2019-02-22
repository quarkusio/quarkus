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

package io.quarkus.smallrye.health;

import java.io.IOException;
import java.util.Set;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthServlet;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthTemplate;
import io.quarkus.undertow.ServletBuildItem;
import io.smallrye.health.SmallRyeHealthReporter;

class SmallRyeHealthProcessor {

    private static final DotName HEALTH = DotName.createSimple(Health.class.getName());

    /**
     * The configuration for health checking.
     */
    SmallRyeHealthConfig health;

    @ConfigRoot(name = "smallrye-health")
    static final class SmallRyeHealthConfig {
        /**
         * The path of the health-checking servlet.
         */
        @ConfigItem(defaultValue = "/health")
        String path;
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    @SuppressWarnings("unchecked")
    void build(SmallRyeHealthTemplate template, RecorderContext recorder,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ServletBuildItem> servlet,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotation) throws IOException {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_HEALTH));

        // Register the servlet
        ServletBuildItem servletBuildItem = new ServletBuildItem("health", SmallRyeHealthServlet.class.getName());
        servletBuildItem.getMappings().add(health.path);
        servlet.produce(servletBuildItem);

        // Make ArC discover the beans marked with the @Health qualifier
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(HEALTH));

        // Add additional beans
        additionalBean.produce(new AdditionalBeanBuildItem(SmallRyeHealthReporter.class, SmallRyeHealthServlet.class));

        // Discover and register the HealthCheckResponseProvider
        Set<String> providers = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                "META-INF/services/" + HealthCheckResponseProvider.class.getName());

        if (providers.isEmpty()) {
            throw new IllegalStateException("No HealthCheckResponseProvider implementation found.");
        } else if (providers.size() > 1) {
            throw new IllegalStateException(
                    String.format("Multiple HealthCheckResponseProvider implementations found: %s", providers));
        }

        template.registerHealthCheckResponseProvider(
                (Class<? extends HealthCheckResponseProvider>) recorder.classProxy(providers.iterator().next()));
    }
}
