package io.quarkus.resteasy.reactive.links.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.resteasy.reactive.common.deployment.JaxRsResourceIndexBuildItem;
import io.quarkus.resteasy.reactive.links.runtime.GetterAccessorsContainer;
import io.quarkus.resteasy.reactive.links.runtime.GetterAccessorsContainerRecorder;
import io.quarkus.resteasy.reactive.links.runtime.LinkInfo;
import io.quarkus.resteasy.reactive.links.runtime.LinksContainer;
import io.quarkus.resteasy.reactive.links.runtime.LinksProviderRecorder;
import io.quarkus.resteasy.reactive.links.runtime.RestLinksProviderProducer;
import io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveDeploymentInfoBuildItem;
import io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveResourceMethodEntriesBuildItem;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;
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
            ResteasyReactiveDeploymentInfoBuildItem deploymentInfoBuildItem,
            ResteasyReactiveResourceMethodEntriesBuildItem resourceMethodEntriesBuildItem,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformersProducer,
            BuildProducer<GeneratedClassBuildItem> generatedClassesProducer,
            GetterAccessorsContainerRecorder getterAccessorsContainerRecorder,
            LinksProviderRecorder linksProviderRecorder) {
        IndexView index = indexBuildItem.getIndexView();
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClassesProducer, true);

        // Initialize links container
        LinksContainer linksContainer = getLinksContainer(deploymentInfoBuildItem, resourceMethodEntriesBuildItem);
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

    private LinksContainer getLinksContainer(ResteasyReactiveDeploymentInfoBuildItem deploymentInfoBuildItem,
            ResteasyReactiveResourceMethodEntriesBuildItem resourceMethodEntriesBuildItem) {
        LinksContainerFactory linksContainerFactory = new LinksContainerFactory();
        return linksContainerFactory.getLinksContainer(
                resourceMethodEntriesBuildItem.getEntries());
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
                for (String parameterName : linkInfo.getPathParameters()) {
                    // We implement a getter inside a class that has the required field.
                    // We later map that getter's accessor with a entity type.
                    // If a field is inside a parent class, the getter accessor will be mapped to each subclass which
                    // has REST links that need access to that field.
                    FieldInfo fieldInfo = getFieldInfo(index, DotName.createSimple(entityType), parameterName);
                    GetterMetadata getterMetadata = new GetterMetadata(fieldInfo);
                    if (!implementedGetters.contains(getterMetadata)) {
                        implementGetterWithAccessor(classOutput, bytecodeTransformersProducer, getterMetadata);
                        implementedGetters.add(getterMetadata);
                    }

                    getterAccessorsContainerRecorder.addAccessor(getterAccessorsContainer,
                            entityType, parameterName, getterMetadata.getGetterAccessorName());
                }
            }
        }

        return getterAccessorsContainer;
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

    /**
     * Find a field info by name inside a class.
     * This is a recursive method that looks through the class hierarchy until the field throws an error if it's not.
     */
    private FieldInfo getFieldInfo(IndexView index, DotName className, String fieldName) {
        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            throw new RuntimeException(String.format("Class '%s' was not found", className));
        }
        FieldInfo fieldInfo = classInfo.field(fieldName);
        if (fieldInfo != null) {
            return fieldInfo;
        }
        if (classInfo.superName() != null) {
            return getFieldInfo(index, classInfo.superName(), fieldName);
        }
        throw new RuntimeException(String.format("Class '%s' field '%s' was not found", className, fieldName));
    }
}
