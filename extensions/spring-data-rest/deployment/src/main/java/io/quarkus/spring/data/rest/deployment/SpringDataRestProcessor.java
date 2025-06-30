package io.quarkus.spring.data.rest.deployment;

import static io.quarkus.deployment.Feature.SPRING_DATA_REST;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.Priorities;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.rest.data.panache.RestDataPanacheException;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.RestDataResourceBuildItem;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.rest.data.panache.deployment.properties.ResourcePropertiesBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;
import io.quarkus.spring.data.rest.runtime.RestDataPanacheExceptionMapper;
import io.quarkus.spring.data.rest.runtime.jta.TransactionalUpdateExecutor;

class SpringDataRestProcessor {

    private static final DotName CRUD_REPOSITORY_INTERFACE = DotName.createSimple(CrudRepository.class.getName());
    private static final DotName LIST_CRUD_REPOSITORY_INTERFACE = DotName.createSimple(ListCrudRepository.class.getName());

    private static final DotName PAGING_AND_SORTING_REPOSITORY_INTERFACE = DotName
            .createSimple(PagingAndSortingRepository.class.getName());

    private static final DotName LIST_PAGING_AND_SORTING_REPOSITORY_INTERFACE = DotName
            .createSimple(ListPagingAndSortingRepository.class.getName());

    private static final DotName JPA_REPOSITORY_INTERFACE = DotName.createSimple(JpaRepository.class.getName());

    private static final List<DotName> EXCLUDED_INTERFACES = Arrays.asList(
            CRUD_REPOSITORY_INTERFACE,
            LIST_CRUD_REPOSITORY_INTERFACE,
            PAGING_AND_SORTING_REPOSITORY_INTERFACE,
            LIST_PAGING_AND_SORTING_REPOSITORY_INTERFACE,
            JPA_REPOSITORY_INTERFACE);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(SPRING_DATA_REST);
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

    @BuildStep
    void registerRepositories(CombinedIndexBuildItem indexBuildItem, Capabilities capabilities,
            BuildProducer<GeneratedBeanBuildItem> implementationsProducer,
            BuildProducer<RestDataResourceBuildItem> restDataResourceProducer,
            BuildProducer<ResourcePropertiesBuildItem> resourcePropertiesProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer) {
        IndexView index = indexBuildItem.getIndex();
        EntityClassHelper entityClassHelper = new EntityClassHelper(index);
        List<ClassInfo> repositoriesToImplement = getRepositoriesToImplement(index, CRUD_REPOSITORY_INTERFACE,
                LIST_CRUD_REPOSITORY_INTERFACE,
                PAGING_AND_SORTING_REPOSITORY_INTERFACE, LIST_PAGING_AND_SORTING_REPOSITORY_INTERFACE,
                JPA_REPOSITORY_INTERFACE);

        implementResources(capabilities, implementationsProducer, restDataResourceProducer, resourcePropertiesProducer,
                unremovableBeansProducer, new RepositoryMethodsImplementor(index, entityClassHelper),
                index,
                repositoriesToImplement);
    }

    /**
     * Implement the {@link io.quarkus.rest.data.panache.RestDataResource} interface for each given Spring Data
     * repository and register its metadata and properties to be later picked up by the `rest-data-panache` extension.
     */
    private void implementResources(Capabilities capabilities,
            BuildProducer<GeneratedBeanBuildItem> implementationsProducer,
            BuildProducer<RestDataResourceBuildItem> restDataResourceProducer,
            BuildProducer<ResourcePropertiesBuildItem> resourcePropertiesProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer,
            ResourceMethodsImplementor methodsImplementor,
            IndexView index,
            List<ClassInfo> repositoriesToImplement) {
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(implementationsProducer);
        ResourceImplementor resourceImplementor = new ResourceImplementor(methodsImplementor);
        EntityClassHelper entityClassHelper = new EntityClassHelper(index);
        for (ClassInfo classInfo : repositoriesToImplement) {
            boolean paged = false;
            if (entityClassHelper.isPagingAndSortingRepository(classInfo.name().toString())) {
                paged = true;
            }
            ResourcePropertiesProvider propertiesProvider = new RepositoryPropertiesProvider(index, paged);
            List<Type> generics = getGenericTypes(classInfo);
            String repositoryName = classInfo.name().toString();
            String entityType = generics.get(0).name().toString();
            String idType = generics.get(1).name().toString();

            String resourceClass = resourceImplementor.implement(classOutput, repositoryName, entityType);

            // Register resource metadata so that a JAX-RS resource could be generated by the rest-data-panache
            restDataResourceProducer.produce(new RestDataResourceBuildItem(
                    new ResourceMetadata(resourceClass, repositoryName, null, entityType, idType, Collections.emptyMap())));
            // Spring Data repositories use different annotations for configuration and we translate them for
            // the rest-data-panache here.
            ResourceProperties resourceProperties = propertiesProvider.getResourceProperties(repositoryName);
            resourcePropertiesProducer.produce(new ResourcePropertiesBuildItem(resourceClass, resourceProperties));
            // Make sure that repository bean is not removed and will be injected to the generated resource
            unremovableBeansProducer.produce(new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanTypeExclusion(DotName.createSimple(repositoryName))));
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
