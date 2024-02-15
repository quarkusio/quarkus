package io.quarkus.resteasy.reactive.common.deployment;

import static org.jboss.resteasy.reactive.common.model.ResourceInterceptor.FILTER_SOURCE_METHOD_METADATA_KEY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.jaxrs.RuntimeDelegateImpl;
import org.jboss.resteasy.reactive.common.model.InterceptorContainer;
import org.jboss.resteasy.reactive.common.model.PreMatchInterceptorContainer;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptor;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveInterceptorScanner;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveParameterContainerScanner;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveScanner;
import org.jboss.resteasy.reactive.common.processor.scanning.SerializerScanningResult;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BuildTimeConditionBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.resteasy.reactive.common.runtime.JaxRsSecurityConfig;
import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveConfig;
import io.quarkus.resteasy.reactive.spi.AbstractInterceptorBuildItem;
import io.quarkus.resteasy.reactive.spi.AdditionalResourceClassBuildItem;
import io.quarkus.resteasy.reactive.spi.ContainerRequestFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.ContainerResponseFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.GeneratedJaxRsResourceBuildItem;
import io.quarkus.resteasy.reactive.spi.IgnoreStackMixingBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderOverrideBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterOverrideBuildItem;
import io.quarkus.resteasy.reactive.spi.ReaderInterceptorBuildItem;
import io.quarkus.resteasy.reactive.spi.WriterInterceptorBuildItem;
import io.quarkus.security.spi.DefaultSecurityCheckBuildItem;

public class ResteasyReactiveCommonProcessor {

    private static final Logger LOG = Logger.getLogger(ResteasyReactiveCommonProcessor.class);

    private static final int LEGACY_READER_PRIORITY = Priorities.USER * 2; // readers are compared by decreased priority
    private static final int LEGACY_WRITER_PRIORITY = Priorities.USER / 2; // writers are compared by increased priority

    private static final String PROVIDERS_SERVICE_FILE = "META-INF/services/" + Providers.class.getName();
    private static final Predicate<ResolvedDependency> IS_RESTEASY_CLASSIC_CLIENT_DEP = d -> d.getArtifactId()
            .contains("resteasy-client");
    private static final Predicate<ResolvedDependency> IS_RESTEASY_CLASSIC_CORE_DEP = d -> d.getArtifactId()
            .equals("resteasy-core");
    private static final Predicate<ResolvedDependency> IS_NOT_TEST_SCOPED = d -> !"test".equals(d.getScope());

    @Produce(ServiceStartBuildItem.class)
    @BuildStep
    void checkMixingStacks(Capabilities capabilities, CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<IgnoreStackMixingBuildItem> ignoreStackMixingItems) {
        if (!ignoreStackMixingItems.isEmpty()) {
            return;
        }
        List<ResolvedDependency> resteasyClassicDeps = curateOutcomeBuildItem.getApplicationModel().getDependencies().stream()
                .filter(d -> d.getGroupId().equals("org.jboss.resteasy")).collect(Collectors.toList());
        boolean hasResteasyCoreDep = resteasyClassicDeps.stream()
                .anyMatch(IS_NOT_TEST_SCOPED.and(IS_RESTEASY_CLASSIC_CORE_DEP));
        if (!hasResteasyCoreDep) {
            return;
        }
        boolean hasResteasyClassicClient = resteasyClassicDeps.stream()
                .anyMatch(IS_NOT_TEST_SCOPED.and(IS_RESTEASY_CLASSIC_CLIENT_DEP));
        if (!hasResteasyClassicClient) { // there is no bulletproof way of knowing whether a server specific dependency has been included, so we deduce it by the absence of client dependency
            throw new DeploymentException("Mixing RESTEasy Reactive and RESTEasy Classic server parts is not supported");
        }
        if (capabilities.isPresent(Capability.REST_CLIENT_REACTIVE)) {
            throw new DeploymentException(
                    "Mixing RESTEasy Reactive and RESTEasy Classic client parts is not supported");
        } else {
            LOG.warn(
                    "Mixing RESTEasy Reactive server and RESTEasy Classic client parts might lead to unexpected results. Consider using 'quarkus-rest-client-reactive' instead.");
        }
    }

    @BuildStep
    void searchForProviders(Capabilities capabilities,
            BuildProducer<AdditionalApplicationArchiveMarkerBuildItem> producer) {
        if (capabilities.isPresent(Capability.RESTEASY) || capabilities.isPresent(Capability.RESTEASY_CLIENT)
                || QuarkusClassLoader.isClassPresentAtRuntime(
                        "org.jboss.resteasy.plugins.providers.JaxrsServerFormUrlEncodedProvider")) { // RESTEasy Classic could be imported via non-Quarkus dependencies
            // in this weird case we don't want the providers to be registered automatically as this would lead to multiple bean definitions
            return;
        }
        // TODO: should we also be looking for the specific provider files?
        producer.produce(new AdditionalApplicationArchiveMarkerBuildItem(PROVIDERS_SERVICE_FILE));
    }

