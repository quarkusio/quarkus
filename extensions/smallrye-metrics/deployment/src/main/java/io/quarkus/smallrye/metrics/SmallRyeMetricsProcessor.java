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

package io.quarkus.smallrye.metrics;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Collection;

import javax.interceptor.Interceptor;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.smallrye.metrics.runtime.SmallRyeMetricsServlet;
import io.quarkus.smallrye.metrics.runtime.SmallRyeMetricsTemplate;
import io.quarkus.undertow.ServletBuildItem;
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

    SmallRyeMetricsConfig metrics;

    @ConfigRoot(name = "smallrye-metrics")
    static final class SmallRyeMetricsConfig {

        /**
         * The path to the metrics Servlet.
         */
        @ConfigItem(defaultValue = "/metrics")
        String path;
    }

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
    @Record(STATIC_INIT)
    public void build(BeanContainerBuildItem beanContainerBuildItem,
            SmallRyeMetricsTemplate metrics,
            ShutdownContextBuildItem shutdown,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<FeatureBuildItem> feature) throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_METRICS));

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, Counted.class.getName()));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, MetricsBinding.class.getName()));

        metrics.createRegistries(beanContainerBuildItem.getValue());

        IndexView index = beanArchiveIndex.getIndex();
        Collection<AnnotationInstance> annos = index.getAnnotations(DotName.createSimple(Counted.class.getName()));

        for (AnnotationInstance anno : annos) {
            AnnotationTarget target = anno.target();

            // We need to exclude metrics interceptors
            if (Kind.CLASS.equals(target.kind())
                    && target.asClass().classAnnotations().stream()
                            .anyMatch(a -> a.name().equals(DotName.createSimple(Interceptor.class.getName())))) {
                continue;
            }

            MethodInfo methodInfo = target.asMethod();
            String name = methodInfo.name();
            if (anno.value("name") != null) {
                name = anno.value("name").asString();
            }
            ClassInfo classInfo = methodInfo.declaringClass();

            metrics.registerCounted(classInfo.name().toString(),
                    name, shutdown);
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void register(SmallRyeMetricsTemplate metrics, ShutdownContextBuildItem shutdown) {
        metrics.registerBaseMetrics(shutdown);
        metrics.registerVendorMetrics(shutdown);
    }

}
