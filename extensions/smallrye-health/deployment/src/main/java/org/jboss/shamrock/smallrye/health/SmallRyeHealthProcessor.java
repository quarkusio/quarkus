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

package org.jboss.shamrock.smallrye.health;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;
import org.jboss.shamrock.arc.deployment.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.annotations.ExecutionTime;
import org.jboss.shamrock.deployment.annotations.Record;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.recording.RecorderContext;
import org.jboss.shamrock.deployment.util.ServiceUtil;
import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;
import org.jboss.shamrock.smallrye.health.runtime.SmallRyeHealthServlet;
import org.jboss.shamrock.smallrye.health.runtime.SmallRyeHealthTemplate;
import org.jboss.shamrock.undertow.ServletBuildItem;

import io.smallrye.health.SmallRyeHealthReporter;

class SmallRyeHealthProcessor {

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
    ServletBuildItem produceServlet() {
        ServletBuildItem servletBuildItem = new ServletBuildItem("health", SmallRyeHealthServlet.class.getName());
        servletBuildItem.getMappings().add(health.path);
        return servletBuildItem;
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans() {
        return Arrays.asList(
                new AdditionalBeanBuildItem(SmallRyeHealthReporter.class),
                new AdditionalBeanBuildItem(SmallRyeHealthServlet.class));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    @SuppressWarnings("unchecked")
    void registerHealthCheckResponseProvider(SmallRyeHealthTemplate template, RecorderContext recorder) throws IOException {
        Set<String> providers = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(), "META-INF/services/" + HealthCheckResponseProvider.class.getName());

        if (providers.isEmpty()) {
            throw new IllegalStateException("No HealthCheckResponseProvider implementation found.");
        } else if (providers.size() > 1) {
            throw new IllegalStateException(String.format("Multiple HealthCheckResponseProvider implementations found: %s", providers));
        }

        template.registerHealthCheckResponseProvider((Class<? extends HealthCheckResponseProvider>) recorder.classProxy(providers.iterator().next()));
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SMALLRYE_HEALTH);
    }

}
