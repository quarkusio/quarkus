package io.quarkus.hibernate.panache.deployment;

import static io.quarkus.hibernate.panache.deployment.EntityToPersistenceUnitUtil.determineEntityPersistenceUnits;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type.Kind;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.staticmethods.InterceptedStaticMethodsTransformersRegisteredBuildItem;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.hibernate.orm.deployment.JpaModelPersistenceUnitMappingBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.panache.PanacheEntityMarker;
import io.quarkus.hibernate.panache.PanacheRepositorySwitcher;
import io.quarkus.hibernate.panache.managed.blocking.PanacheManagedBlockingEntity;
import io.quarkus.hibernate.panache.managed.reactive.PanacheManagedReactiveEntity;
import io.quarkus.hibernate.panache.runtime.PanacheHibernateRecorder;
import io.quarkus.hibernate.panache.stateless.blocking.PanacheStatelessBlockingEntity;
import io.quarkus.hibernate.panache.stateless.reactive.PanacheStatelessReactiveEntity;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerBuildItem;
import io.quarkus.panache.hibernate.common.deployment.HibernateEnhancersRegisteredBuildItem;

public final class PanacheHibernateResourceProcessor {

    static final DotName DOTNAME_PANACHE_REPOSITORY_SWITCHER = DotName.createSimple(PanacheRepositorySwitcher.class.getName());

    static final DotName DOTNAME_PANACHE_MANAGED_BLOCKING_ENTITY = DotName
            .createSimple(PanacheManagedBlockingEntity.class.getName());
    static final DotName DOTNAME_PANACHE_MANAGED_REACTIVE_ENTITY = DotName
            .createSimple(PanacheManagedReactiveEntity.class.getName());
    static final DotName DOTNAME_PANACHE_STATELESS_BLOCKING_ENTITY = DotName
            .createSimple(PanacheStatelessBlockingEntity.class.getName());
    static final DotName DOTNAME_PANACHE_STATELESS_REACTIVE_ENTITY = DotName
            .createSimple(PanacheStatelessReactiveEntity.class.getName());
    static final DotName DOTNAME_PANACHE_ENTITY_MARKER = DotName.createSimple(PanacheEntityMarker.class.getName());

    private static final DotName DOTNAME_SESSION = DotName.createSimple(Session.class.getName());

