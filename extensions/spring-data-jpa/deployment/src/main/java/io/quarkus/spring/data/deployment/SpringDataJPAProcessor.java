package io.quarkus.spring.data.deployment;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;
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

    private static final Logger LOGGER = Logger.getLogger(SpringDataJPAProcessor.class.getName());
    private static final Pattern pattern = Pattern.compile("spring\\.jpa\\..*");
    public static final String SPRING_JPA_SHOW_SQL = "spring.jpa.show-sql";
    public static final String SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT = "spring.jpa.properties.hibernate.dialect";
    public static final String SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT_STORAGE_ENGINE = "spring.jpa.properties.hibernate.dialect.storage_engine";
    public static final String SPRING_JPA_GENERATE_DDL = "spring.jpa.generate-ddl";
    public static final String SPRING_JPA_HIBERNATE_NAMING_PHYSICAL_STRATEGY = "spring.jpa.hibernate.naming.physical-strategy";
    public static final String SPRING_JPA_HIBERNATE_NAMING_IMPLICIT_STRATEGY = "spring.jpa.hibernate.naming.implicit-strategy";
    public static final String QUARKUS_HIBERNATE_ORM_DIALECT = "quarkus.hibernate-orm.dialect";
    public static final String QUARKUS_HIBERNATE_ORM_LOG_SQL = "quarkus.hibernate-orm.log.sql";
    public static final String QUARKUS_HIBERNATE_ORM_DIALECT_STORAGE_ENGINE = "quarkus.hibernate-orm.dialect.storage-engine";
    public static final String QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION = "quarkus.hibernate-orm.database.generation";
    public static final String QUARKUS_HIBERNATE_ORM_PHYSICAL_NAMING_STRATEGY = "quarkus.hibernate-orm.physical-naming-strategy";
    public static final String QUARKUS_HIBERNATE_ORM_IMPLICIT_NAMING_STRATEGY = "quarkus.hibernate-orm.implicit-naming-strategy";

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

        detectAndLogSpecificSpringPropertiesIfExist();

        IndexView indexIndex = index.getIndex();
        List<ClassInfo> interfacesExtendingCrudRepository = getAllInterfacesExtending(DotNames.SUPPORTED_REPOSITORIES,
                indexIndex);

        removeNoRepositoryBeanClasses(interfacesExtendingCrudRepository);
        implementCrudRepositories(generatedBeans, generatedClasses, additionalBeans, reflectiveClasses,
                interfacesExtendingCrudRepository, indexIndex);
    }

    private void detectAndLogSpecificSpringPropertiesIfExist() {
        Config config = ConfigProvider.getConfig();

        Iterable<String> iterablePropertyNames = config.getPropertyNames();
        List<String> propertyNames = new ArrayList<String>();
        iterablePropertyNames.forEach(propertyNames::add);
        List<String> springProperties = propertyNames.stream().filter(s -> pattern.matcher(s).matches()).collect(toList());
        String notSupportedProperties = "";

        if (!springProperties.isEmpty()) {
            for (String sp : springProperties) {
                switch (sp) {
                    case SPRING_JPA_SHOW_SQL:
                        notSupportedProperties = notSupportedProperties + "\t- " + SPRING_JPA_SHOW_SQL
                                + " should be replaced by " + QUARKUS_HIBERNATE_ORM_LOG_SQL + "\n";
                        break;
                    case SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT:
                        notSupportedProperties = notSupportedProperties + "\t- " + SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT
                                + " should be replaced by " + QUARKUS_HIBERNATE_ORM_DIALECT + "\n";
                        break;
                    case SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT_STORAGE_ENGINE:
                        notSupportedProperties = notSupportedProperties + "\t- "
                                + SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT_STORAGE_ENGINE + " should be replaced by "
                                + QUARKUS_HIBERNATE_ORM_DIALECT_STORAGE_ENGINE + "\n";
                        break;
                    case SPRING_JPA_GENERATE_DDL:
                        notSupportedProperties = notSupportedProperties + "\t- " + SPRING_JPA_GENERATE_DDL
                                + " should be replaced by " + QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION + "\n";
                        break;
                    case SPRING_JPA_HIBERNATE_NAMING_PHYSICAL_STRATEGY:
                        notSupportedProperties = notSupportedProperties + "\t- " + SPRING_JPA_HIBERNATE_NAMING_PHYSICAL_STRATEGY
                                + " should be replaced by " + QUARKUS_HIBERNATE_ORM_PHYSICAL_NAMING_STRATEGY + "\n";
                        break;
                    case SPRING_JPA_HIBERNATE_NAMING_IMPLICIT_STRATEGY:
                        notSupportedProperties = notSupportedProperties + "\t- " + SPRING_JPA_HIBERNATE_NAMING_IMPLICIT_STRATEGY
                                + " should be replaced by " + QUARKUS_HIBERNATE_ORM_IMPLICIT_NAMING_STRATEGY + "\n";
                        break;
                    default:
                        notSupportedProperties = notSupportedProperties + "\t- " + sp + "\n";
                        break;
                }
            }
            LOGGER.warnf(
                    "Quarkus does not support the following Spring Boot configuration properties: %n%s",
                    notSupportedProperties);
        }
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
