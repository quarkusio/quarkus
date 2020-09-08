package io.quarkus.liquibase;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;

import org.jboss.logging.Logger;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.*;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.liquibase.runtime.LiquibaseBuildTimeConfig;
import io.quarkus.liquibase.runtime.LiquibaseContainerProducer;
import io.quarkus.liquibase.runtime.LiquibaseRecorder;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.exception.LiquibaseException;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;

class LiquibaseProcessor {

    private static final Logger LOGGER = Logger.getLogger(LiquibaseProcessor.class);

    private static final String LIQUIBASE_BEAN_NAME_PREFIX = "liquibase_";

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.LIQUIBASE);
    }

    @BuildStep(onlyIf = NativeBuild.class)
    @Record(STATIC_INIT)
    void nativeImageConfiguration(
            LiquibaseRecorder recorder,
            LiquibaseBuildTimeConfig liquibaseBuildConfig,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflective,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {

        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(liquibase.diff.compare.CompareControl.class.getName()));

        reflective.produce(new ReflectiveClassBuildItem(false, true, false,
                liquibase.change.AbstractSQLChange.class.getName(),
                liquibase.database.jvm.JdbcConnection.class.getName()));

        reflective.produce(new ReflectiveClassBuildItem(true, true, true,
                liquibase.parser.ChangeLogParserCofiguration.class.getName(),
                liquibase.logging.core.DefaultLoggerConfiguration.class.getName(),
                liquibase.configuration.GlobalConfiguration.class.getName(),
                com.datical.liquibase.ext.config.LiquibaseProConfiguration.class.getName(),
                liquibase.license.LicenseServiceFactory.class.getName(),
                liquibase.executor.ExecutorService.class.getName(),
                liquibase.change.ChangeFactory.class.getName(),
                liquibase.logging.core.LogServiceFactory.class.getName(),
                liquibase.logging.LogFactory.class.getName(),
                liquibase.change.ColumnConfig.class.getName(),
                liquibase.change.AddColumnConfig.class.getName(),
                liquibase.sql.visitor.PrependSqlVisitor.class.getName(),
                liquibase.sql.visitor.ReplaceSqlVisitor.class.getName(),
                liquibase.sql.visitor.AppendSqlVisitor.class.getName(),
                liquibase.sql.visitor.RegExpReplaceSqlVisitor.class.getName()));

        reflective.produce(new ReflectiveClassBuildItem(false, false, true,
                liquibase.change.ConstraintsConfig.class.getName()));

        Collection<String> dataSourceNames = jdbcDataSourceBuildItems.stream()
                .map(i -> i.getName())
                .collect(Collectors.toSet());

        resource.produce(
                new NativeImageResourceBuildItem(getChangeLogs(dataSourceNames, liquibaseBuildConfig).toArray(new String[0])));

        Stream.of(liquibase.change.Change.class,
                liquibase.changelog.ChangeLogHistoryService.class,
                liquibase.command.LiquibaseCommand.class,
                liquibase.database.Database.class,
                liquibase.database.DatabaseConnection.class,
                liquibase.datatype.LiquibaseDataType.class,
                liquibase.diff.compare.DatabaseObjectComparator.class,
                liquibase.diff.DiffGenerator.class,
                liquibase.diff.output.changelog.ChangeGenerator.class,
                liquibase.executor.Executor.class,
                liquibase.license.LicenseService.class,
                liquibase.lockservice.LockService.class,
                liquibase.logging.LogService.class,
                liquibase.parser.ChangeLogParser.class,
                liquibase.parser.NamespaceDetails.class,
                liquibase.parser.SnapshotParser.class,
                liquibase.precondition.Precondition.class,
                liquibase.serializer.ChangeLogSerializer.class,
                liquibase.serializer.SnapshotSerializer.class,
                liquibase.servicelocator.ServiceLocator.class,
                liquibase.snapshot.SnapshotGenerator.class,
                liquibase.sqlgenerator.SqlGenerator.class,
                liquibase.structure.DatabaseObject.class)
                .forEach(t -> addService(services, reflective, t));

        // liquibase XSD
        resource.produce(new NativeImageResourceBuildItem(
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.6.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.7.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.9.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.10.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.0.xsd",
                "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd",
                "www.liquibase.org/xml/ns/pro/liquibase-pro-3.8.xsd",
                "www.liquibase.org/xml/ns/pro/liquibase-pro-3.9.xsd",
                "www.liquibase.org/xml/ns/pro/liquibase-pro-3.10.xsd",
                "www.liquibase.org/xml/ns/pro/liquibase-pro-4.0.xsd",
                "liquibase.build.properties"));

        // liquibase resource bundles
        resourceBundle.produce(new NativeImageResourceBundleBuildItem("liquibase/i18n/liquibase-core"));
    }

    private void addService(BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<ReflectiveClassBuildItem> reflective, Class<?> serviceClass) {
        try {
            String service = "META-INF/services/" + serviceClass.getName();
            Set<String> implementations = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                    service);
            services.produce(new ServiceProviderBuildItem(serviceClass.getName(), implementations.toArray(new String[0])));

            reflective.produce(new ReflectiveClassBuildItem(true, true, false, implementations.toArray(new String[0])));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.LIQUIBASE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem createBeansAndStartActions(LiquibaseRecorder recorder,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            BuildProducer<JdbcDataSourceSchemaReadyBuildItem> schemaReadyBuildItem) {

        // make a LiquibaseContainerProducer bean
        additionalBeans
                .produce(AdditionalBeanBuildItem.builder().addBeanClasses(LiquibaseContainerProducer.class).setUnremovable()
                        .setDefaultScope(DotNames.SINGLETON).build());
        // add the @LiquibaseDataSource class otherwise it won't registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(LiquibaseDataSource.class).build());

        Collection<String> dataSourceNames = getDataSourceNames(jdbcDataSourceBuildItems);

        for (String dataSourceName : dataSourceNames) {
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(LiquibaseFactory.class)
                    .scope(Dependent.class) // this is what the existing code does, but it doesn't seem reasonable
                    .setRuntimeInit()
                    .unremovable()
                    .supplier(recorder.liquibaseSupplier(dataSourceName));

            if (DataSourceUtil.isDefault(dataSourceName)) {
                configurator.addQualifier(Default.class);
            } else {
                String beanName = LIQUIBASE_BEAN_NAME_PREFIX + dataSourceName;
                configurator.name(beanName);

                configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", beanName).done();
                configurator.addQualifier().annotation(LiquibaseDataSource.class).addValue("value", dataSourceName).done();
            }

            syntheticBeanBuildItemBuildProducer.produce(configurator.done());
        }

        // will actually run the actions at runtime
        recorder.doStartActions();

        // once we are done running the migrations, we produce a build item indicating that the
        // schema is "ready"
        schemaReadyBuildItem.produce(new JdbcDataSourceSchemaReadyBuildItem(dataSourceNames));

        return new ServiceStartBuildItem("liquibase");
    }

    private Set<String> getDataSourceNames(List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems) {
        Set<String> result = new HashSet<>(jdbcDataSourceBuildItems.size());
        for (JdbcDataSourceBuildItem item : jdbcDataSourceBuildItems) {
            result.add(item.getName());
        }
        return result;
    }

    /**
     * Collect the configured changeLog file for the default and all named datasources.
     * <p>
     * A {@link LinkedHashSet} is used to avoid duplications.
     */
    private List<String> getChangeLogs(Collection<String> dataSourceNames, LiquibaseBuildTimeConfig liquibaseBuildConfig) {
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
