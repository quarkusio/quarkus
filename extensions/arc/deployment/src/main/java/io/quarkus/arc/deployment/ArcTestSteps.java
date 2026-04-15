package io.quarkus.arc.deployment;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
        // We sort the bean infos below in ad-hoc fashion to enforce stable bytecode generation.
        // There is an ongoing work (tracked in https://github.com/quarkusio/quarkus/pull/53359)
        // to add a reproducible build stream. Once that issue is resolved, we could revert to using
        // the original stream without sorting.
        List<BeanInfo> beanInfos = beanDiscoveryFinished.beanStream().classBeans()
                .stream()
                .sorted(Comparator.comparing(bi -> bi.getBeanClass().toString()))
                .toList();
        for (BeanInfo bean : beanInfos) {
            if (predicate.test(bean.getBeanClass())) {
                applicationBeanClasses.add(bean.getBeanClass().toString());
            }
        }
        recorder.initTestApplicationClassPredicate(applicationBeanClasses);
    }

}
