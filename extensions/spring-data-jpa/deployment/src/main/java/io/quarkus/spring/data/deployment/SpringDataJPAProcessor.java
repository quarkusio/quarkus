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
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
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
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.hibernate.orm.deployment.IgnorableNonIndexedClasses;
import io.quarkus.hibernate.orm.panache.deployment.JavaJpaTypeBundle;
import io.quarkus.spring.data.deployment.generate.SpringDataRepositoryCreator;

public class SpringDataJPAProcessor {

    private static final Logger LOGGER = Logger.getLogger(SpringDataJPAProcessor.class.getName());
    private static final Pattern pattern = Pattern.compile("spring\\..*");
    public static final String SPRING_JPA_SHOW_SQL = "spring.jpa.show-sql";
    public static final String SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT = "spring.jpa.properties.hibernate.dialect";
    public static final String SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT_STORAGE_ENGINE = "spring.jpa.properties.hibernate.dialect.storage_engine";
    public static final String SPRING_JPA_GENERATE_DDL = "spring.jpa.generate-ddl";
    public static final String SPRING_JPA_HIBERNATE_NAMING_PHYSICAL_STRATEGY = "spring.jpa.hibernate.naming.physical-strategy";
    public static final String SPRING_JPA_HIBERNATE_NAMING_IMPLICIT_STRATEGY = "spring.jpa.hibernate.naming.implicit-strategy";
    private static final String SPRING_DATASOURCE_DATA = "spring.datasource.data";
    public static final String QUARKUS_HIBERNATE_ORM_DIALECT = "quarkus.hibernate-orm.dialect";
    public static final String QUARKUS_HIBERNATE_ORM_LOG_SQL = "quarkus.hibernate-orm.log.sql";
    public static final String QUARKUS_HIBERNATE_ORM_DIALECT_STORAGE_ENGINE = "quarkus.hibernate-orm.dialect.storage-engine";
    public static final String QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION = "quarkus.hibernate-orm.database.generation";
    public static final String QUARKUS_HIBERNATE_ORM_PHYSICAL_NAMING_STRATEGY = "quarkus.hibernate-orm.physical-naming-strategy";
    public static final String QUARKUS_HIBERNATE_ORM_IMPLICIT_NAMING_STRATEGY = "quarkus.hibernate-orm.implicit-naming-strategy";
    private static final String QUARKUS_HIBERNATE_ORM_SQL_LOAD_SCRIPT = "quarkus.hibernate-orm.sql-load-script";

    @BuildStep
    FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(Feature.SPRING_DATA_JPA);
    }

    @BuildStep
    void contributeClassesToIndex(BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClasses) {
        // index the Spring Data repository interfaces that extend Repository because we need to pull the generic types from it
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(
                Repository.class.getName(),
                CrudRepository.class.getName(),
                PagingAndSortingRepository.class.getName(),
                JpaRepository.class.getName(),
                QueryByExampleExecutor.class.getName()));
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

        IndexView indexView = index.getIndex();
        List<ClassInfo> interfacesExtendingRepository = getAllInterfacesExtending(DotNames.SUPPORTED_REPOSITORIES,
                indexView);

        addRepositoryDefinitionInstances(indexView, interfacesExtendingRepository);

        addInterfacesExtendingIntermediateRepositories(indexView, interfacesExtendingRepository);

        removeNoRepositoryBeanClasses(interfacesExtendingRepository);
        implementCrudRepositories(generatedBeans, generatedClasses, additionalBeans, reflectiveClasses,
                interfacesExtendingRepository, indexView);
    }

    private void addInterfacesExtendingIntermediateRepositories(IndexView indexView,
            List<ClassInfo> interfacesExtendingRepository) {
        Collection<DotName> noRepositoryBeanRepos = getAllNoRepositoryBeanInterfaces(indexView);
        Iterator<DotName> iterator = noRepositoryBeanRepos.iterator();
        while (iterator.hasNext()) {
            DotName interfaceName = iterator.next();
            if (DotNames.SUPPORTED_REPOSITORIES.contains(interfaceName)) {
                iterator.remove();
            }
        }
        List<ClassInfo> interfacesExtending = getAllInterfacesExtending(noRepositoryBeanRepos, indexView);
        interfacesExtendingRepository.addAll(interfacesExtending);
    }

    // classes annotated with @RepositoryDefinition behave exactly as if they extended Repository
    private void addRepositoryDefinitionInstances(IndexView indexView, List<ClassInfo> interfacesExtendingRepository) {
        Collection<AnnotationInstance> repositoryDefinitions = indexView
                .getAnnotations(DotNames.SPRING_DATA_REPOSITORY_DEFINITION);
        for (AnnotationInstance repositoryDefinition : repositoryDefinitions) {
            AnnotationTarget target = repositoryDefinition.target();
            if (target.kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            ClassInfo classInfo = target.asClass();
            Set<DotName> supportedRepositories = DotNames.SUPPORTED_REPOSITORIES;
            for (DotName supportedRepository : supportedRepositories) {
                if (classInfo.interfaceNames().contains(supportedRepository)) {
                    throw new IllegalArgumentException("Class " + classInfo.name()
                            + " which is annotated with @RepositoryDefinition cannot also extend " + supportedRepository);
                }
            }
            interfacesExtendingRepository.add(classInfo);
        }
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
                    case SPRING_DATASOURCE_DATA:
                        notSupportedProperties = notSupportedProperties + "\t- " + QUARKUS_HIBERNATE_ORM_SQL_LOAD_SCRIPT
                                + " could be used to load data instead of " + SPRING_DATASOURCE_DATA
                                + " but it does not support either comma separated list of resources or resources with ant-style patterns as "
                                + SPRING_DATASOURCE_DATA
                                + " does, it accepts the name of the file containing the SQL statements to execute when when Hibernate ORM starts.\n";
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

    private void removeNoRepositoryBeanClasses(List<ClassInfo> interfacesExtendingRepository) {
        Iterator<ClassInfo> iterator = interfacesExtendingRepository.iterator();
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

    private Collection<DotName> getAllNoRepositoryBeanInterfaces(IndexView index) {
        return index.getKnownClasses()
                .stream()
                .filter(classInfo -> Modifier.isInterface(classInfo.flags()))
                .filter(classInfo -> classInfo.classAnnotation(DotNames.SPRING_DATA_NO_REPOSITORY_BEAN) != null)
                .map(classInfo -> classInfo.name())
                .collect(Collectors.toSet());
    }

    // generate a concrete class that will be used by Arc to resolve injection points
    private void implementCrudRepositories(BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            List<ClassInfo> crudRepositoriesToImplement, IndexView index) {

        ClassOutput beansClassOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
        ClassOutput otherClassOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);

        SpringDataRepositoryCreator repositoryCreator = new SpringDataRepositoryCreator(beansClassOutput, otherClassOutput,
                index, (n) -> {
                    // the implementation of fragments don't need to be beans themselves
                    additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(n));
                },
                (className -> {
                    // the generated classes that implement interfaces for holding custom query results need
                    // to be registered for reflection here since this is the only point where the generated class is known
                    reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, className));
                }), JavaJpaTypeBundle.BUNDLE);

        for (ClassInfo crudRepositoryToImplement : crudRepositoriesToImplement) {
            repositoryCreator.implementCrudRepository(crudRepositoryToImplement, index);
        }
    }
}