    private static final DotName DOTNAME_ID = DotName.createSimple(Id.class.getName());

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.HIBERNATE_ORM_PANACHE); // FIXME
    }

    @BuildStep
    void produceModel(BuildProducer<AdditionalJpaModelBuildItem> models) {
        // only useful for the index resolution: hibernate will register it to be transformed, but BuildMojo
        // only transforms classes from the application jar, so we do our own transforming
        models.produce(
                new AdditionalJpaModelBuildItem(DOTNAME_PANACHE_MANAGED_BLOCKING_ENTITY.toString()));
        models.produce(
                new AdditionalJpaModelBuildItem(DOTNAME_PANACHE_MANAGED_REACTIVE_ENTITY.toString()));
        models.produce(
                new AdditionalJpaModelBuildItem(DOTNAME_PANACHE_STATELESS_BLOCKING_ENTITY.toString()));
        models.produce(
                new AdditionalJpaModelBuildItem(DOTNAME_PANACHE_STATELESS_REACTIVE_ENTITY.toString()));
    }

    @BuildStep
    UnremovableBeanBuildItem ensureBeanLookupAvailable() {
        // FIXME: look for mutiny sessions too?
        return new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanTypeExclusion(DOTNAME_SESSION));
    }

    @BuildStep
    void collectEntityClasses(CombinedIndexBuildItem index, BuildProducer<PanacheEntityClassBuildItem> entityClasses) {
        // NOTE: we don't skip abstract/generic entities because they still need accessors
        for (ClassInfo panacheEntityBaseSubclass : index.getIndex().getAllKnownImplementations(DOTNAME_PANACHE_ENTITY_MARKER)) {
            // FIXME: should we really skip PanacheEntity or all MappedSuperClass?
            if (!panacheEntityBaseSubclass.name().equals(DOTNAME_PANACHE_MANAGED_BLOCKING_ENTITY)
                    && !panacheEntityBaseSubclass.name().equals(DOTNAME_PANACHE_MANAGED_REACTIVE_ENTITY)
                    && !panacheEntityBaseSubclass.name().equals(DOTNAME_PANACHE_STATELESS_BLOCKING_ENTITY)
                    && !panacheEntityBaseSubclass.name().equals(DOTNAME_PANACHE_STATELESS_REACTIVE_ENTITY)) {
                entityClasses.produce(new PanacheEntityClassBuildItem(panacheEntityBaseSubclass));
            }
        }
    }

    @BuildStep
    @Consume(HibernateEnhancersRegisteredBuildItem.class)
    @Consume(InterceptedStaticMethodsTransformersRegisteredBuildItem.class)
    void build(
            CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<PanacheEntityClassBuildItem> entityClasses,
            Optional<JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems,
            BuildProducer<EntityToPersistenceUnitBuildItem> entityToPersistenceUnit) {

        Set<String> panacheEntities = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementations(DOTNAME_PANACHE_REPOSITORY_SWITCHER)) {
            // we don't want to add methods to abstract/generic entities/repositories: they get added to bottom types
            // which can't be either
            if (Modifier.isAbstract(classInfo.flags())
                    || !classInfo.typeParameters().isEmpty())
                continue;
            List<org.jboss.jandex.Type> typeParameters = JandexUtil
                    .resolveTypeParameters(classInfo.name(), DOTNAME_PANACHE_REPOSITORY_SWITCHER, index.getIndex());
            panacheEntities.add(typeParameters.get(0).name().toString());
        }

        Set<String> modelClasses = new HashSet<>();
        for (PanacheEntityClassBuildItem entityClass : entityClasses) {
            String entityClassName = entityClass.get().name().toString();
            modelClasses.add(entityClassName);
        }

        panacheEntities.addAll(modelClasses);

        determineEntityPersistenceUnits(jpaModelPersistenceUnitMapping, descriptors, panacheEntities, "Panache", (e, pu,
                reactivePU) -> entityToPersistenceUnit.produce(new EntityToPersistenceUnitBuildItem(e, pu, reactivePU)));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void recordEntityToPersistenceUnit(Optional<JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping,
            List<EntityToPersistenceUnitBuildItem> items,
            CombinedIndexBuildItem index,
            PanacheHibernateRecorder recorder,
            Capabilities capabilities) throws ClassNotFoundException {
        // PU
        // FIXME: for now, this ignores the reactive PUs, but reactive PUs are not supported yet by Panache Reactive
        Map<String, String> map = new HashMap<>();
        for (EntityToPersistenceUnitBuildItem item : items) {
            map.put(item.getEntityClass(), item.getPersistenceUnitName());
        }
        recorder.setEntityToPersistenceUnit(map,
                jpaModelPersistenceUnitMapping.map(JpaModelPersistenceUnitMappingBuildItem::isIncomplete)
                        // This happens if there is no persistence unit, in which case we definitely know this metadata is complete.
                        .orElse(false),
                capabilities.isPresent(Capability.HIBERNATE_REACTIVE));
        // Panache 2 repos
        Map<Class<?>, Class<?>> repositoryClassesToEntityClasses = new HashMap<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementations(DOTNAME_PANACHE_REPOSITORY_SWITCHER)) {
            // Only keep concrete classes
            if (classInfo.isInterface() || classInfo.isAbstract()) {
                continue;
            }
            List<org.jboss.jandex.Type> typeParameters = JandexUtil
                    .resolveTypeParameters(classInfo.name(), DOTNAME_PANACHE_REPOSITORY_SWITCHER, index.getIndex());
            if (typeParameters.get(0).kind() == Kind.CLASS) {
                String entityClassName = typeParameters.get(0).name().toString();
                Class<?> entityClass = Class.forName(entityClassName, false, Thread.currentThread().getContextClassLoader());
                Class<?> repositoryClass = Class.forName(classInfo.name().toString(), false,
                        Thread.currentThread().getContextClassLoader());
                repositoryClassesToEntityClasses.put(repositoryClass, entityClass);
            } else {
                throw new RuntimeException("Failed to find entity linked to repository: " + classInfo + ", it appears to be: "
                        + typeParameters.get(0) + " but we don't know what to do with it, it should be an entity type");
            }
        }
        recorder.setRepositoryClassesToEntityClasses(repositoryClassesToEntityClasses);
    }

    @BuildStep
    void makePanache2ReposUnremovable(
            CombinedIndexBuildItem index,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeanBuildItems) throws ClassNotFoundException {
        Set<DotName> unremovable = new HashSet<>();
        // Find Panache 2 repos, by listing every entity, to find query interfaces with no supertype
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(Entity.class)) {
            AnnotationTarget target = annotation.target();
            if (target.kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            ClassInfo entityClass = target.asClass();
            // find its member interfaces
            for (DotName memberClassName : entityClass.memberClasses()) {
                ClassInfo memberClass = index.getIndex().getClassByName(memberClassName);
                if (!memberClass.isInterface()) {
                    continue;
                }
                for (ClassInfo implementingBean : index.getIndex().getAllKnownImplementations(memberClassName)) {
                    unremovable.add(implementingBean.name());
                }
            }
        }
        // Now, some entities that do not explicitely add stateless/managed query repos will get some generated, so
        // either we figure out who they are, or we blanket add them all
        index.getIndex().getKnownClasses();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementations(DOTNAME_PANACHE_REPOSITORY_SWITCHER)) {
            // Only keep concrete classes
            if (classInfo.isInterface() || classInfo.isAbstract()) {
                continue;
            }
            unremovable.add(classInfo.name());
        }
        unremovableBeanBuildItems.produce(UnremovableBeanBuildItem.beanTypes(unremovable));
    }

    @BuildStep
    ValidationPhaseBuildItem.ValidationErrorBuildItem validate(ValidationPhaseBuildItem validationPhase,
            CombinedIndexBuildItem index) throws BuildException {
        // we verify that no ID fields are defined (via @Id) when extending PanacheEntity
        for (AnnotationInstance annotationInstance : index.getIndex().getAnnotations(DOTNAME_ID)) {
            ClassInfo info = JandexUtil.getEnclosingClass(annotationInstance);
            if (JandexUtil.isSubclassOf(index.getIndex(), info, DOTNAME_PANACHE_MANAGED_BLOCKING_ENTITY)
                    || JandexUtil.isSubclassOf(index.getIndex(), info, DOTNAME_PANACHE_MANAGED_REACTIVE_ENTITY)
                    || JandexUtil.isSubclassOf(index.getIndex(), info, DOTNAME_PANACHE_STATELESS_BLOCKING_ENTITY)
                    || JandexUtil.isSubclassOf(index.getIndex(), info, DOTNAME_PANACHE_STATELESS_REACTIVE_ENTITY)) {
                BuildException be = new BuildException("You provide a JPA identifier via @Id inside '" + info.name() +
                        "' but one is already provided by PanacheEntity, " +
                        "your class should extend PanacheEntityBase instead, or use the id provided by PanacheEntity",
                        Collections.emptyList());
                return new ValidationPhaseBuildItem.ValidationErrorBuildItem(be);
            }
        }
        return null;
    }
}
