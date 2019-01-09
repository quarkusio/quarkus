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

package org.jboss.shamrock.health;

import java.util.Arrays;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.health.runtime.HealthServlet;
import org.jboss.shamrock.undertow.ServletBuildItem;

import io.smallrye.health.SmallRyeHealthReporter;


class HealthProcessor {

    /**
     * The path to the health check servlet
     */
    @ConfigProperty(name = "shamrock.health.path", defaultValue = "/health")
    String path;


    @BuildStep
    ServletBuildItem produceServlet() {
        ServletBuildItem servletBuildItem = new ServletBuildItem("health", HealthServlet.class.getName());
        servletBuildItem.getMappings().add(path);
        return servletBuildItem;
    }

    @BuildStep
    List<AdditionalBeanBuildItem> produceCdi() {
        return Arrays.asList(
                new AdditionalBeanBuildItem(SmallRyeHealthReporter.class),
                new AdditionalBeanBuildItem(HealthServlet.class));
    }
    
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.MP_HEALTH);
    }

}
