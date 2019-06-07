package io.quarkus.camel.component.servlet.deployment;

import java.io.IOException;
import java.util.Map.Entry;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.camel.servlet.runtime.CamelServletConfig;
import io.quarkus.camel.servlet.runtime.CamelServletConfig.ServletConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem.Builder;

class CamelServletProcessor {

    CamelServletConfig camelServletConfig;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.CAMEL_SERVLET);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void build(BuildProducer<ServletBuildItem> servlet, BuildProducer<AdditionalBeanBuildItem> additionalBean)
            throws IOException {

        boolean servletCreated = false;
        if (camelServletConfig.servlet.defaultServlet.isValid()) {
            servlet.produce(
                    newServlet(ServletConfig.DEFAULT_SERVLET_NAME, camelServletConfig.servlet.defaultServlet, additionalBean));
            servletCreated = true;
        }

        for (Entry<String, ServletConfig> e : camelServletConfig.servlet.namedServlets.entrySet()) {
            if (ServletConfig.DEFAULT_SERVLET_NAME.equals(e.getKey())) {
                throw new IllegalStateException(
                        String.format("Use quarkus.camel.servlet.urlPatterns instead of quarkus.camel.servlet.%s.urlPatterns",
                                ServletConfig.DEFAULT_SERVLET_NAME));
            }
            servlet.produce(newServlet(e.getKey(), e.getValue(), additionalBean));
            servletCreated = true;
        }

        if (!servletCreated) {
            throw new IllegalStateException(
                    String.format(
                            "Map at least one servlet to a path using quarkus.camel.servlet.urlPatterns or quarkus.camel.servlet.[your-servlet-name].urlPatterns",
                            ServletConfig.DEFAULT_SERVLET_NAME));
        }

    }

    static ServletBuildItem newServlet(final String key, final ServletConfig servletConfig,
            final BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        final String servletName = servletConfig.getEffectiveServletName(key);
        if (servletConfig.urlPatterns.isEmpty()) {
            throw new IllegalStateException(String.format("Missing quarkus.camel.servlet%s.url-patterns",
                    ServletConfig.DEFAULT_SERVLET_NAME.equals(servletName) ? "" : "." + servletName));
        }
        final Builder builder = ServletBuildItem.builder(servletName, servletConfig.servletClass);
        additionalBean.produce(new AdditionalBeanBuildItem(servletConfig.servletClass));
        for (String pattern : servletConfig.urlPatterns) {
            builder.addMapping(pattern);
        }
        return builder.build();
    }

}
