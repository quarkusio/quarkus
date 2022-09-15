package io.quarkus.panache.common.deployment;

import static io.quarkus.panache.common.deployment.PanacheConstants.META_INF_PANACHE_ARCHIVE_MARKER;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.Transient;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo.DescriptorUtils;

public final class PanacheHibernateCommonResourceProcessor {

    private static final DotName DOTNAME_TRANSIENT = DotName.createSimple(Transient.class.getName());
    private static final DotName DOTNAME_KOTLIN_METADATA = DotName.createSimple("kotlin.Metadata");

    // This MUST be a separate step from replaceFieldAccess,
    // to avoid a cycle in build steps:
    //
    // HibernateEnhancersRegisteredBuildItem
    // needed for PanacheEntityClassesBuildItem
    // needed for InterceptedStaticMethodsTransformersRegisteredBuildItem
    // needed for HibernateEnhancersRegisteredBuildItem
    @BuildStep
    void findEntityClasses(CombinedIndexBuildItem index,
            Optional<HibernateModelClassCandidatesForFieldAccessBuildItem> candidatesForFieldAccess,
            BuildProducer<HibernateMetamodelForFieldAccessBuildItem> modelInfoBuildItem,
            BuildProducer<PanacheEntityClassesBuildItem> fieldAccessEnhancedEntityClasses) {
        if (candidatesForFieldAccess.isEmpty()) {
            // Hibernate ORM is disabled
            return;
        }

        MetamodelInfo modelInfo = new MetamodelInfo();

        // Technically we wouldn't need to process embeddables, but we don't have an easy way to exclude them.
        for (String entityClassName : candidatesForFieldAccess.get().getAllModelClassNames()) {
            ClassInfo entityClass = index.getIndex().getClassByName(DotName.createSimple(entityClassName));
            if (entityClass == null) {
                // Probably a synthetic entity, such as Envers' DefaultRevisionEntity.
                // We don't need to generate accessors for those.
                continue;
            }
            if (entityClass.annotationsMap().containsKey(DOTNAME_KOTLIN_METADATA)) {
                // This is a Kotlin class.
                // Historically we've never created accessors automatically for Kotlin,
                // since Kotlin language features (properties) can be used instead.
                continue;
            }
            modelInfo.addEntityModel(createEntityModel(entityClass));
        }

        // Share the metamodel for use in replaceFieldAccesses
        modelInfoBuildItem.produce(new HibernateMetamodelForFieldAccessBuildItem(modelInfo));

        Set<String> entitiesWithPublicFields = modelInfo.getEntitiesWithPublicFields();
        if (entitiesWithPublicFields.isEmpty()) {
            // There are no public fields to be accessed in the first place.
            return;
        }

        // Share with other extensions that we will generate accessors for some classes
        fieldAccessEnhancedEntityClasses
                .produce(new PanacheEntityClassesBuildItem(entitiesWithPublicFields));
    }

    @BuildStep
    @Consume(HibernateEnhancersRegisteredBuildItem.class)
    void replaceFieldAccesses(CombinedIndexBuildItem index,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            Optional<HibernateMetamodelForFieldAccessBuildItem> modelInfoBuildItem,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {
        if (modelInfoBuildItem.isEmpty()) {
            // Hibernate ORM is disabled
            return;
        }

        MetamodelInfo modelInfo = modelInfoBuildItem.get().getMetamodelInfo();
        Set<String> entitiesWithPublicFields = modelInfo.getEntitiesWithPublicFields();
        if (entitiesWithPublicFields.isEmpty()) {
            // There are no public fields to be accessed in the first place.
            return;
        }

        // Generate accessors for public fields in entities, mapped superclasses
        // (and embeddables, see where we build modelInfo above).
        PanacheJpaEntityAccessorsEnhancer entityAccessorsEnhancer = new PanacheJpaEntityAccessorsEnhancer(index.getIndex(),
                modelInfo);
        for (String entityClassName : entitiesWithPublicFields) {
            transformers.produce(new BytecodeTransformerBuildItem(true, entityClassName, entityAccessorsEnhancer));
        }

        // Replace field access in application code with calls to accessors
        Set<String> entityClassNamesInternal = new HashSet<>();
        for (String entityClassName : entitiesWithPublicFields) {
            entityClassNamesInternal.add(entityClassName.replace(".", "/"));
        }

        PanacheFieldAccessEnhancer panacheFieldAccessEnhancer = new PanacheFieldAccessEnhancer(modelInfo);
        QuarkusClassLoader tccl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
        List<ClassPathElement> archives = tccl.getElementsWithResource(META_INF_PANACHE_ARCHIVE_MARKER);
        Set<String> produced = new HashSet<>();
        //we always transform the root archive, even though it should be run with the annotation
        //processor on the CP it might not be if the user is using jpa-modelgen
        //this won't cover every situation, but we have documented this, and as the fields are now
        //made private the error should be very obvious
        //we only do this for hibernate, as it is more common to have an additional annotation processor
        for (ClassInfo i : applicationArchivesBuildItem.getRootArchive().getIndex().getKnownClasses()) {
            String cn = i.name().toString();
            produced.add(cn);
            transformers.produce(
                    new BytecodeTransformerBuildItem(cn, panacheFieldAccessEnhancer, entityClassNamesInternal));
        }

        for (ClassPathElement i : archives) {
            for (String res : i.getProvidedResources()) {
                if (res.endsWith(".class")) {
                    String cn = res.replace("/", ".").substring(0, res.length() - 6);
                    if (produced.contains(cn)) {
                        continue;
                    }
                    produced.add(cn);
                    transformers.produce(
                            new BytecodeTransformerBuildItem(cn, panacheFieldAccessEnhancer, entityClassNamesInternal));
                }
            }
        }
    }

    private EntityModel createEntityModel(ClassInfo classInfo) {
        EntityModel entityModel = new EntityModel(classInfo);
        for (FieldInfo fieldInfo : classInfo.fields()) {
            String name = fieldInfo.name();
            if (Modifier.isPublic(fieldInfo.flags())
                    && !Modifier.isStatic(fieldInfo.flags())
                    && !fieldInfo.hasAnnotation(DOTNAME_TRANSIENT)) {
                entityModel.addField(new EntityField(name, DescriptorUtils.typeToString(fieldInfo.type())));
            }
        }
        return entityModel;
    }

}
