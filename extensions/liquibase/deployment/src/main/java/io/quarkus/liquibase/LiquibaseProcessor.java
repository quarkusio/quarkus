package io.quarkus.liquibase;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.logging.Logger;

import io.quarkus.agroal.deployment.JdbcDataSourceBuildItem;
import io.quarkus.agroal.deployment.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.liquibase.runtime.LiquibaseBuildTimeConfig;
import io.quarkus.liquibase.runtime.LiquibaseProducer;
import io.quarkus.liquibase.runtime.LiquibaseRecorder;
import io.quarkus.liquibase.runtime.LiquibaseRuntimeConfig;
import io.quarkus.liquibase.runtime.graal.LiquibaseServiceLoader;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.core.DerbyDatabase;
import liquibase.database.core.H2Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.MariaDBDatabase;
import liquibase.database.core.MySQLDatabase;
import liquibase.database.core.PostgresDatabase;
import liquibase.exception.LiquibaseException;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.servicelocator.LiquibaseService;
import liquibase.servicelocator.ServiceLocator;

class LiquibaseProcessor {

    private static final Logger LOGGER = Logger.getLogger(LiquibaseServiceLoader.class);

    LiquibaseBuildTimeConfig liquibaseBuildConfig;

    private static final Map<String, String> KIND_TO_IMPL;

