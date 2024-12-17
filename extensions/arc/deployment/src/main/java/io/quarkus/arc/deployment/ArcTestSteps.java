package io.quarkus.arc.deployment;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.runtime.ArcRecorder;
import io.quarkus.arc.runtime.test.ActivateSessionContextInterceptor;
import io.quarkus.arc.runtime.test.PreloadedTestApplicationClassPredicate;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;

@BuildSteps(onlyIf = IsTest.class)
public class ArcTestSteps {

    @BuildStep
    public void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        // We need to register the bean implementation for TestApplicationClassPredicate
        // TestApplicationClassPredicate is used programmatically in the ArC recorder when StartupEvent is fired
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(PreloadedTestApplicationClassPredicate.class));
        // In tests, register the ActivateSessionContextInterceptor and ActivateSessionContext interceptor binding
        additionalBeans.produce(new AdditionalBeanBuildItem(ActivateSessionContextInterceptor.class));
        additionalBeans.produce(new AdditionalBeanBuildItem("io.quarkus.test.ActivateSessionContext"));
    }

    @BuildStep
    AnnotationsTransformerBuildItem addInterceptorBinding() {
        return new AnnotationsTransformerBuildItem(
                AnnotationTransformation.forClasses().whenClass(ActivateSessionContextInterceptor.class).transform(tc -> tc.add(
                        AnnotationInstance.builder(DotName.createSimple("io.quarkus.test.ActivateSessionContext")).build())));
    }

    // For some reason the annotation literal generated for io.quarkus.test.ActivateSessionContext lives in app class loader.
    // This predicates ensures that the generated bean is considered an app class too.
    // As a consequence, the type and all methods of ActivateSessionContextInterceptor must be public.
    @BuildStep
    ApplicationClassPredicateBuildItem appClassPredicate() {
        return new ApplicationClassPredicateBuildItem(new Predicate<String>() {

            @Override
            public boolean test(String name) {
                return name.startsWith(ActivateSessionContextInterceptor.class.getName());
            }
        });
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void initTestApplicationClassPredicateBean(ArcRecorder recorder, BeanContainerBuildItem beanContainer,
            BeanDiscoveryFinishedBuildItem beanDiscoveryFinished,
            CompletedApplicationClassPredicateBuildItem predicate) {
        Set<String> applicationBeanClasses = new HashSet<>();
        for (BeanInfo bean : beanDiscoveryFinished.beanStream().classBeans()) {
            if (predicate.test(bean.getBeanClass())) {
                applicationBeanClasses.add(bean.getBeanClass().toString());
            }
        }
        recorder.initTestApplicationClassPredicate(applicationBeanClasses);
    }

}
