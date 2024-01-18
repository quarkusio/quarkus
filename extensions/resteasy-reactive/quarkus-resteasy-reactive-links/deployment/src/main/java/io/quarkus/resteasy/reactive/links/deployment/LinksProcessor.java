package io.quarkus.resteasy.reactive.links.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OBJECT_NAME;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.common.util.RestMediaType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.resteasy.reactive.common.deployment.JaxRsResourceIndexBuildItem;
import io.quarkus.resteasy.reactive.links.runtime.GetterAccessorsContainer;
import io.quarkus.resteasy.reactive.links.runtime.GetterAccessorsContainerRecorder;
import io.quarkus.resteasy.reactive.links.runtime.LinkInfo;
import io.quarkus.resteasy.reactive.links.runtime.LinksContainer;
import io.quarkus.resteasy.reactive.links.runtime.LinksProviderRecorder;
import io.quarkus.resteasy.reactive.links.runtime.RestLinksProviderProducer;
import io.quarkus.resteasy.reactive.links.runtime.hal.HalServerResponseFilter;
import io.quarkus.resteasy.reactive.links.runtime.hal.ResteasyReactiveHalService;
import io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveResourceMethodEntriesBuildItem;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerResponseFilterBuildItem;
import io.quarkus.runtime.RuntimeValue;

final class LinksProcessor {

