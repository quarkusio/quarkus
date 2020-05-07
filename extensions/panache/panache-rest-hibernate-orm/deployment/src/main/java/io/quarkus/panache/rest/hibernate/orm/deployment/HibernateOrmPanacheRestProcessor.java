package io.quarkus.panache.rest.hibernate.orm.deployment;

import static io.quarkus.deployment.builditem.FeatureBuildItem.PANACHE_REST_HIBERNATE_ORM;

import java.lang.reflect.Modifier;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.panache.rest.common.deployment.PanacheCrudResourceBuildItem;
import io.quarkus.panache.rest.hibernate.orm.PanacheEntityCrudResource;
import io.quarkus.panache.rest.hibernate.orm.PanacheRepositoryCrudResource;

class HibernateOrmPanacheRestProcessor {

    private static final DotName PANACHE_ENTITY_CRUD_RESOURCE_INTERFACE = DotName
            .createSimple(PanacheEntityCrudResource.class.getName());

    private static final DotName PANACHE_REPOSITORY_CRUD_RESOURCE_INTERFACE = DotName
            .createSimple(PanacheRepositoryCrudResource.class.getName());

    private static final Logger LOGGER = Logger.getLogger(HibernateOrmPanacheRestProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(PANACHE_REST_HIBERNATE_ORM);
    }

    @BuildStep
    void findEntityCrudResources(CombinedIndexBuildItem index, BuildProducer<PanacheCrudResourceBuildItem> resourcesProducer) {
        for (ClassInfo classInfo : index.getIndex().getKnownDirectImplementors(PANACHE_ENTITY_CRUD_RESOURCE_INTERFACE)) {
            validateCrudResource(index.getIndex(), classInfo);
            List<Type> generics = getGenericTypes(classInfo);
            String entityClassName = generics.get(0).toString();
            String idClassName = generics.get(1).toString();
            PanacheCrudResourceBuildItem resource = new PanacheCrudResourceBuildItem(classInfo,
                    new EntityDataAccessImplementor(entityClassName), new HibernateOrmIdFieldPredicate(), idClassName,
                    entityClassName);
            resourcesProducer.produce(resource);
        }
    }

    @BuildStep
    void findRepositoryCrudResources(CombinedIndexBuildItem index,
            BuildProducer<PanacheCrudResourceBuildItem> resourcesProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer) {
        for (ClassInfo classInfo : index.getIndex().getKnownDirectImplementors(PANACHE_REPOSITORY_CRUD_RESOURCE_INTERFACE)) {
            validateCrudResource(index.getIndex(), classInfo);
            List<Type> generics = getGenericTypes(classInfo);
            String repositoryClassName = generics.get(0).toString();
            String entityClassName = generics.get(1).toString();
            String idClassName = generics.get(2).toString();
            PanacheCrudResourceBuildItem resource = new PanacheCrudResourceBuildItem(classInfo,
                    new RepositoryDataAccessImplementor(repositoryClassName), new HibernateOrmIdFieldPredicate(), idClassName,
                    entityClassName);
            resourcesProducer.produce(resource);
            unremovableBeansProducer.produce(
                    new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNameExclusion(repositoryClassName)));
        }
    }

    private void validateCrudResource(IndexView index, ClassInfo classInfo) {
        if (!Modifier.isInterface(classInfo.flags())) {
            throw new RuntimeException(classInfo.name() + " has to be an interface");
        }

        if (classInfo.interfaceNames().size() > 1) {
            throw new RuntimeException(classInfo.name() + " should only extend Panache REST CRUD interface");
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
