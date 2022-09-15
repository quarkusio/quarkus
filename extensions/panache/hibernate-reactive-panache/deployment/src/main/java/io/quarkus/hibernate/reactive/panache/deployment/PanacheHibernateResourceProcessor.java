package io.quarkus.hibernate.reactive.panache.deployment;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.deployment.HibernateEnhancersRegisteredBuildItem;
import io.quarkus.panache.common.deployment.PanacheJpaEntityOperationsEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerBuildItem;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public final class PanacheHibernateResourceProcessor {

    static final DotName DOTNAME_PANACHE_REPOSITORY_BASE = DotName.createSimple(PanacheRepositoryBase.class.getName());
    private static final DotName DOTNAME_PANACHE_REPOSITORY = DotName.createSimple(PanacheRepository.class.getName());
    static final DotName DOTNAME_PANACHE_ENTITY_BASE = DotName.createSimple(PanacheEntityBase.class.getName());
    private static final DotName DOTNAME_PANACHE_ENTITY = DotName.createSimple(PanacheEntity.class.getName());

    private static final DotName DOTNAME_REACTIVE_SESSION = DotName.createSimple(Mutiny.Session.class.getName());

    private static final DotName DOTNAME_ID = DotName.createSimple(Id.class.getName());
    protected static final String META_INF_PANACHE_ARCHIVE_MARKER = "META-INF/panache-archive.marker";

    private static final DotName DOTNAME_TRANSIENT = DotName.createSimple(Transient.class.getName());
    private static final DotName DOTNAME_UNI = DotName.createSimple(Uni.class.getName());
    private static final DotName DOTNAME_MULTI = DotName.createSimple(Multi.class.getName());

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.HIBERNATE_REACTIVE_PANACHE);
    }

    @BuildStep
    AdditionalJpaModelBuildItem produceModel() {
        // only useful for the index resolution: hibernate will register it to be transformed, but BuildMojo
        // only transforms classes from the application jar, so we do our own transforming
        return new AdditionalJpaModelBuildItem("io.quarkus.hibernate.reactive.panache.PanacheEntity");
    }

    @BuildStep
    UnremovableBeanBuildItem ensureBeanLookupAvailable() {
        return UnremovableBeanBuildItem.beanTypes(DOTNAME_REACTIVE_SESSION);
    }

    @BuildStep
    AdditionalApplicationArchiveMarkerBuildItem marker() {
        return new AdditionalApplicationArchiveMarkerBuildItem(META_INF_PANACHE_ARCHIVE_MARKER);
    }

    @BuildStep
    void collectEntityClasses(CombinedIndexBuildItem index, BuildProducer<PanacheEntityClassBuildItem> entityClasses) {
        // NOTE: we don't skip abstract/generic entities because they still need accessors
        for (ClassInfo panacheEntityBaseSubclass : index.getIndex().getAllKnownSubclasses(DOTNAME_PANACHE_ENTITY_BASE)) {
            // FIXME: should we really skip PanacheEntity or all MappedSuperClass?
            if (!panacheEntityBaseSubclass.name().equals(DOTNAME_PANACHE_ENTITY)) {
                entityClasses.produce(new PanacheEntityClassBuildItem(panacheEntityBaseSubclass));
            }
        }
    }

    @BuildStep
    @Consume(HibernateEnhancersRegisteredBuildItem.class)
    void build(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<PanacheEntityClassBuildItem> entityClasses,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems) throws Exception {

        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(bi -> bi.getMethodCustomizer()).collect(Collectors.toList());

        PanacheJpaRepositoryEnhancer daoEnhancer = new PanacheJpaRepositoryEnhancer(index.getIndex());
        Set<String> daoClasses = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_PANACHE_REPOSITORY_BASE)) {
            // Skip PanacheRepository
            if (classInfo.name().equals(DOTNAME_PANACHE_REPOSITORY))
                continue;
            if (daoEnhancer.skipRepository(classInfo))
                continue;
            daoClasses.add(classInfo.name().toString());
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_PANACHE_REPOSITORY)) {
            if (daoEnhancer.skipRepository(classInfo))
                continue;
            daoClasses.add(classInfo.name().toString());
        }
        for (String daoClass : daoClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(daoClass, daoEnhancer));
        }

        PanacheJpaEntityOperationsEnhancer entityOperationsEnhancer = new PanacheJpaEntityOperationsEnhancer(index.getIndex(),
                methodCustomizers,
                ReactiveJavaJpaTypeBundle.BUNDLE);
        for (PanacheEntityClassBuildItem entityClass : entityClasses) {
            String entityClassName = entityClass.get().name().toString();
            transformers.produce(new BytecodeTransformerBuildItem(true, entityClassName, entityOperationsEnhancer));
        }
    }

    @BuildStep
    ValidationPhaseBuildItem.ValidationErrorBuildItem validate(ValidationPhaseBuildItem validationPhase,
            CombinedIndexBuildItem index) throws BuildException {
        // we verify that no ID fields are defined (via @Id) when extending PanacheEntity
        for (AnnotationInstance annotationInstance : index.getIndex().getAnnotations(DOTNAME_ID)) {
            ClassInfo info = JandexUtil.getEnclosingClass(annotationInstance);
            if (JandexUtil.isSubclassOf(index.getIndex(), info, DOTNAME_PANACHE_ENTITY)) {
                BuildException be = new BuildException("You provide a JPA identifier via @Id inside '" + info.name() +
                        "' but one is already provided by PanacheEntity, " +
                        "your class should extend PanacheEntityBase instead, or use the id provided by PanacheEntity",
                        Collections.emptyList());
                return new ValidationPhaseBuildItem.ValidationErrorBuildItem(be);
            }
        }
        return null;
    }

    private static final String CHECK_RETURN_VALUE_BINARY_NAME = "io/smallrye/common/annotation/CheckReturnValue";
    private static final String CHECK_RETURN_VALUE_SIGNATURE = "L" + CHECK_RETURN_VALUE_BINARY_NAME + ";";

    @BuildStep
    PanacheMethodCustomizerBuildItem mutinyReturnTypes() {
        return new PanacheMethodCustomizerBuildItem(new PanacheMethodCustomizer() {
            @Override
            public void customize(Type entityClassSignature, MethodInfo method, MethodVisitor mv) {
                DotName returnType = method.returnType().name();
                if (returnType.equals(DOTNAME_UNI) || returnType.equals(DOTNAME_MULTI)) {
                    mv.visitAnnotation(CHECK_RETURN_VALUE_SIGNATURE, true);
                }
            }
        });
    }
}
