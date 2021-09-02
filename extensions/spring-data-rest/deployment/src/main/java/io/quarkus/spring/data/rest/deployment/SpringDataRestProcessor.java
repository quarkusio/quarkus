package io.quarkus.spring.data.rest.deployment;

import static io.quarkus.deployment.Feature.SPRING_DATA_REST;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.Priorities;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.rest.data.panache.RestDataPanacheException;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.RestDataResourceBuildItem;
import io.quarkus.rest.data.panache.deployment.properties.ResourcePropertiesBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;
import io.quarkus.spring.data.rest.deployment.crud.CrudMethodsImplementor;
import io.quarkus.spring.data.rest.deployment.crud.CrudPropertiesProvider;
import io.quarkus.spring.data.rest.deployment.paging.PagingAndSortingMethodsImplementor;
import io.quarkus.spring.data.rest.deployment.paging.PagingAndSortingPropertiesProvider;
import io.quarkus.spring.data.rest.runtime.RestDataPanacheExceptionMapper;
import io.quarkus.spring.data.rest.runtime.jta.TransactionalUpdateExecutor;

class SpringDataRestProcessor {

    private static final DotName CRUD_REPOSITORY_INTERFACE = DotName.createSimple(CrudRepository.class.getName());

    private static final DotName PAGING_AND_SORTING_REPOSITORY_INTERFACE = DotName
            .createSimple(PagingAndSortingRepository.class.getName());

    private static final DotName JPA_REPOSITORY_INTERFACE = DotName.createSimple(JpaRepository.class.getName());

    private static final List<DotName> EXCLUDED_INTERFACES = Arrays.asList(
            CRUD_REPOSITORY_INTERFACE,
            PAGING_AND_SORTING_REPOSITORY_INTERFACE,
            JPA_REPOSITORY_INTERFACE);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(SPRING_DATA_REST);
    }

    @BuildStep
    ExceptionMapperBuildItem registerRestDataPanacheExceptionMapper() {
        return new ExceptionMapperBuildItem(RestDataPanacheExceptionMapper.class.getName(),
                RestDataPanacheException.class.getName(), Priorities.USER + 100, false);
    }

    @BuildStep
    AdditionalBeanBuildItem registerTransactionalExecutor() {
        return AdditionalBeanBuildItem.unremovableOf(TransactionalUpdateExecutor.class);
    }

    @BuildStep
    void registerCrudRepositories(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<GeneratedBeanBuildItem> implementationsProducer,
            BuildProducer<RestDataResourceBuildItem> restDataResourceProducer,
            BuildProducer<ResourcePropertiesBuildItem> resourcePropertiesProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer) {
        IndexView index = indexBuildItem.getIndex();

        implementResources(implementationsProducer, restDataResourceProducer, resourcePropertiesProducer,
                unremovableBeansProducer, new CrudMethodsImplementor(index), new CrudPropertiesProvider(index),
                getRepositoriesToImplement(index, CRUD_REPOSITORY_INTERFACE));
    }

    @BuildStep
    void registerPagingAndSortingRepositories(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<GeneratedBeanBuildItem> implementationsProducer,
            BuildProducer<RestDataResourceBuildItem> restDataResourceProducer,
            BuildProducer<ResourcePropertiesBuildItem> resourcePropertiesProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer) {
        IndexView index = indexBuildItem.getIndex();

        implementResources(implementationsProducer, restDataResourceProducer, resourcePropertiesProducer,
                unremovableBeansProducer, new PagingAndSortingMethodsImplementor(index),
                new PagingAndSortingPropertiesProvider(index),
                getRepositoriesToImplement(index, PAGING_AND_SORTING_REPOSITORY_INTERFACE, JPA_REPOSITORY_INTERFACE));
    }

    /**
     * Implement the {@link io.quarkus.rest.data.panache.RestDataResource} interface for each given Spring Data
     * repository and register its metadata and properties to be later picked up by the `rest-data-panache` extension.
     */
    private void implementResources(BuildProducer<GeneratedBeanBuildItem> implementationsProducer,
            BuildProducer<RestDataResourceBuildItem> restDataResourceProducer,
            BuildProducer<ResourcePropertiesBuildItem> resourcePropertiesProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer,
            ResourceMethodsImplementor methodsImplementor,
            ResourcePropertiesProvider propertiesProvider,
            List<ClassInfo> repositoriesToImplement) {
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(implementationsProducer);
        ResourceImplementor resourceImplementor = new ResourceImplementor(methodsImplementor);
        for (ClassInfo classInfo : repositoriesToImplement) {
            List<Type> generics = getGenericTypes(classInfo);
            String repositoryInterface = classInfo.name().toString();
            String entityType = generics.get(0).toString();
            String idType = generics.get(1).toString();

            String resourceClass = resourceImplementor.implement(classOutput, repositoryInterface, entityType);

            // Register resource metadata so that a JAX-RS resource could be generated by the rest-data-panache
            restDataResourceProducer.produce(new RestDataResourceBuildItem(
                    new ResourceMetadata(resourceClass, repositoryInterface, entityType, idType)));
            // Spring Data repositories use different annotations for configuration and we translate them for
            // the rest-data-panache here.
            resourcePropertiesProducer.produce(new ResourcePropertiesBuildItem(resourceClass,
                    propertiesProvider.getResourceProperties(repositoryInterface)));
            // Make sure that repository bean is not removed and will be injected to the generated resource
            unremovableBeansProducer.produce(new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanTypeExclusion(DotName.createSimple(repositoryInterface))));
        }
    }

    private List<ClassInfo> getRepositoriesToImplement(IndexView indexView, DotName... repositoryInterfaces) {
        List<ClassInfo> result = new LinkedList<>();
        for (DotName repositoryInterface : repositoryInterfaces) {
            for (ClassInfo classInfo : indexView.getKnownDirectImplementors(repositoryInterface)) {
                if (!hasImplementors(indexView, classInfo) && !EXCLUDED_INTERFACES.contains(classInfo.name())) {
                    validateResource(classInfo);
                    result.add(classInfo);
                }
            }
        }
        return result;
    }

    private boolean hasImplementors(IndexView index, ClassInfo classInfo) {
        return !index.getKnownDirectImplementors(classInfo.name()).isEmpty();
    }

    private void validateResource(ClassInfo classInfo) {
        if (!Modifier.isInterface(classInfo.flags())) {
            throw new RuntimeException(classInfo.name() + " has to be an interface");
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
