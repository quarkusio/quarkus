package org.jboss.shamrock.health;

import java.util.Arrays;
import java.util.List;

import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.ShamrockConfig;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.health.runtime.HealthServlet;
import org.jboss.shamrock.undertow.ServletBuildItem;

import io.smallrye.health.SmallRyeHealthReporter;


class HealthProcessor {

    @BuildStep
    ServletBuildItem produceServlet(ShamrockConfig config) {
        ServletBuildItem servletBuildItem = new ServletBuildItem("health", HealthServlet.class.getName());
        servletBuildItem.getMappings().add(config.getConfig("health.path", "/health"));
        return servletBuildItem;
    }

    @BuildStep
    List<AdditionalBeanBuildItem> produceCdi() {
        return Arrays.asList(
                new AdditionalBeanBuildItem(SmallRyeHealthReporter.class),
                new AdditionalBeanBuildItem(HealthServlet.class));
    }

}
