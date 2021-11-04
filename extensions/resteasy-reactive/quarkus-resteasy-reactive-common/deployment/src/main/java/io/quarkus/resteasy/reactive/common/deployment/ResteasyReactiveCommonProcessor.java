package io.quarkus.resteasy.reactive.common.deployment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ws.rs.Priorities;
import javax.ws.rs.ext.RuntimeDelegate;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.resteasy.reactive.common.jaxrs.RuntimeDelegateImpl;
import org.jboss.resteasy.reactive.common.model.InterceptorContainer;
import org.jboss.resteasy.reactive.common.model.PreMatchInterceptorContainer;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptor;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveInterceptorScanner;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveScanner;
import org.jboss.resteasy.reactive.common.processor.scanning.SerializerScanningResult;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BuildTimeConditionBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveConfig;
import io.quarkus.resteasy.reactive.spi.AbstractInterceptorBuildItem;
import io.quarkus.resteasy.reactive.spi.AdditionalResourceClassBuildItem;
import io.quarkus.resteasy.reactive.spi.ContainerRequestFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.ContainerResponseFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.GeneratedJaxRsResourceBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderOverrideBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterOverrideBuildItem;
import io.quarkus.resteasy.reactive.spi.ReaderInterceptorBuildItem;
import io.quarkus.resteasy.reactive.spi.WriterInterceptorBuildItem;
import io.quarkus.security.spi.AdditionalSecuredClassesBuildItem;
import io.quarkus.security.spi.SecurityTransformerUtils;

public class ResteasyReactiveCommonProcessor {

    private static final int LEGACY_READER_PRIORITY = Priorities.USER * 2; // readers are compared by decreased priority
    private static final int LEGACY_WRITER_PRIORITY = Priorities.USER / 2; // writers are compared by increased priority

    @BuildStep
    void setUpDenyAllJaxRs(CombinedIndexBuildItem index,
            ResteasyReactiveConfig rrConfig,
            Optional<ResourceScanningResultBuildItem> resteasyDeployment,
            BuildProducer<AdditionalSecuredClassesBuildItem> additionalSecuredClasses) {

        Config config = ConfigProvider.getConfig();

        // we do this in order to avoid having 'io.quarkus.resteasy.reactive.common.runtime.JaxRsSecurityConfig' conflict with 'io.quarkus.resteasy.runtime.JaxRsSecurityConfig'
        Optional<Boolean> denyUnannotatedEndpointsConfig = config
                .getOptionalValue("quarkus.security.jaxrs.deny-unannotated-endpoints", Boolean.class);
        Optional<List<String>> defaultRolesAllowedConfig = config
                .getOptionalValues("quarkus.security.jaxrs.default-roles-allowed", String.class);

        if (rrConfig.denyJaxRs.orElse(denyUnannotatedEndpointsConfig.orElse(false)) && resteasyDeployment.isPresent()) {
            final List<ClassInfo> classes = new ArrayList<>();

            Set<DotName> resourceClasses = resteasyDeployment.get().getResult().getScannedResourcePaths().keySet();
            for (DotName className : resourceClasses) {
                ClassInfo classInfo = index.getIndex().getClassByName(className);
                if (!SecurityTransformerUtils.hasSecurityAnnotation(classInfo)) {
                    classes.add(classInfo);
                }
            }

            additionalSecuredClasses.produce(new AdditionalSecuredClassesBuildItem(classes));
        } else if (defaultRolesAllowedConfig.isPresent() && resteasyDeployment.isPresent()) {

            final List<ClassInfo> classes = new ArrayList<>();
            Set<DotName> resourceClasses = resteasyDeployment.get().getResult().getScannedResourcePaths().keySet();
            for (DotName className : resourceClasses) {
                ClassInfo classInfo = index.getIndex().getClassByName(className);
                if (!SecurityTransformerUtils.hasSecurityAnnotation(classInfo)) {
                    classes.add(classInfo);
                }
            }
            additionalSecuredClasses
                    .produce(new AdditionalSecuredClassesBuildItem(classes, defaultRolesAllowedConfig));
        }
    }

