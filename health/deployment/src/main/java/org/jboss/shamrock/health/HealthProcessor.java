package org.jboss.shamrock.health;

import java.util.Arrays;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
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

}
