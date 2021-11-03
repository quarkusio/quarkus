package io.quarkus.mongodb.rest.data.panache.deployment;

import static io.quarkus.deployment.Feature.MONGODB_REST_DATA_PANACHE;

import java.lang.reflect.Modifier;
import java.util.List;

import javax.ws.rs.Priorities;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.mongodb.rest.data.panache.PanacheMongoEntityResource;
import io.quarkus.mongodb.rest.data.panache.PanacheMongoRepositoryResource;
import io.quarkus.mongodb.rest.data.panache.runtime.NoopUpdateExecutor;
import io.quarkus.mongodb.rest.data.panache.runtime.RestDataPanacheExceptionMapper;
import io.quarkus.rest.data.panache.RestDataPanacheException;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.RestDataResourceBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;

class MongoPanacheRestProcessor {

    private static final DotName PANACHE_MONGO_ENTITY_RESOURCE_INTERFACE = DotName
            .createSimple(PanacheMongoEntityResource.class.getName());

    private static final DotName PANACHE_MONGO_REPOSITORY_RESOURCE_INTERFACE = DotName
            .createSimple(PanacheMongoRepositoryResource.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(MONGODB_REST_DATA_PANACHE);
    }

    @BuildStep
    void registerRestDataPanacheExceptionMapper(
            BuildProducer<ResteasyJaxrsProviderBuildItem> resteasyJaxrsProviderBuildItemBuildProducer,
            BuildProducer<ExceptionMapperBuildItem> exceptionMapperBuildItemBuildProducer) {
        resteasyJaxrsProviderBuildItemBuildProducer
                .produce(new ResteasyJaxrsProviderBuildItem(RestDataPanacheExceptionMapper.class.getName()));
        exceptionMapperBuildItemBuildProducer
                .produce(new ExceptionMapperBuildItem(RestDataPanacheExceptionMapper.class.getName(),
                        RestDataPanacheException.class.getName(), Priorities.USER + 100, false));
    }

    @BuildStep
    AdditionalBeanBuildItem registerTransactionalExecutor() {
        return AdditionalBeanBuildItem.unremovableOf(NoopUpdateExecutor.class);
    }

    @BuildStep
    void findEntityResources(CombinedIndexBuildItem index, Capabilities capabilities,
            BuildProducer<GeneratedBeanBuildItem> implementationsProducer,
            BuildProducer<RestDataResourceBuildItem> restDataResourceProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformersProducer) {
        EntityClassHelper entityClassHelper = new EntityClassHelper(index.getIndex());
        ResourceImplementor resourceImplementor = new ResourceImplementor(entityClassHelper);
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(implementationsProducer);

        for (ClassInfo classInfo : index.getIndex()
                .getKnownDirectImplementors(PANACHE_MONGO_ENTITY_RESOURCE_INTERFACE)) {
            validateResource(index.getIndex(), classInfo);

            List<Type> generics = getGenericTypes(classInfo);
            String resourceInterface = classInfo.name().toString();
            String entityType = generics.get(0).toString();
            String idType = generics.get(1).toString();

            DataAccessImplementor dataAccessImplementor = new EntityDataAccessImplementor(entityType);
            String resourceClass = resourceImplementor.implement(
                    classOutput, dataAccessImplementor, resourceInterface, entityType);

            restDataResourceProducer.produce(new RestDataResourceBuildItem(
                    new ResourceMetadata(resourceClass, resourceInterface, entityType, idType)));
            if (capabilities.isPresent(Capability.RESTEASY)) {
                bytecodeTransformersProducer.produce(
                        getEntityIdAnnotationTransformer(entityType, entityClassHelper.getIdField(entityType).name()));
            }
        }
    }

    @BuildStep
    void findRepositoryResources(CombinedIndexBuildItem index, Capabilities capabilities,
            BuildProducer<GeneratedBeanBuildItem> implementationsProducer,
            BuildProducer<RestDataResourceBuildItem> restDataResourceProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformersProducer) {
        EntityClassHelper entityClassHelper = new EntityClassHelper(index.getIndex());
        ResourceImplementor resourceImplementor = new ResourceImplementor(entityClassHelper);
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(implementationsProducer);

        for (ClassInfo classInfo : index.getIndex()
                .getKnownDirectImplementors(PANACHE_MONGO_REPOSITORY_RESOURCE_INTERFACE)) {
            validateResource(index.getIndex(), classInfo);

            List<Type> generics = getGenericTypes(classInfo);
            String resourceInterface = classInfo.name().toString();
            String repositoryClassName = generics.get(0).toString();
            String entityType = generics.get(1).toString();
            String idType = generics.get(2).toString();

            DataAccessImplementor dataAccessImplementor = new RepositoryDataAccessImplementor(repositoryClassName);
            String resourceClass = resourceImplementor.implement(
                    classOutput, dataAccessImplementor, resourceInterface, entityType);
            unremovableBeansProducer.produce(new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanClassNameExclusion(repositoryClassName)));

            restDataResourceProducer.produce(new RestDataResourceBuildItem(
                    new ResourceMetadata(resourceClass, resourceInterface, entityType, idType)));
            if (capabilities.isPresent(Capability.RESTEASY)) {
                bytecodeTransformersProducer.produce(
                        getEntityIdAnnotationTransformer(entityType, entityClassHelper.getIdField(entityType).name()));
            }
        }
    }

    private void validateResource(IndexView index, ClassInfo classInfo) {
        if (!Modifier.isInterface(classInfo.flags())) {
            throw new RuntimeException(classInfo.name() + " has to be an interface");
        }

        if (classInfo.interfaceNames().size() > 1) {
            throw new RuntimeException(classInfo.name() + " should only extend REST Data Panache interface");
        }

        if (!index.getKnownDirectImplementors(classInfo.name()).isEmpty()) {
            throw new RuntimeException(classInfo.name() + " should not be extended or implemented");
        }
    }

    private List<Type> getGenericTypes(ClassInfo classInfo) {
        return classInfo.interfaceTypes()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException(classInfo.toString() + " does not have generic types"))
                .asParameterizedType()
                .arguments();
    }

    /**
     * Annotate Mongo entity ID fields with a RESTEasy links annotation.
     * Otherwise RESTEasy will not be able to generate links that use ID as path parameter.
     */
    private BytecodeTransformerBuildItem getEntityIdAnnotationTransformer(String entityClassName, String idFieldName) {
        return new BytecodeTransformerBuildItem(entityClassName,
                (className, classVisitor) -> new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
                    @Override
                    public FieldVisitor visitField(int access, String name, String descriptor, String signature,
                            Object value) {
                        FieldVisitor fieldVisitor = super.visitField(access, name, descriptor, signature, value);
                        if (name.equals(idFieldName)) {
                            fieldVisitor.visitAnnotation("Lorg/jboss/resteasy/links/ResourceID;", true).visitEnd();
                        }
                        return fieldVisitor;
                    }
                });
    }
}