    private final GetterAccessorImplementor getterAccessorImplementor = new GetterAccessorImplementor();

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.RESTEASY_REACTIVE_LINKS));
    }

    @BuildStep
    MethodScannerBuildItem linksSupport() {
        return new MethodScannerBuildItem(new LinksMethodScanner());
    }

    @BuildStep
    @Record(STATIC_INIT)
    void initializeLinksProvider(JaxRsResourceIndexBuildItem indexBuildItem,
            ResteasyReactiveResourceMethodEntriesBuildItem resourceMethodEntriesBuildItem,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformersProducer,
            BuildProducer<GeneratedClassBuildItem> generatedClassesProducer,
            GetterAccessorsContainerRecorder getterAccessorsContainerRecorder,
            LinksProviderRecorder linksProviderRecorder) {
        IndexView index = indexBuildItem.getIndexView();
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClassesProducer, true);

        // Initialize links container
        LinksContainer linksContainer = getLinksContainer(resourceMethodEntriesBuildItem, index);
        // Implement getters to access link path parameter values
        RuntimeValue<GetterAccessorsContainer> getterAccessorsContainer = implementPathParameterValueGetters(
                index, classOutput, linksContainer, getterAccessorsContainerRecorder, bytecodeTransformersProducer);

        linksProviderRecorder.setGetterAccessorsContainer(getterAccessorsContainer);
        linksProviderRecorder.setLinksContainer(linksContainer);
    }

    @BuildStep
    AdditionalBeanBuildItem registerRestLinksProviderProducer() {
        return AdditionalBeanBuildItem.unremovableOf(RestLinksProviderProducer.class);
    }

    @BuildStep
    @Produce(ArtifactResultBuildItem.class)
    void validateJsonNeededForHal(Capabilities capabilities,
            ResteasyReactiveResourceMethodEntriesBuildItem resourceMethodEntriesBuildItem) {
        boolean isHalSupported = capabilities.isPresent(Capability.HAL);
        if (isHalSupported && isHalMediaTypeUsedInAnyResource(resourceMethodEntriesBuildItem.getEntries())) {

            if (!capabilities.isPresent(Capability.RESTEASY_REACTIVE_JSON_JSONB) && !capabilities.isPresent(
                    Capability.RESTEASY_REACTIVE_JSON_JACKSON)) {

                throw new IllegalStateException("Cannot generate HAL endpoints without "
                        + "either 'quarkus-resteasy-reactive-jsonb' or 'quarkus-resteasy-reactive-jackson'");
            }
        }
    }

    @BuildStep
    void addHalSupport(Capabilities capabilities,
            BuildProducer<CustomContainerResponseFilterBuildItem> customResponseFilters,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        boolean isHalSupported = capabilities.isPresent(Capability.HAL);
        if (isHalSupported) {
            customResponseFilters.produce(
                    new CustomContainerResponseFilterBuildItem(HalServerResponseFilter.class.getName()));

            additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(ResteasyReactiveHalService.class));
        }
    }

    private boolean isHalMediaTypeUsedInAnyResource(List<ResteasyReactiveResourceMethodEntriesBuildItem.Entry> entries) {
        for (ResteasyReactiveResourceMethodEntriesBuildItem.Entry entry : entries) {
            for (String mediaType : entry.getResourceMethod().getProduces()) {
                if (RestMediaType.APPLICATION_HAL_JSON.equals(mediaType)) {
                    return true;
                }
            }
        }

        return false;
    }

    private LinksContainer getLinksContainer(ResteasyReactiveResourceMethodEntriesBuildItem resourceMethodEntriesBuildItem,
            IndexView index) {
        LinksContainerFactory linksContainerFactory = new LinksContainerFactory();
        return linksContainerFactory.getLinksContainer(resourceMethodEntriesBuildItem.getEntries(), index);
    }

    /**
     * For each path parameter implement a getter method in a class that holds its value.
     * Then implement a getter accessor class that knows how to access that getter method to avoid using reflection later.
     */
    private RuntimeValue<GetterAccessorsContainer> implementPathParameterValueGetters(IndexView index,
            ClassOutput classOutput, LinksContainer linksContainer,
            GetterAccessorsContainerRecorder getterAccessorsContainerRecorder,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformersProducer) {
        RuntimeValue<GetterAccessorsContainer> getterAccessorsContainer = getterAccessorsContainerRecorder.newContainer();
        Set<GetterMetadata> implementedGetters = new HashSet<>();

        for (List<LinkInfo> linkInfos : linksContainer.getLinksMap().values()) {
            for (LinkInfo linkInfo : linkInfos) {
                String entityType = linkInfo.getEntityType();
                DotName className = DotName.createSimple(entityType);

                validateClassHasFieldId(index, entityType);

                for (String parameterName : linkInfo.getPathParameters()) {
                    FieldInfoSupplier byParamName = new FieldInfoSupplier(c -> c.field(parameterName), className, index);

                    // We implement a getter inside a class that has the required field.
                    // We later map that getter's accessor with an entity type.
                    // If a field is inside a parent class, the getter accessor will be mapped to each subclass which
                    // has REST links that need access to that field.
                    FieldInfo fieldInfo = byParamName.get();
                    if ((fieldInfo == null) && parameterName.equals("id")) {
                        // this is a special case where we want to go through the fields of the class
                        // and see if any is annotated with any sort of @Id annotation
                        // N.B. as this module does not depend on any other module that could supply this @Id annotation
                        // (like Panache), we need this general lookup
                        FieldInfoSupplier byIdAnnotation = new FieldInfoSupplier(
                                c -> {
                                    for (FieldInfo field : c.fields()) {
                                        List<AnnotationInstance> annotationInstances = field.annotations();
                                        for (AnnotationInstance annotationInstance : annotationInstances) {
                                            if (annotationInstance.name().toString().endsWith("persistence.Id")) {
                                                return field;
                                            }
                                        }
                                    }
                                    return null;
                                },
                                className,
                                index);
                        fieldInfo = byIdAnnotation.get();
                    }

                    if (fieldInfo != null) {
                        GetterMetadata getterMetadata = new GetterMetadata(fieldInfo);
                        if (!implementedGetters.contains(getterMetadata)) {
                            implementGetterWithAccessor(classOutput, bytecodeTransformersProducer, getterMetadata);
                            implementedGetters.add(getterMetadata);
                        }

                        getterAccessorsContainerRecorder.addAccessor(getterAccessorsContainer,
                                entityType, getterMetadata.getFieldName(), getterMetadata.getGetterAccessorName());
                    }
                }
            }
        }

        return getterAccessorsContainer;
    }

    /**
     * Validates if the given classname contains a field `id` or annotated with `@Id`
     *
     * @throws IllegalStateException if the classname does not contain any sort of field identifier
     */
    private void validateClassHasFieldId(IndexView index, String entityType) {
        // create a new independent class name that we can override
        DotName className = DotName.createSimple(entityType);
        ClassInfo classInfo = index.getClassByName(className);

        if (classInfo == null) {
            throw new RuntimeException(String.format("Class '%s' was not found", classInfo));
        }
        validateRec(index, entityType, classInfo);
    }

    /**
     * Validates if the given classname contains a field `id` or annotated with `@Id`
     *
     * @throws IllegalStateException if the classname does not contain any sort of field identifier
     */
    private void validateRec(IndexView index, String entityType, ClassInfo classInfo) {
        List<FieldInfo> fieldsNamedId = classInfo.fields().stream()
                .filter(f -> f.name().equals("id"))
                .toList();

        List<AnnotationInstance> fieldsAnnotatedWithId = classInfo.fields().stream()
                .flatMap(f -> f.annotations().stream())
                .filter(a -> a.name().toString().endsWith("persistence.Id"))
                .toList();

        // Id field found, break the loop
        if (!fieldsNamedId.isEmpty() || !fieldsAnnotatedWithId.isEmpty())
            return;

        // Id field not found and hope is gone
        DotName superClassName = classInfo.superName();
        if (superClassName == null) {
            throw new IllegalStateException("Cannot generate web links for the class " + entityType +
                    " because is either missing an `id` field or a field with an `@Id` annotation");
        }

        // Id field not found but there's still hope
        classInfo = index.getClassByName(superClassName);
        if (classInfo == null) {
            throw new RuntimeException(String.format("Class '%s' was not found", classInfo));
        }
        validateRec(index, entityType, classInfo);
    }

    /**
     * Implement a field getter inside a class and create an accessor class which knows how to access it.
     */
    private void implementGetterWithAccessor(ClassOutput classOutput,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformersProducer,
            GetterMetadata getterMetadata) {
        bytecodeTransformersProducer.produce(new BytecodeTransformerBuildItem(
                getterMetadata.getEntityType(), GetterImplementor.getVisitorFunction(getterMetadata)));
        getterAccessorImplementor.implement(classOutput, getterMetadata);
    }

    private static class FieldInfoSupplier implements Supplier<FieldInfo> {

        private final Function<ClassInfo, FieldInfo> strategy;
        private final DotName className;
        private final IndexView index;

        public FieldInfoSupplier(Function<ClassInfo, FieldInfo> strategy, DotName className, IndexView index) {
            this.strategy = strategy;
            this.className = className;
            this.index = index;
        }

        @Override
        public FieldInfo get() {
            return findFieldRecursively(className);
        }

        private FieldInfo findFieldRecursively(DotName className) {
            ClassInfo classInfo = index.getClassByName(className);
            if (classInfo == null) {
                throw new RuntimeException(String.format("Class '%s' was not found", className));
            }
            FieldInfo result = strategy.apply(classInfo);
            if (result != null) {
                return result;
            }
            if (classInfo.superName() != null && !classInfo.superName().equals(OBJECT_NAME)) {
                return findFieldRecursively(classInfo.superName());
            }
            return null;
        }
    }

}