    static {
        Map<String, String> knownKindsToImpl = new HashMap<>();
        knownKindsToImpl.put(DatabaseKind.DERBY, DerbyDatabase.class.getName());
        knownKindsToImpl.put(DatabaseKind.H2, H2Database.class.getName());
        knownKindsToImpl.put(DatabaseKind.MARIADB, MariaDBDatabase.class.getName());
        knownKindsToImpl.put(DatabaseKind.MSSQL, MSSQLDatabase.class.getName());
        knownKindsToImpl.put(DatabaseKind.MYSQL, MySQLDatabase.class.getName());
        knownKindsToImpl.put(DatabaseKind.POSTGRESQL, PostgresDatabase.class.getName());
        KIND_TO_IMPL = Collections.unmodifiableMap(knownKindsToImpl);
    }

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.LIQUIBASE);
    }

    /**
     * The default service loader is super slow
     *
     * As part of the extension build we index liquibase, then we use this index to find all implementations of services
     */
    @BuildStep(onlyIfNot = NativeBuild.class)
    @Record(STATIC_INIT)
    public void fastServiceLoader(LiquibaseRecorder recorder,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems) throws IOException {
        DotName liquibaseServiceName = DotName.createSimple(LiquibaseService.class.getName());
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/liquibase.idx")) {
            IndexReader reader = new IndexReader(in);
            Index index = reader.read();
            Map<String, List<String>> services = new HashMap<>();
            for (Class<?> c : Arrays.asList(liquibase.diff.compare.DatabaseObjectComparator.class,
                    liquibase.parser.NamespaceDetails.class,
                    liquibase.precondition.Precondition.class,
                    liquibase.database.Database.class,
                    liquibase.parser.ChangeLogParser.class,
                    liquibase.change.Change.class,
                    liquibase.snapshot.SnapshotGenerator.class,
                    liquibase.changelog.ChangeLogHistoryService.class,
                    liquibase.datatype.LiquibaseDataType.class,
                    liquibase.executor.Executor.class,
                    liquibase.lockservice.LockService.class,
                    liquibase.sqlgenerator.SqlGenerator.class,
                    liquibase.license.LicenseService.class)) {
                List<String> impls = new ArrayList<>();
                services.put(c.getName(), impls);
                Set<ClassInfo> classes = new HashSet<>();
                if (c.isInterface()) {
                    classes.addAll(index.getAllKnownImplementors(DotName.createSimple(c.getName())));
                } else {
                    classes.addAll(index.getAllKnownSubclasses(DotName.createSimple(c.getName())));
                }
                for (ClassInfo found : classes) {
                    if (Modifier.isAbstract(found.flags()) ||
                            Modifier.isInterface(found.flags()) ||
                            !found.hasNoArgsConstructor() ||
                            !Modifier.isPublic(found.flags())) {
                        continue;
                    }
                    AnnotationInstance annotationInstance = found.classAnnotation(liquibaseServiceName);
                    if (annotationInstance == null || !annotationInstance.value("skip").asBoolean()) {
                        impls.add(found.name().toString());
                    }
                }
            }
            //if we know what DB types are in use we limit them
            //this gives a huge startup time boost
            //otherwise it generates SQL for every DB
            boolean allKnown = true;
            Set<String> databases = new HashSet<>();
            for (JdbcDataSourceBuildItem i : jdbcDataSourceBuildItems) {
                String known = KIND_TO_IMPL.get(i.getDbKind());
                if (known == null) {
                    allKnown = false;
                } else {
                    databases.add(known);
                }
            }
            if (allKnown) {
                services.put(Database.class.getName(), new ArrayList<>(databases));
            }
            recorder.setJvmServiceImplementations(services);
        }
    }

    @BuildStep(onlyIf = NativeBuild.class)
    @Record(STATIC_INIT)
    void nativeImageConfiguration(
            LiquibaseRecorder recorder,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflective,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<GeneratedResourceBuildItem> generatedResource,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {

        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("liquibase.util.StringUtils"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("liquibase.servicelocator.ServiceLocator"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("liquibase.diff.compare.CompareControl"));

        reflective.produce(new ReflectiveClassBuildItem(false, true, false,
                "liquibase.change.AbstractSQLChange",
                "liquibase.database.jvm.JdbcConnection"));

        reflective.produce(new ReflectiveClassBuildItem(true, true, true,
                "liquibase.change.ColumnConfig",
                "liquibase.change.AddColumnConfig"));

        reflective.produce(new ReflectiveClassBuildItem(false, false, true,
                "liquibase.change.ConstraintsConfig"));

        addReflection(reflective, true, liquibase.change.Change.class);

        // add all implementation of these classes to the reflection process
        addReflection(reflective, false,
                liquibase.configuration.ConfigurationContainer.class,
                liquibase.parser.LiquibaseParser.class,
                liquibase.structure.DatabaseObject.class,
                liquibase.sql.visitor.SqlVisitor.class);

        // load the liquibase services
        Map<String, List<String>> serviceClassesImplementationRegistry = new HashMap<>();

        Stream.of(liquibase.diff.compare.DatabaseObjectComparator.class,
                liquibase.parser.NamespaceDetails.class,
                liquibase.precondition.Precondition.class,
                liquibase.database.Database.class,
                liquibase.parser.ChangeLogParser.class,
                liquibase.change.Change.class,
                liquibase.snapshot.SnapshotGenerator.class,
                liquibase.changelog.ChangeLogHistoryService.class,
                liquibase.datatype.LiquibaseDataType.class,
                liquibase.executor.Executor.class,
                liquibase.lockservice.LockService.class,
                liquibase.sqlgenerator.SqlGenerator.class)
                .forEach(t -> addService(reflective, t, true, serviceClassesImplementationRegistry));

        addService(reflective, liquibase.license.LicenseService.class, false, serviceClassesImplementationRegistry);

        recorder.setServicesImplementations(serviceClassesImplementationRegistry);

        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(i -> i.getName())
                .collect(Collectors.toSet());

        resource.produce(
                new NativeImageResourceBuildItem(getChangeLogs(dataSourceNames).toArray(new String[0])));

        // liquibase XSD
        resource.produce(new NativeImageResourceBuildItem(
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.6.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.7.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.9.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd",
                "www.liquibase.org/xml/ns/pro/liquibase-pro-3.8.xsd",
                "liquibase.build.properties"));

        // liquibase resource bundles
        resourceBundle.produce(new NativeImageResourceBundleBuildItem("liquibase/i18n/liquibase-core"));
    }

    @Record(STATIC_INIT)
    @BuildStep
    void build(
            LiquibaseRecorder recorder,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListener,
            BuildProducer<GeneratedBeanBuildItem> generatedBean) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.LIQUIBASE));

        AdditionalBeanBuildItem unremovableProducer = AdditionalBeanBuildItem.unremovableOf(LiquibaseProducer.class);
        additionalBean.produce(unremovableProducer);

        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(i -> i.getName())
                .collect(Collectors.toSet());
        new LiquibaseDatasourceBeanGenerator(dataSourceNames, generatedBean).createLiquibaseProducerBean();

        beanContainerListener.produce(
                new BeanContainerListenerBuildItem(recorder.setLiquibaseBuildConfig(liquibaseBuildConfig)));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    ServiceStartBuildItem configureRuntimeProperties(LiquibaseRecorder recorder,
            LiquibaseRuntimeConfig liquibaseRuntimeConfig,
            BeanContainerBuildItem beanContainer,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<JdbcDataSourceSchemaReadyBuildItem> jdbcDataSourceSchemaReady) {
        recorder.configureLiquibaseProperties(liquibaseRuntimeConfig, beanContainer.getValue());
        recorder.doStartActions(liquibaseRuntimeConfig, beanContainer.getValue());
        // once we are done running the migrations, we produce a build item indicating that the
        // schema is "ready"
        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(i -> i.getName())
                .collect(Collectors.toSet());
        jdbcDataSourceSchemaReady.produce(new JdbcDataSourceSchemaReadyBuildItem(dataSourceNames));
        return new ServiceStartBuildItem("liquibase");
    }

    /**
     * Search for all implementation of the interface {@code className}.
     * <p>
     * Each implementation is added to the reflection configuration and recorded
     * in a map which used to load the implementations of the service in native image.
     */
    private void addService(BuildProducer<ReflectiveClassBuildItem> reflective, Class<?> className, boolean methods,
            Map<String, List<String>> serviceClassesImplementationRegistry) {

        Class<?>[] classImplementations = ServiceLocator.getInstance().findClasses(className);

        if (classImplementations != null && classImplementations.length > 0) {
            reflective.produce(new ReflectiveClassBuildItem(true, methods, false, classImplementations));
            List<String> serviceImplementations = new ArrayList<>();

            for (Class<?> classImpl : classImplementations) {
                serviceImplementations.add(classImpl.getName());
            }

            serviceClassesImplementationRegistry.put(className.getName(), serviceImplementations);
        }

        reflective.produce(new ReflectiveClassBuildItem(false, false, false, className.getName()));
        reflective.produce(new ReflectiveClassBuildItem(false, false, false, className.getName()));
    }

    /**
     * Add the reflection for the liquibase class interface and all the implementations of the interface
     */
    private void addReflection(BuildProducer<ReflectiveClassBuildItem> reflective, boolean methods, Class<?>... className) {
        for (Class<?> clazz : className) {
            Class<?>[] impl = ServiceLocator.getInstance().findClasses(clazz);
            if (impl != null && impl.length > 0) {
                reflective.produce(new ReflectiveClassBuildItem(true, methods, false, impl));
            }
        }
    }

    /**
     * Collect the configured changeLog file for the default and all named datasources.
     * <p>
     * A {@link LinkedHashSet} is used to avoid duplications.
     */
    private List<String> getChangeLogs(Collection<String> dataSourceNames) {
        if (dataSourceNames.isEmpty()) {
            return Collections.emptyList();
        }

        ChangeLogParameters changeLogParameters = new ChangeLogParameters();
        ClassLoaderResourceAccessor classLoaderResourceAccessor = new ClassLoaderResourceAccessor(
                Thread.currentThread().getContextClassLoader());

        ChangeLogParserFactory changeLogParserFactory = ChangeLogParserFactory.getInstance();

        Set<String> resources = new LinkedHashSet<>();

        // default datasource
        if (DataSourceUtil.hasDefault(dataSourceNames)) {
            resources.addAll(findAllChangeLogs(liquibaseBuildConfig.defaultDataSource.changeLog, changeLogParserFactory,
                    classLoaderResourceAccessor, changeLogParameters));
        }

        // named datasources
        Collection<String> namedDataSourceChangeLogs = dataSourceNames.stream()
                .filter(n -> !DataSourceUtil.isDefault(n))
                .map(liquibaseBuildConfig::getConfigForDataSourceName)
                .map(c -> c.changeLog)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String namedDataSourceChangeLog : namedDataSourceChangeLogs) {
            resources.addAll(
                    findAllChangeLogs(namedDataSourceChangeLog, changeLogParserFactory, classLoaderResourceAccessor,
                            changeLogParameters));
        }

        LOGGER.debugf("Liquibase changeLogs: %s", resources);

        return new ArrayList<>(resources);
    }

    /**
     * Finds all resource files for the given change log file
     */
    private Set<String> findAllChangeLogs(String file, ChangeLogParserFactory changeLogParserFactory,
            ClassLoaderResourceAccessor classLoaderResourceAccessor,
            ChangeLogParameters changeLogParameters) {
        try {
            ChangeLogParser parser = changeLogParserFactory.getParser(file, classLoaderResourceAccessor);
            DatabaseChangeLog changelog = parser.parse(file, changeLogParameters, classLoaderResourceAccessor);

            if (changelog != null) {
                Set<String> result = new LinkedHashSet<>();
                // get all changeSet files
                for (ChangeSet changeSet : changelog.getChangeSets()) {
                    result.add(changeSet.getFilePath());

                    // get all parents of the changeSet
                    DatabaseChangeLog parent = changeSet.getChangeLog();
                    while (parent != null) {
                        result.add(parent.getFilePath());
                        parent = parent.getParentChangeLog();
                    }
                }
                result.add(changelog.getFilePath());
                return result;
            }
        } catch (LiquibaseException ex) {
            throw new IllegalStateException(ex);
        }
        return Collections.emptySet();
    }

}
