package org.jboss.shamrock.metrics;

import java.util.Collection;

import javax.inject.Inject;
import javax.interceptor.Interceptor;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.BeanArchiveIndex;
import org.jboss.shamrock.deployment.BeanDeployment;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.ShamrockConfig;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.metrics.runtime.MetricsDeploymentTemplate;
import org.jboss.shamrock.metrics.runtime.MetricsServlet;
import org.jboss.shamrock.undertow.ServletData;
import org.jboss.shamrock.undertow.ServletDeployment;

import io.smallrye.metrics.MetricProducer;
import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.MetricsRequestHandler;
import io.smallrye.metrics.interceptors.CountedInterceptor;
import io.smallrye.metrics.interceptors.MeteredInterceptor;
import io.smallrye.metrics.interceptors.MetricNameFactory;
import io.smallrye.metrics.interceptors.MetricsBinding;
import io.smallrye.metrics.interceptors.MetricsInterceptor;
import io.smallrye.metrics.interceptors.TimedInterceptor;

public class MetricsProcessor implements ResourceProcessor {


    @Inject
    private BeanDeployment beanDeployment;

    @Inject
    private ShamrockConfig config;

    @Inject
    private ServletDeployment servletDeployment;

    @Inject
    private BeanArchiveIndex beanArchiveIndex;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        ServletData servletData = new ServletData("metrics", MetricsServlet.class.getName());
        servletData.getMapings().add(config.getConfig("metrics.path", "/metrics"));
        servletDeployment.addServlet(servletData);

        beanDeployment.addAdditionalBean(MetricProducer.class,
                MetricNameFactory.class,
                MetricRegistries.class);

        beanDeployment.addAdditionalBean(MetricsInterceptor.class,
                MeteredInterceptor.class,
                CountedInterceptor.class,
                TimedInterceptor.class);

        beanDeployment.addAdditionalBean(MetricsRequestHandler.class, MetricsServlet.class);

        processorContext.addReflectiveClass(false, false, Counted.class.getName(), MetricsBinding.class.getName());


        try (BytecodeRecorder recorder = processorContext.addStaticInitTask(RuntimePriority.WELD_DEPLOYMENT + 30)) {
            MetricsDeploymentTemplate metrics = recorder.getRecordingProxy(MetricsDeploymentTemplate.class);

            metrics.createRegistries(null);

            IndexView index = beanArchiveIndex.getIndex();
            Collection<AnnotationInstance> annos = index.getAnnotations(DotName.createSimple(Counted.class.getName()));

            for (AnnotationInstance anno : annos) {
                AnnotationTarget target = anno.target();

                // We need to exclude metrics interceptors
                if (Kind.CLASS.equals(target.kind())
                        && target.asClass().classAnnotations().stream().anyMatch(a -> a.name().equals(DotName.createSimple(Interceptor.class.getName())))) {
                    continue;
                }

                MethodInfo methodInfo = target.asMethod();
                String name = methodInfo.name();
                if(anno.value("name") != null) {
                    name = anno.value("name").asString();
                }
                ClassInfo classInfo = methodInfo.declaringClass();

                metrics.registerCounted(classInfo.name().toString(),
                        name);
            }

        }
        try (BytecodeRecorder recorder = processorContext.addDeploymentTask(RuntimePriority.WELD_DEPLOYMENT + 30)) {
            MetricsDeploymentTemplate metrics = recorder.getRecordingProxy(MetricsDeploymentTemplate.class);
            metrics.registerBaseMetrics();
            metrics.registerVendorMetrics();
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
