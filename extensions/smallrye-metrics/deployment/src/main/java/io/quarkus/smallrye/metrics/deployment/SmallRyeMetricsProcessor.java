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

package io.quarkus.smallrye.metrics.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.smallrye.metrics.runtime.SmallRyeMetricsServlet;
import io.quarkus.smallrye.metrics.runtime.SmallRyeMetricsTemplate;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.smallrye.metrics.MetricProducer;
import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.MetricsRequestHandler;
import io.smallrye.metrics.interceptors.CountedInterceptor;
import io.smallrye.metrics.interceptors.MeteredInterceptor;
import io.smallrye.metrics.interceptors.MetricNameFactory;
import io.smallrye.metrics.interceptors.MetricsBinding;
import io.smallrye.metrics.interceptors.MetricsInterceptor;
import io.smallrye.metrics.interceptors.TimedInterceptor;

public class SmallRyeMetricsProcessor {

    private static final Set<DotName> metricsAnnotations = new HashSet<>(Arrays.asList(
            DotName.createSimple(Gauge.class.getName()),
            DotName.createSimple(Counted.class.getName()),
            DotName.createSimple(Timed.class.getName()),
            DotName.createSimple(Metered.class.getName())));

    @ConfigRoot(name = "smallrye-metrics")
    static final class SmallRyeMetricsConfig {

        /**
         * The path to the metrics Servlet.
         */
        @ConfigItem(defaultValue = "/metrics")
        String path;
    }

    SmallRyeMetricsConfig metrics;

    @BuildStep
    ServletBuildItem createServlet() {
        ServletBuildItem servletBuildItem = ServletBuildItem.builder("metrics", SmallRyeMetricsServlet.class.getName())
                .addMapping(metrics.path + (metrics.path.endsWith("/") ? "*" : "/*"))
                .build();
        return servletBuildItem;
    }

    @BuildStep
    void beans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(MetricProducer.class,
                MetricNameFactory.class,
                MetricRegistries.class,
                MetricsInterceptor.class,
                MeteredInterceptor.class,
                CountedInterceptor.class,
                TimedInterceptor.class,
                MetricsRequestHandler.class,
                SmallRyeMetricsServlet.class));
    }

    @BuildStep
    void annotationTransformers(BuildProducer<AnnotationsTransformerBuildItem> transformers) {
        // attach @MetricsBinding to each class that contains any metric annotations
        transformers.produce(new AnnotationsTransformerBuildItem(ctx -> {
            if (ctx.isClass()) {
                // skip classes in package io.smallrye.metrics.interceptors
                if (ctx.getTarget().asClass().name().toString()
                        .startsWith(io.smallrye.metrics.interceptors.MetricsInterceptor.class.getPackage().getName())) {
                    return;
                }
                for (DotName annotationName : ctx.getTarget().asClass().annotations().keySet()) {
                    if (metricsAnnotations.contains(annotationName)) {
                        ctx.transform().add(AnnotationInstance.create(DotName.createSimple(MetricsBinding.class.getName()),
                                ctx.getTarget(), new AnnotationValue[0]))
                                .done();
                        return;
                    }
                }
            }
        }));
    }

    @BuildStep
    AutoInjectAnnotationBuildItem autoInjectMetric() {
        return new AutoInjectAnnotationBuildItem(DotName.createSimple(Metric.class.getName()));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(BeanContainerBuildItem beanContainerBuildItem,
            SmallRyeMetricsTemplate metrics,
            ShutdownContextBuildItem shutdown,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<FeatureBuildItem> feature) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_METRICS));

        for (DotName metricsAnnotation : metricsAnnotations) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, metricsAnnotation.toString()));
        }
        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, MetricsBinding.class.getName()));

        metrics.createRegistries(beanContainerBuildItem.getValue());
    }

    @BuildStep
    @Record(STATIC_INIT)
    void registerBaseAndVendorMetrics(SmallRyeMetricsTemplate metrics, ShutdownContextBuildItem shutdown) {
        metrics.registerBaseMetrics(shutdown);
        metrics.registerVendorMetrics(shutdown);
    }

    @BuildStep
    public void logCleanup(BuildProducer<LogCleanupFilterBuildItem> logCleanupFilter) {
        logCleanupFilter.produce(new LogCleanupFilterBuildItem("io.smallrye.metrics.MetricsRegistryImpl",
                "Register metric ["));
    }
}
