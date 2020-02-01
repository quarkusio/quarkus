package io.quarkus.spring.data.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.springframework.data.domain.Auditable;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.hibernate.orm.deployment.IgnorableNonIndexedClasses;
import io.quarkus.spring.data.deployment.generate.SpringDataRepositoryCreator;

public class SpringDataJPAProcessor {

    @BuildStep
    FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(FeatureBuildItem.SPRING_DATA_JPA);
    }

    @BuildStep
    IgnorableNonIndexedClasses ignorable() {
        Set<String> ignorable = new HashSet<>();
        ignorable.add(Auditable.class.getName());
        ignorable.add(Persistable.class.getName());
        return new IgnorableNonIndexedClasses(ignorable);
    }

    @BuildStep
    void build(CombinedIndexBuildItem index,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans, BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        IndexView indexIndex = index.getIndex();
        List<ClassInfo> interfacesExtendingCrudRepository = getAllInterfacesExtending(DotNames.SUPPORTED_REPOSITORIES,
                indexIndex);

        removeNoRepositoryBeanClasses(interfacesExtendingCrudRepository);
        implementCrudRepositories(generatedBeans, generatedClasses, additionalBeans, reflectiveClasses,
                interfacesExtendingCrudRepository, indexIndex);
    }

    private void removeNoRepositoryBeanClasses(List<ClassInfo> interfacesExtendingCrudRepository) {
        Iterator<ClassInfo> iterator = interfacesExtendingCrudRepository.iterator();
        while (iterator.hasNext()) {
            ClassInfo next = iterator.next();
            if (next.classAnnotation(DotNames.SPRING_DATA_NO_REPOSITORY_BEAN) != null) {
                iterator.remove();
            }
        }
    }

    // inefficient implementation, see: https://github.com/wildfly/jandex/issues/65
    private List<ClassInfo> getAllInterfacesExtending(Collection<DotName> targets, IndexView index) {
        List<ClassInfo> result = new ArrayList<>();
        Collection<ClassInfo> knownClasses = index.getKnownClasses();
        for (ClassInfo clazz : knownClasses) {
            if (!Modifier.isInterface(clazz.flags())) {
                continue;
            }
            List<DotName> interfaceNames = clazz.interfaceNames();
            boolean found = false;
            for (DotName interfaceName : interfaceNames) {
                if (targets.contains(interfaceName)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                result.add(clazz);
            }
        }
        return result;
    }

    // generate a concrete class that will be used by Arc to resolve injection points
    private void implementCrudRepositories(BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            List<ClassInfo> crudRepositoriesToImplement, IndexView index) {

        ClassOutput beansClassOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
        ClassOutput otherClassOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);

        // index the Spring Data repository interfaces that extend Repository because we need to pull the generic types from it
        Indexer indexer = new Indexer();
        Set<DotName> additionalIndex = new HashSet<>();
        indexRepositoryInterface(index, indexer, additionalIndex, Repository.class);
        indexRepositoryInterface(index, indexer, additionalIndex, CrudRepository.class);
        indexRepositoryInterface(index, indexer, additionalIndex, PagingAndSortingRepository.class);
        indexRepositoryInterface(index, indexer, additionalIndex, JpaRepository.class);
        indexRepositoryInterface(index, indexer, additionalIndex, QueryByExampleExecutor.class);
        CompositeIndex compositeIndex = CompositeIndex.create(index, indexer.complete());

        SpringDataRepositoryCreator repositoryCreator = new SpringDataRepositoryCreator(beansClassOutput, otherClassOutput,
                compositeIndex, (n) -> {
                    // the implementation of fragments don't need to be beans themselves
                    additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(n));
                },
                (className -> {
                    // the generated classes that implement interfaces for holding custom query results need
                    // to be registered for reflection here since this is the only point where the generated class is known
                    reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, className));
                }));

        for (ClassInfo crudRepositoryToImplement : crudRepositoriesToImplement) {
            repositoryCreator.implementCrudRepository(crudRepositoryToImplement);
        }
    }

    private void indexRepositoryInterface(IndexView index, Indexer indexer, Set<DotName> additionalIndex,
            Class<?> repoClass) {
        IndexingUtil.indexClass(repoClass.getName(), indexer, index, additionalIndex,
                SpringDataJPAProcessor.class.getClassLoader());
    }
}