    @BuildStep
    void setUpDenyAllJaxRs(JaxRsSecurityConfig securityConfig,
            BuildProducer<DefaultSecurityCheckBuildItem> defaultSecurityCheckProducer) {
        if (securityConfig.denyJaxRs()) {
            defaultSecurityCheckProducer.produce(DefaultSecurityCheckBuildItem.denyAll());
        } else if (securityConfig.defaultRolesAllowed().isPresent()) {
            defaultSecurityCheckProducer
                    .produce(DefaultSecurityCheckBuildItem.rolesAllowed(securityConfig.defaultRolesAllowed().get()));
        }
    }

    @BuildStep
    ApplicationResultBuildItem handleApplication(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<BuildTimeConditionBuildItem> buildTimeConditions,
            ResteasyReactiveConfig config) {
        ApplicationScanningResult result = ResteasyReactiveScanner
                .scanForApplicationClass(combinedIndexBuildItem.getComputingIndex(),
                        config.buildTimeConditionAware() ? getExcludedClasses(buildTimeConditions) : Collections.emptySet());
        if (result.getSelectedAppClass() != null) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(result.getSelectedAppClass().name().toString())
                    .build());
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
            ContainerRequestFilterBuildItem crfbi = (ContainerRequestFilterBuildItem) filterItem;
            interceptor.setNonBlockingRequired(crfbi.isNonBlockingRequired());
            interceptor.setWithFormRead(crfbi.isWithFormRead());
            MethodInfo filterSourceMethod = crfbi.getFilterSourceMethod();
            if (filterSourceMethod != null) {
                interceptor.metadata = Map.of(FILTER_SOURCE_METHOD_METADATA_KEY, filterSourceMethod);
            }
        } else if (filterItem instanceof ContainerResponseFilterBuildItem) {
            MethodInfo filterSourceMethod = ((ContainerResponseFilterBuildItem) filterItem).getFilterSourceMethod();
            if (filterSourceMethod != null) {
                interceptor.metadata = Map.of(FILTER_SOURCE_METHOD_METADATA_KEY, filterSourceMethod);
            }
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
            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder(i.getClassName()).build());
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
     * To do that, we first need to make them built-in (as the spec mandates that non-build-in providers are chosen
     * over built-in ones) and then we also need to change their priority
     */
    @BuildStep
    void deprioritizeLegacyProviders(BuildProducer<MessageBodyReaderOverrideBuildItem> readers,
            BuildProducer<MessageBodyWriterOverrideBuildItem> writers) {
        readers.produce(new MessageBodyReaderOverrideBuildItem(
                "org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider", LEGACY_READER_PRIORITY, true));
        readers.produce(new MessageBodyReaderOverrideBuildItem("com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider",
                LEGACY_READER_PRIORITY, true));
        readers.produce(new MessageBodyReaderOverrideBuildItem("com.fasterxml.jackson.jakarta.rs.JacksonJaxbJsonProvider",
                LEGACY_READER_PRIORITY, true));
        readers.produce(new MessageBodyReaderOverrideBuildItem("org.keycloak.admin.client.JacksonProvider",
                LEGACY_READER_PRIORITY, true));

        writers.produce(new MessageBodyWriterOverrideBuildItem(
                "org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider", LEGACY_WRITER_PRIORITY, true));
        writers.produce(new MessageBodyWriterOverrideBuildItem("com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider",
                LEGACY_WRITER_PRIORITY, true));
        writers.produce(new MessageBodyWriterOverrideBuildItem("com.fasterxml.jackson.jakarta.rs.json.JacksonJaxbJsonProvider",
                LEGACY_WRITER_PRIORITY, true));
        writers.produce(new MessageBodyWriterOverrideBuildItem("org.keycloak.admin.client.JacksonProvider",
                LEGACY_WRITER_PRIORITY, true));
    }

    /**
     * @param buildTimeConditions the build time conditions from which the excluded classes are extracted.
     * @return the set of classes that have been annotated with unsuccessful build time conditions.
     */
    public static Set<String> getExcludedClasses(List<BuildTimeConditionBuildItem> buildTimeConditions) {
        return buildTimeConditions.stream()
                .filter(item -> !item.isEnabled())
                .map(BuildTimeConditionBuildItem::getTarget)
                .filter(target -> target.kind() == AnnotationTarget.Kind.CLASS)
                .map(target -> target.asClass().toString())
                .collect(Collectors.toSet());
    }

    @BuildStep
    public void scanForParameterContainers(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<ParameterContainersBuildItem> parameterContainersBuildItemBuildProducer) {
        IndexView index = combinedIndexBuildItem.getComputingIndex();
        Set<DotName> res = ResteasyReactiveParameterContainerScanner.scanParameterContainers(index,
                applicationResultBuildItem.getResult());
        parameterContainersBuildItemBuildProducer.produce(new ParameterContainersBuildItem(res));
    }
}