    @BuildStep
    ApplicationResultBuildItem handleApplication(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<BuildTimeConditionBuildItem> buildTimeConditions,
            ResteasyReactiveConfig config) {
        ApplicationScanningResult result = ResteasyReactiveScanner
                .scanForApplicationClass(combinedIndexBuildItem.getComputingIndex(),
                        config.buildTimeConditionAware ? getExcludedClasses(buildTimeConditions) : Collections.emptySet());
        if (result.getSelectedAppClass() != null) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, result.getSelectedAppClass().name().toString()));
        }
        return new ApplicationResultBuildItem(result);
    }

    @BuildStep
    public ResourceInterceptorsContributorBuildItem scanForIOInterceptors(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem) {
        return new ResourceInterceptorsContributorBuildItem(new Consumer<ResourceInterceptors>() {
            @Override
            public void accept(ResourceInterceptors interceptors) {
                ResteasyReactiveInterceptorScanner.scanForIOInterceptors(interceptors,
                        combinedIndexBuildItem.getComputingIndex(),
                        applicationResultBuildItem.getResult());
            }
        });
    }

    @BuildStep
    public ResourceInterceptorsBuildItem buildResourceInterceptors(List<ResourceInterceptorsContributorBuildItem> scanningTasks,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer,
            List<WriterInterceptorBuildItem> writerInterceptors,
            List<ReaderInterceptorBuildItem> readerInterceptors,
            List<ContainerRequestFilterBuildItem> requestFilters,
            List<ContainerResponseFilterBuildItem> responseFilters) {
        ResourceInterceptors resourceInterceptors = new ResourceInterceptors();
        for (ResourceInterceptorsContributorBuildItem i : scanningTasks) {
            i.getBuildTask().accept(resourceInterceptors);
        }
        AdditionalBeanBuildItem.Builder beanBuilder = AdditionalBeanBuildItem.builder();
        registerContainerBeans(beanBuilder, resourceInterceptors.getContainerResponseFilters());
        registerContainerBeans(beanBuilder, resourceInterceptors.getContainerRequestFilters());
        registerContainerBeans(beanBuilder, resourceInterceptors.getReaderInterceptors());
        registerContainerBeans(beanBuilder, resourceInterceptors.getWriterInterceptors());
        Set<String> globalNameBindings = applicationResultBuildItem.getResult().getGlobalNameBindings();
        for (WriterInterceptorBuildItem i : writerInterceptors) {
            registerInterceptors(globalNameBindings, resourceInterceptors.getWriterInterceptors(), i, beanBuilder);
        }
        for (ReaderInterceptorBuildItem i : readerInterceptors) {
            registerInterceptors(globalNameBindings, resourceInterceptors.getReaderInterceptors(), i, beanBuilder);
        }
        for (ContainerRequestFilterBuildItem i : requestFilters) {
            registerInterceptors(globalNameBindings, resourceInterceptors.getContainerRequestFilters(), i, beanBuilder);
        }
        for (ContainerResponseFilterBuildItem i : responseFilters) {
            registerInterceptors(globalNameBindings, resourceInterceptors.getContainerResponseFilters(), i, beanBuilder);
        }
        additionalBeanBuildItemBuildProducer.produce(beanBuilder.setUnremovable().build());
        return new ResourceInterceptorsBuildItem(resourceInterceptors);
    }

    protected <T, B extends AbstractInterceptorBuildItem> void registerInterceptors(Set<String> globalNameBindings,
            InterceptorContainer<T> interceptors, B filterItem, AdditionalBeanBuildItem.Builder beanBuilder) {
        if (filterItem.isRegisterAsBean()) {
            beanBuilder.addBeanClass(filterItem.getClassName());
        }
        ResourceInterceptor<T> interceptor = interceptors.create();
        interceptor.setClassName(filterItem.getClassName());
        Integer priority = filterItem.getPriority();
        if (priority != null) {
            interceptor.setPriority(priority);
        }
        if (filterItem instanceof ContainerRequestFilterBuildItem) {
            interceptor.setNonBlockingRequired(((ContainerRequestFilterBuildItem) filterItem).isNonBlockingRequired());
        }
        if (interceptors instanceof PreMatchInterceptorContainer
                && ((ContainerRequestFilterBuildItem) filterItem).isPreMatching()) {
            ((PreMatchInterceptorContainer<T>) interceptors).addPreMatchInterceptor(interceptor);

        } else {
            Set<String> nameBindingNames = filterItem.getNameBindingNames();
            if (nameBindingNames.isEmpty() || namePresent(nameBindingNames, globalNameBindings)) {
                interceptors.addGlobalRequestInterceptor(interceptor);
            } else {
                interceptor.setNameBindingNames(nameBindingNames);
                interceptors.addNameRequestInterceptor(interceptor);
            }
        }
    }

    private void registerContainerBeans(AdditionalBeanBuildItem.Builder additionalProviders,
            InterceptorContainer<?> container) {
        for (ResourceInterceptor<?> i : container.getGlobalResourceInterceptors()) {
            additionalProviders.addBeanClass(i.getClassName());
        }
        for (ResourceInterceptor<?> i : container.getNameResourceInterceptors()) {
            additionalProviders.addBeanClass(i.getClassName());
        }
        if (container instanceof PreMatchInterceptorContainer) {
            for (ResourceInterceptor<?> i : ((PreMatchInterceptorContainer<?>) container).getPreMatchInterceptors()) {
                additionalProviders.addBeanClass(i.getClassName());
            }
        }
    }

    private boolean namePresent(Set<String> nameBindingNames, Set<String> globalNameBindings) {
        for (String i : globalNameBindings) {
            if (nameBindingNames.contains(i)) {
                return true;
            }
        }
        return false;
    }

    @BuildStep
    JaxRsResourceIndexBuildItem resourceIndex(CombinedIndexBuildItem combinedIndex,
            List<GeneratedJaxRsResourceBuildItem> generatedJaxRsResources,
            BuildProducer<GeneratedBeanBuildItem> generatedBeansProducer) throws IOException {
        if (generatedJaxRsResources.isEmpty()) {
            return new JaxRsResourceIndexBuildItem(combinedIndex.getComputingIndex());
        }

        Indexer indexer = new Indexer();
        for (GeneratedJaxRsResourceBuildItem generatedJaxRsResource : generatedJaxRsResources) {
            indexer.index(new ByteArrayInputStream(generatedJaxRsResource.getData()));
            generatedBeansProducer
                    .produce(new GeneratedBeanBuildItem(generatedJaxRsResource.getName(), generatedJaxRsResource.getData()));
        }
        return new JaxRsResourceIndexBuildItem(CompositeIndex.create(combinedIndex.getComputingIndex(), indexer.complete()));
    }

    @BuildStep
    void scanResources(
            JaxRsResourceIndexBuildItem jaxRsResourceIndexBuildItem,
            List<AdditionalResourceClassBuildItem> additionalResourceClassBuildItems,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformerBuildItemBuildProducer,
            BuildProducer<ResourceScanningResultBuildItem> resourceScanningResultBuildItemBuildProducer) {

        Map<DotName, ClassInfo> additionalResources = new HashMap<>();
        Map<DotName, String> additionalResourcePaths = new HashMap<>();
        for (AdditionalResourceClassBuildItem bi : additionalResourceClassBuildItems) {
            additionalResources.put(bi.getClassInfo().name(), bi.getClassInfo());
            additionalResourcePaths.put(bi.getClassInfo().name(), bi.getPath());
        }
        ResourceScanningResult res = ResteasyReactiveScanner.scanResources(jaxRsResourceIndexBuildItem.getIndexView(),
                additionalResources, additionalResourcePaths);
        if (res == null) {
            return;
        }
        if (!res.getResourcesThatNeedCustomProducer().isEmpty()) {
            annotationsTransformerBuildItemBuildProducer
                    .produce(new AnnotationsTransformerBuildItem(
                            new VetoingAnnotationTransformer(res.getResourcesThatNeedCustomProducer().keySet())));
        }
        resourceScanningResultBuildItemBuildProducer.produce(new ResourceScanningResultBuildItem(res));
    }

    @BuildStep
    public void setupEndpoints(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BeanContainerBuildItem beanContainerBuildItem,
            Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<MessageBodyWriterBuildItem> messageBodyWriterBuildItemBuildProducer,
            BuildProducer<MessageBodyReaderBuildItem> messageBodyReaderBuildItemBuildProducer) throws NoSuchMethodException {

        if (!resourceScanningResultBuildItem.isPresent()) {
            // no detected @Path, bail out
            return;
        }

        IndexView index = beanArchiveIndexBuildItem.getIndex();
        SerializerScanningResult serializers = ResteasyReactiveScanner.scanForSerializers(index,
                applicationResultBuildItem.getResult());
        for (var i : serializers.getReaders()) {
            messageBodyReaderBuildItemBuildProducer.produce(new MessageBodyReaderBuildItem(i.getClassName(),
                    i.getHandledClassName(), i.getMediaTypeStrings(), i.getRuntimeType(), i.isBuiltin(), i.getPriority()));
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, i.getClassName()));
        }
        for (var i : serializers.getWriters()) {
            messageBodyWriterBuildItemBuildProducer.produce(new MessageBodyWriterBuildItem(i.getClassName(),
                    i.getHandledClassName(), i.getMediaTypeStrings(), i.getRuntimeType(), i.isBuiltin(), i.getPriority()));
        }
    }

    @BuildStep
    void registerRuntimeDelegateImpl(BuildProducer<ServiceProviderBuildItem> serviceProviders) {
        serviceProviders.produce(new ServiceProviderBuildItem(RuntimeDelegate.class.getName(),
                RuntimeDelegateImpl.class.getName()));
    }

    /*
     * There are some MessageBodyReaders and MessageBodyWriters that are brought in transitively
     * by the inclusion of the extension like the 'quarkus-keycloak-admin-client'.
     * We need to make sure that these providers are not selected over the ones that our Quarkus extensions provide.
     * To do that, we first need to make them built-in (as the spec mandates that non-build-in providers are choosen
     * over built-in ones) and then we also need to change their priority
     */
    @BuildStep
    void deprioritizeLegacyProviders(BuildProducer<MessageBodyReaderOverrideBuildItem> readers,
            BuildProducer<MessageBodyWriterOverrideBuildItem> writers) {
        readers.produce(new MessageBodyReaderOverrideBuildItem(
                "org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider", LEGACY_READER_PRIORITY, true));
        readers.produce(new MessageBodyReaderOverrideBuildItem("com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider",
                LEGACY_READER_PRIORITY, true));
        readers.produce(new MessageBodyReaderOverrideBuildItem("com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider",
                LEGACY_READER_PRIORITY, true));

        writers.produce(new MessageBodyWriterOverrideBuildItem(
                "org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider", LEGACY_WRITER_PRIORITY, true));
        writers.produce(new MessageBodyWriterOverrideBuildItem("com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider",
                LEGACY_WRITER_PRIORITY, true));
        writers.produce(new MessageBodyWriterOverrideBuildItem("com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider",
                LEGACY_WRITER_PRIORITY, true));
    }

    /**
     * @param buildTimeConditions the build time conditions from which the excluded classes are extracted.
     * @return the set of classes that have been annotated with unsuccessful build time conditions.
     */
    private static Set<String> getExcludedClasses(List<BuildTimeConditionBuildItem> buildTimeConditions) {
        return buildTimeConditions.stream()
                .filter(item -> !item.isEnabled())
                .map(BuildTimeConditionBuildItem::getTarget)
                .filter(target -> target.kind() == AnnotationTarget.Kind.CLASS)
                .map(target -> target.asClass().toString())
                .collect(Collectors.toSet());
    }
}
