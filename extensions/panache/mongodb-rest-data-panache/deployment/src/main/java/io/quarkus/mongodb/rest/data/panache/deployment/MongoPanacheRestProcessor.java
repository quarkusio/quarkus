package io.quarkus.mongodb.rest.data.panache.deployment;

import static io.quarkus.deployment.Feature.MONGODB_REST_DATA_PANACHE;

import java.lang.reflect.Modifier;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.mongodb.rest.data.panache.PanacheMongoEntityResource;
import io.quarkus.mongodb.rest.data.panache.PanacheMongoRepositoryResource;
import io.quarkus.rest.data.panache.deployment.DataAccessImplementor;
import io.quarkus.rest.data.panache.deployment.RestDataEntityInfo;
import io.quarkus.rest.data.panache.deployment.RestDataResourceBuildItem;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;

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
    void findEntityResources(CombinedIndexBuildItem index, BuildProducer<RestDataResourceBuildItem> resourcesProducer) {
        RestDataEntityInfoProvider entityInfoProvider = new RestDataEntityInfoProvider(index.getIndex());
        for (ClassInfo classInfo : index.getIndex().getKnownDirectImplementors(PANACHE_MONGO_ENTITY_RESOURCE_INTERFACE)) {
            validateResource(index.getIndex(), classInfo);
            List<Type> generics = getGenericTypes(classInfo);
            String entityClassName = generics.get(0).toString();
            String idClassName = generics.get(1).toString();
            RestDataEntityInfo entityInfo = entityInfoProvider.get(entityClassName, idClassName);
            DataAccessImplementor dataAccessImplementor = new EntityDataAccessImplementor(entityClassName);
            RestDataResourceInfo resourceInfo = new RestDataResourceInfo(classInfo.toString(), entityInfo,
                    dataAccessImplementor);
            resourcesProducer.produce(new RestDataResourceBuildItem(resourceInfo));
        }
    }

    @BuildStep
    void findRepositoryResources(CombinedIndexBuildItem index, BuildProducer<RestDataResourceBuildItem> resourcesProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformersProducer) {
        RestDataEntityInfoProvider entityInfoProvider = new RestDataEntityInfoProvider(index.getIndex());
        for (ClassInfo classInfo : index.getIndex().getKnownDirectImplementors(PANACHE_MONGO_REPOSITORY_RESOURCE_INTERFACE)) {
            validateResource(index.getIndex(), classInfo);
            List<Type> generics = getGenericTypes(classInfo);
            String repositoryClassName = generics.get(0).toString();
            String entityClassName = generics.get(1).toString();
            String idClassName = generics.get(2).toString();
            RestDataEntityInfo entityInfo = entityInfoProvider.get(entityClassName, idClassName);
            DataAccessImplementor dataAccessImplementor = new RepositoryDataAccessImplementor(repositoryClassName);
            RestDataResourceInfo resourceInfo = new RestDataResourceInfo(classInfo.toString(), entityInfo,
                    dataAccessImplementor);
            resourcesProducer.produce(new RestDataResourceBuildItem(resourceInfo));
            unremovableBeansProducer.produce(
                    new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNameExclusion(repositoryClassName)));
            bytecodeTransformersProducer
                    .produce(getEntityIdAnnotationTransformer(entityClassName, entityInfo.getIdField().name()));
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
                    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                        FieldVisitor fieldVisitor = super.visitField(access, name, descriptor, signature, value);
                        if (name.equals(idFieldName)) {
                            fieldVisitor.visitAnnotation("Lorg/jboss/resteasy/links/ResourceID;", true).visitEnd();
                        }
                        return fieldVisitor;
                    }
                });
    }
}
