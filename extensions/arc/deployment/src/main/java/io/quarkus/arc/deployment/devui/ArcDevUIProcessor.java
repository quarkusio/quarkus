package io.quarkus.arc.deployment.devui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.ArcConfig;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.runtime.dev.EventsMonitor;
import io.quarkus.arc.runtime.dev.console.InvocationInterceptor;
import io.quarkus.arc.runtime.dev.console.InvocationTree;
import io.quarkus.arc.runtime.dev.console.InvocationsMonitor;
import io.quarkus.arc.runtime.dev.console.Monitored;
import io.quarkus.arc.runtime.dev.ui.ArcJsonRPCService;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

public class ArcDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    public CardPageBuildItem pages(ArcBeanInfoBuildItem arcBeanInfoBuildItem, ArcConfig config) {
        DevBeanInfos beanInfos = arcBeanInfoBuildItem.getBeanInfos();

        CardPageBuildItem pageBuildItem = new CardPageBuildItem();
        pageBuildItem.setLogo("cdi_logo.png", "cdi_logo.png");
        pageBuildItem.addLibraryVersion("jakarta.enterprise", "jakarta.enterprise.cdi-api", "Jakarta CDI",
                "https://jakarta.ee/specifications/cdi/");

        List<DevBeanInfo> beans = beanInfos.getBeans();
        if (!beans.isEmpty()) {
            pageBuildItem.addPage(Page.webComponentPageBuilder()
                    .icon("font-awesome-solid:egg")
                    .componentLink("qwc-arc-beans.js")
                    .staticLabel(String.valueOf(beans.size())));

            pageBuildItem.addBuildTimeData(BEANS, toDevBeanWithInterceptorInfo(beans, beanInfos));

            pageBuildItem.addBuildTimeData(BEAN_IDS_WITH_DEPENDENCY_GRAPHS, beanInfos.getDependencyGraphs().keySet());
            pageBuildItem.addBuildTimeData(DEPENDENCY_GRAPHS, beanInfos.getDependencyGraphs());
        }

        List<DevObserverInfo> observers = beanInfos.getObservers();
        if (!observers.isEmpty()) {
            pageBuildItem.addPage(Page.webComponentPageBuilder()
                    .icon("font-awesome-solid:eye")
                    .componentLink("qwc-arc-observers.js")
                    .staticLabel(String.valueOf(observers.size())));

            pageBuildItem.addBuildTimeData(OBSERVERS, observers);
        }

        List<DevInterceptorInfo> interceptors = beanInfos.getInterceptors();
        if (!interceptors.isEmpty()) {
            pageBuildItem.addPage(Page.webComponentPageBuilder()
                    .icon("font-awesome-solid:traffic-light")
                    .componentLink("qwc-arc-interceptors.js")
                    .staticLabel(String.valueOf(interceptors.size())));

            pageBuildItem.addBuildTimeData(INTERCEPTORS, interceptors);
        }

        List<DevDecoratorInfo> decorators = beanInfos.getDecorators();
        if (!decorators.isEmpty()) {
            pageBuildItem.addPage(Page.webComponentPageBuilder()
                    .icon("font-awesome-solid:traffic-light")
                    .componentLink("qwc-arc-decorators.js")
                    .staticLabel(String.valueOf(decorators.size())));

            pageBuildItem.addBuildTimeData(DECORATORS, decorators);
        }

        if (config.devMode().monitoringEnabled()) {
            pageBuildItem.addPage(Page.webComponentPageBuilder()
                    .icon("font-awesome-solid:fire")
                    .componentLink("qwc-arc-fired-events.js"));

            pageBuildItem.addPage(Page.webComponentPageBuilder()
                    .icon("font-awesome-solid:diagram-project")
                    .componentLink("qwc-arc-invocation-trees.js"));
        }

