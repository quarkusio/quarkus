package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import static io.quarkus.deployment.Feature.HIBERNATE_ORM_REST_DATA_PANACHE;

import java.lang.reflect.Modifier;
import java.util.List;

import javax.ws.rs.Priorities;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.hibernate.orm.rest.data.panache.runtime.RestDataPanacheExceptionMapper;
import io.quarkus.hibernate.orm.rest.data.panache.runtime.jta.TransactionalUpdateExecutor;
import io.quarkus.rest.data.panache.RestDataPanacheException;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.RestDataResourceBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;

class HibernateOrmPanacheRestProcessor {

    private static final DotName PANACHE_ENTITY_RESOURCE_INTERFACE = DotName
            .createSimple(PanacheEntityResource.class.getName());

    private static final DotName PANACHE_REPOSITORY_RESOURCE_INTERFACE = DotName
            .createSimple(PanacheRepositoryResource.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(HIBERNATE_ORM_REST_DATA_PANACHE);
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
        return AdditionalBeanBuildItem.unremovableOf(TransactionalUpdateExecutor.class);
    }

    /**
     * Find Panache entity resources and generate their implementations.
     */
    @BuildStep
    void findEntityResources(CombinedIndexBuildItem index,
            BuildProducer<GeneratedBeanBuildItem> implementationsProducer,
            BuildProducer<RestDataResourceBuildItem> restDataResourceProducer) {
        ResourceImplementor resourceImplementor = new ResourceImplementor(new EntityClassHelper(index.getIndex()));
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(implementationsProducer);

        for (ClassInfo classInfo : index.getIndex().getKnownDirectImplementors(PANACHE_ENTITY_RESOURCE_INTERFACE)) {
            validateResource(index.getIndex(), classInfo);

            List<Type> generics = getGenericTypes(classInfo);
            String resourceInterface = classInfo.name().toString();
            String entityType = generics.get(0).name().toString();
            String idType = generics.get(1).name().toString();

            DataAccessImplementor dataAccessImplementor = new EntityDataAccessImplementor(entityType);
            String resourceClass = resourceImplementor.implement(
                    classOutput, dataAccessImplementor, resourceInterface, entityType);

            restDataResourceProducer.produce(new RestDataResourceBuildItem(
                    new ResourceMetadata(resourceClass, resourceInterface, entityType, idType)));
        }
    }

    /**
     * Find Panache repository resources and generate their implementations.
     */
    @BuildStep
    void findRepositoryResources(CombinedIndexBuildItem index,
            BuildProducer<GeneratedBeanBuildItem> implementationsProducer,
            BuildProducer<RestDataResourceBuildItem> restDataResourceProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer) {
        ResourceImplementor resourceImplementor = new ResourceImplementor(new EntityClassHelper(index.getIndex()));
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(implementationsProducer);

        for (ClassInfo classInfo : index.getIndex().getKnownDirectImplementors(PANACHE_REPOSITORY_RESOURCE_INTERFACE)) {
            validateResource(index.getIndex(), classInfo);

            List<Type> generics = getGenericTypes(classInfo);
            String resourceInterface = classInfo.name().toString();
            String repositoryClassName = generics.get(0).name().toString();
            String entityType = generics.get(1).name().toString();
            String idType = generics.get(2).name().toString();

            DataAccessImplementor dataAccessImplementor = new RepositoryDataAccessImplementor(repositoryClassName);
            String resourceClass = resourceImplementor.implement(
                    classOutput, dataAccessImplementor, resourceInterface, entityType);
            // Make sure that repository bean is not removed and will be injected to the generated resource
            unremovableBeansProducer.produce(new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanClassNameExclusion(repositoryClassName)));

            restDataResourceProducer.produce(new RestDataResourceBuildItem(
                    new ResourceMetadata(resourceClass, resourceInterface, entityType, idType)));
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
}