        int removedComponents = beanInfos.getRemovedComponents();
        if (removedComponents > 0) {
            pageBuildItem.addPage(Page.webComponentPageBuilder()
                    .icon("font-awesome-solid:trash-can")
                    .componentLink("qwc-arc-removed-components.js")
                    .staticLabel(String.valueOf(removedComponents)));

            pageBuildItem.addBuildTimeData(REMOVED_BEANS, beanInfos.getRemovedBeans());
            pageBuildItem.addBuildTimeData(REMOVED_DECORATORS, beanInfos.getRemovedDecorators());
            pageBuildItem.addBuildTimeData(REMOVED_INTERCEPTORS, beanInfos.getRemovedInterceptors());
        }

        return pageBuildItem;
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(ArcJsonRPCService.class);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void registerMonitoringComponents(ArcConfig config, BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<AnnotationsTransformerBuildItem> annotationTransformers,
            CustomScopeAnnotationsBuildItem customScopes, List<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        if (!config.devMode().monitoringEnabled()) {
            return;
        }
        if (!config.transformUnproxyableClasses()) {
            throw new IllegalStateException(
                    "Dev UI problem: monitoring of CDI business method invocations not possible\n\t- quarkus.arc.transform-unproxyable-classes was set to false and therefore it would not be possible to apply interceptors to unproxyable bean classes\n\t- please disable the monitoring feature via quarkus.arc.dev-mode.monitoring-enabled=false or enable unproxyable classes transformation");
        }
        // Register beans
        beans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(EventsMonitor.class, InvocationTree.class, InvocationsMonitor.class,
                        InvocationInterceptor.class,
                        Monitored.class)
                .build());

        // Add @Monitored to all beans
        Set<DotName> skipNames = Set.of(DotName.createSimple(InvocationTree.class),
                DotName.createSimple(InvocationsMonitor.class), DotName.createSimple(EventsMonitor.class));
        annotationTransformers.produce(new AnnotationsTransformerBuildItem(AnnotationsTransformer
                .appliedToClass()
                .whenClass(c -> (customScopes.isScopeDeclaredOn(c)
                        || isAdditionalBeanDefiningAnnotationOn(c, beanDefiningAnnotations))
                        && !skipClass(c, skipNames))
                .thenTransform(t -> t.add(Monitored.class))));
    }

    private boolean skipClass(ClassInfo beanClass, Set<DotName> skipNames) {
        if (skipNames.contains(beanClass.name())) {
            return true;
        }
        if (beanClass.name().packagePrefix().startsWith("io.quarkus.devui.runtime")) {
            // Skip monitoring for internal devui components
            return true;
        }
        return false;
    }

    private List<DevBeanWithInterceptorInfo> toDevBeanWithInterceptorInfo(List<DevBeanInfo> beans, DevBeanInfos devBeanInfos) {
        List<DevBeanWithInterceptorInfo> l = new ArrayList<>();
        for (DevBeanInfo dbi : beans) {
            l.add(new DevBeanWithInterceptorInfo(dbi, devBeanInfos));
        }
        return l;
    }

    private boolean isAdditionalBeanDefiningAnnotationOn(ClassInfo beanClass,
            List<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        for (BeanDefiningAnnotationBuildItem beanDefiningAnnotation : beanDefiningAnnotations) {
            if (beanClass.hasDeclaredAnnotation(beanDefiningAnnotation.getName())) {
                return true;
            }
        }
        return false;
    }

    private static final String BEAN_IDS_WITH_DEPENDENCY_GRAPHS = "beanIdsWithDependencyGraphs";
    private static final String DEPENDENCY_GRAPHS = "dependencyGraphs";
    private static final String BEANS = "beans";
    private static final String OBSERVERS = "observers";
    private static final String INTERCEPTORS = "interceptors";
    private static final String DECORATORS = "decorators";
    private static final String REMOVED_BEANS = "removedBeans";
    private static final String REMOVED_DECORATORS = "removedDecorators";
    private static final String REMOVED_INTERCEPTORS = "removedInterceptors";

}
