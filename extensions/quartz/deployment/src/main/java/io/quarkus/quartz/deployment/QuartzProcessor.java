package io.quarkus.quartz.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import jakarta.inject.Singleton;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobListener;
import org.quartz.TriggerListener;
import org.quartz.core.QuartzSchedulerThread;
import org.quartz.core.SchedulerSignalerImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.jdbcjobstore.AttributeRestoringConnectionInvocationHandler;
import org.quartz.impl.jdbcjobstore.JobStoreSupport;
import org.quartz.impl.triggers.AbstractTrigger;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.simpl.InitThreadContextClassLoadHelper;
import org.quartz.simpl.SimpleInstanceIdGenerator;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.InstanceIdGenerator;
import org.quartz.spi.SchedulerPlugin;
import org.quartz.spi.ThreadPool;
import org.quartz.utils.DirtyFlagMap;
import org.quartz.utils.StringKeyDirtyFlagMap;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.quartz.Nonconcurrent;
import io.quarkus.quartz.runtime.QuarkusQuartzConnectionPoolProvider;
import io.quarkus.quartz.runtime.QuartzBuildTimeConfig;
import io.quarkus.quartz.runtime.QuartzExtensionPointConfig;
import io.quarkus.quartz.runtime.QuartzRecorder;
import io.quarkus.quartz.runtime.QuartzSchedulerImpl;
import io.quarkus.quartz.runtime.QuartzSupport;
import io.quarkus.quartz.runtime.jdbc.JDBCDataSource;
import io.quarkus.quartz.runtime.jdbc.QuarkusDBv8Delegate;
import io.quarkus.quartz.runtime.jdbc.QuarkusHSQLDBDelegate;
import io.quarkus.quartz.runtime.jdbc.QuarkusMSSQLDelegate;
import io.quarkus.quartz.runtime.jdbc.QuarkusPostgreSQLDelegate;
import io.quarkus.quartz.runtime.jdbc.QuarkusStdJDBCDelegate;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.deployment.ScheduledBusinessMethodItem;
import io.quarkus.scheduler.deployment.SchedulerImplementationBuildItem;

public class QuartzProcessor {

    private static final DotName JOB = DotName.createSimple(Job.class.getName());
    private static final DotName DELEGATE_POSTGRESQL = DotName.createSimple(QuarkusPostgreSQLDelegate.class.getName());
    private static final DotName DELEGATE_DB2V8 = DotName.createSimple(QuarkusDBv8Delegate.class.getName());
    private static final DotName DELEGATE_HSQLDB = DotName.createSimple(QuarkusHSQLDBDelegate.class.getName());
    private static final DotName DELEGATE_MSSQL = DotName.createSimple(QuarkusMSSQLDelegate.class.getName());
    private static final DotName DELEGATE_STDJDBC = DotName.createSimple(QuarkusStdJDBCDelegate.class.getName());
    private static final DotName NONCONCURRENT = DotName.createSimple(Nonconcurrent.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.QUARTZ);
    }

    @BuildStep
    SchedulerImplementationBuildItem implementation() {
        return new SchedulerImplementationBuildItem(Scheduled.QUARTZ, DotName.createSimple(QuartzSchedulerImpl.class), 1);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return new AdditionalBeanBuildItem(QuartzSchedulerImpl.class);
    }

    @BuildStep
    AutoAddScopeBuildItem addScope() {
        // Add @Dependent to a Job implementation that has no scope defined but requires CDI services
        return AutoAddScopeBuildItem.builder().implementsInterface(JOB).requiresContainerServices()
                .defaultScope(BuiltinScope.DEPENDENT).build();
    }

    @BuildStep
    NativeImageProxyDefinitionBuildItem connectionProxy(QuartzBuildTimeConfig config) {
        if (config.storeType().isDbStore()) {
            return new NativeImageProxyDefinitionBuildItem(Connection.class.getName());
        }
        return null;
    }

    @BuildStep
    QuartzJDBCDriverDialectBuildItem driver(List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
            QuartzBuildTimeConfig config, Capabilities capabilities, CombinedIndexBuildItem indexBuildItem) {
        if (!config.storeType().isDbStore()) {
            if (config.clustered()) {
                throw new ConfigurationException("Clustered jobs configured with unsupported job store option");
            }
            // No DB storage, the driver can stay empty, and we don't need data sources either
            return new QuartzJDBCDriverDialectBuildItem(Optional.empty(), null);
        }

        if (capabilities.isMissing(Capability.AGROAL)) {
            throw new ConfigurationException(
                    "The Agroal extension is missing and it is required when a Quartz JDBC store is used.");
        }

        Optional<String> driverDelegate = config.driverDelegate();
        if (driverDelegate.isPresent()) {
            // user-specified custom delegate
            IndexView indexView = indexBuildItem.getIndex();
            ClassInfo customDelegate = indexView.getClassByName(driverDelegate.get());
            if (customDelegate == null) {
                String message = String.format(
                        "Custom JDBC delegate implementation class '%s' was not found in Jandex index. " +
                                "Make sure the dependency containing this class has proper marker file enabling discovery. " +
                                "Alternatively, you can index a dependency using IndexDependencyBuildItem.",
                        driverDelegate.get());
                throw new ConfigurationException(message);
            } else {
                // any custom implementation needs to be a subclass of known Quarkus delegate
                boolean implementsKnownDelegate = false;
                for (DotName knownImplementation : Set.of(DELEGATE_MSSQL, DELEGATE_POSTGRESQL, DELEGATE_DB2V8, DELEGATE_STDJDBC,
                        DELEGATE_HSQLDB)) {
                    for (ClassInfo classInfo : indexView.getAllKnownSubclasses(knownImplementation)) {
                        if (classInfo.name().equals(customDelegate.name())) {
                            implementsKnownDelegate = true;
                            break;
                        }
                    }
                }
                if (!implementsKnownDelegate) {
                    String message = String.format(
                            "Custom JDBC delegate implementation with name '%s' needs to be a subclass of one of the existing Quarkus delegates such as io.quarkus.quartz.runtime.jdbc.QuarkusPostgreSQLDelegate.",
                            driverDelegate.get());
                    throw new ConfigurationException(message);
                }
            }
            // A custom delegate implementation, we don't need to check datasources
            return new QuartzJDBCDriverDialectBuildItem(driverDelegate, null);
        } else {
            if (config.deferDatasourceCheck()) {
                // if defer is set to true and there is a DS name, throw an exception
                if (config.dataSourceName().isPresent()) {
                    String message = String.format(
                            "Quartz datasource resolution can be either deferred to runtime or specified at build time but not both. Related properties are quarkus.quartz.defer-datasource-check=%s and quarkus.quartz.datasource=%s",
                            config.deferDatasourceCheck(), config.dataSourceName());
                    throw new ConfigurationException(message);
                }
                // Defer driver resolution to runtime
                List<JDBCDataSource> dataSources = new ArrayList<>();
                for (JdbcDataSourceBuildItem jdbcDataSourceBuildItem : jdbcDataSourceBuildItems) {
                    dataSources.add(new JDBCDataSource(jdbcDataSourceBuildItem.getName(), jdbcDataSourceBuildItem.isDefault(),
                            jdbcDataSourceBuildItem.getDbKind()));
                }
                return new QuartzJDBCDriverDialectBuildItem(Optional.empty(), dataSources);
            } else {
                // Perform driver resolution at build time
                Optional<JdbcDataSourceBuildItem> selectedJdbcDataSourceBuildItem = jdbcDataSourceBuildItems.stream()
                        .filter(i -> config.dataSourceName().isPresent() ? config.dataSourceName().get().equals(i.getName())
                                : i.isDefault())
                        .findFirst();

                if (!selectedJdbcDataSourceBuildItem.isPresent()) {
                    String message = String.format(
                            "JDBC Store configured but the '%s' datasource is not configured properly. You can configure your datasource by following the guide available at: https://quarkus.io/guides/datasource",
                            config.dataSourceName().isPresent() ? config.dataSourceName().get() : "default");
                    throw new ConfigurationException(message);
                }
                return new QuartzJDBCDriverDialectBuildItem(Optional.of(guessDriver(selectedJdbcDataSourceBuildItem.get())),
                        null);
            }
        }
    }

    private String guessDriver(JdbcDataSourceBuildItem jdbcDataSource) {
        String dataSourceKind = jdbcDataSource.getDbKind();
        if (DatabaseKind.isPostgreSQL(dataSourceKind)) {
            return QuarkusPostgreSQLDelegate.class.getName();
        }
        if (DatabaseKind.isH2(dataSourceKind)) {
            return QuarkusHSQLDBDelegate.class.getName();
        }
        if (DatabaseKind.isMsSQL(dataSourceKind)) {
            return QuarkusMSSQLDelegate.class.getName();
        }
        if (DatabaseKind.isDB2(dataSourceKind)) {
            return QuarkusDBv8Delegate.class.getName();
        }

        return QuarkusStdJDBCDelegate.class.getName();
    }

    @BuildStep
    List<ReflectiveClassBuildItem> reflectiveClasses(QuartzBuildTimeConfig config,
            QuartzJDBCDriverDialectBuildItem driverDialect, List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems) {
        List<ReflectiveClassBuildItem> reflectiveClasses = new ArrayList<>();

        if (config.serializeJobData()) {
            reflectiveClasses.add(ReflectiveClassBuildItem.builder(
                    String.class,
                    JobDataMap.class,
                    DirtyFlagMap.class,
                    StringKeyDirtyFlagMap.class,
                    HashMap.class)
                    .reason(getClass().getName())
                    .serialization(true).build());
        }

        Class<?> threadPoolClass;
        try {
            threadPoolClass = Class.forName(config.threadPoolClass(), false, Thread.currentThread().getContextClassLoader());
            if (!ThreadPool.class.isAssignableFrom(threadPoolClass)) {
                throw new ConfigurationException(
                        "Thread pool class does not implement ThreadPool interface spi: " + config.threadPoolClass());
            }
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Thread pool class not found: " + config.threadPoolClass());
        }

        reflectiveClasses.add(ReflectiveClassBuildItem.builder(threadPoolClass, SimpleInstanceIdGenerator.class)
                .reason(getClass().getName())
                .methods().build());
        reflectiveClasses
                .add(ReflectiveClassBuildItem.builder(CascadingClassLoadHelper.class, InitThreadContextClassLoadHelper.class)
                        .reason(getClass().getName())
                        .build());
        reflectiveClasses.add(ReflectiveClassBuildItem.builder(config.storeType().clazz)
                .reason(getClass().getName())
                .methods().fields().build());

        if (config.storeType().isDbStore()) {
            reflectiveClasses.add(ReflectiveClassBuildItem.builder(
                    JobStoreSupport.class,
                    AbstractTrigger.class,
                    SimpleTriggerImpl.class,
                    QuarkusQuartzConnectionPoolProvider.class)
                    .reason(getClass().getName())
                    .methods().build());
            reflectiveClasses.add(ReflectiveClassBuildItem.builder(Connection.class)
                    .reason(getClass().getName()).methods()
                    .fields().build());
            if (driverDialect.getDriver().isPresent()) {
                // build time datasource resolution
                reflectiveClasses.add(ReflectiveClassBuildItem.builder(driverDialect.getDriver().get())
                        .reason(getClass().getName())
                        .methods().build());
            } else {
                // deferred datasource resolution, register all DB kinds we can derive from configuration
                for (JdbcDataSourceBuildItem jdbcDataSourceBuildItem : jdbcDataSourceBuildItems) {
                    reflectiveClasses.add(ReflectiveClassBuildItem.builder(guessDriver(jdbcDataSourceBuildItem))
                            .reason(getClass().getName())
                            .methods().build());
                }
                reflectiveClasses
                        .add(ReflectiveClassBuildItem.builder("io.quarkus.quartz.runtime.QuartzSchedulerImpl$InvokerJob")
                                .reason(getClass().getName())
                                .methods().fields().build());
            }
        }

        reflectiveClasses
                .addAll(getAdditionalConfigurationReflectiveClasses(config.instanceIdGenerators(), InstanceIdGenerator.class));
        reflectiveClasses.addAll(getAdditionalConfigurationReflectiveClasses(config.triggerListeners(), TriggerListener.class));
        reflectiveClasses.addAll(getAdditionalConfigurationReflectiveClasses(config.jobListeners(), JobListener.class));
        reflectiveClasses.addAll(getAdditionalConfigurationReflectiveClasses(config.plugins(), SchedulerPlugin.class));
        reflectiveClasses
                .add(ReflectiveClassBuildItem.builder("io.quarkus.quartz.runtime.QuartzSchedulerImpl$NonconcurrentInvokerJob")
                        .reason(getClass().getName())
                        .methods().build());
        return reflectiveClasses;
    }

    private List<ReflectiveClassBuildItem> getAdditionalConfigurationReflectiveClasses(
            Map<String, QuartzExtensionPointConfig> config, Class<?> clazz) {
        List<ReflectiveClassBuildItem> reflectiveClasses = new ArrayList<>();
        for (QuartzExtensionPointConfig props : config.values()) {
            try {
                if (!clazz
                        .isAssignableFrom(
                                Class.forName(props.clazz(), false, Thread.currentThread().getContextClassLoader()))) {
                    throw new IllegalArgumentException(String.format("%s does not implements %s", props.clazz(), clazz));
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
            reflectiveClasses.add(ReflectiveClassBuildItem.builder(props.clazz())
                    .reason(getClass().getName())
                    .methods().build());
        }
        return reflectiveClasses;
    }

    @BuildStep
    public List<LogCleanupFilterBuildItem> logCleanup(QuartzBuildTimeConfig config) {
        List<LogCleanupFilterBuildItem> logCleanUps = new ArrayList<>();
        logCleanUps.add(new LogCleanupFilterBuildItem(StdSchedulerFactory.class.getName(),
                Level.INFO,
                "Quartz scheduler version:",
                "Using default implementation for",
                "Quartz scheduler '"));

        logCleanUps.add(new LogCleanupFilterBuildItem(org.quartz.core.QuartzScheduler.class.getName(),
                Level.INFO,
                "Quartz Scheduler v",
                "JobFactory set to:",
                "Scheduler meta-data:",
                "Scheduler "));

        logCleanUps.add(new LogCleanupFilterBuildItem(config.storeType().clazz, config.storeType().simpleName
                + " initialized.", "Handling", "Using db table-based data access locking",
                "JDBCJobStore threads will inherit ContextClassLoader of thread",
                "Couldn't rollback jdbc connection", "Database connection shutdown unsuccessful"));
        logCleanUps.add(new LogCleanupFilterBuildItem(SchedulerSignalerImpl.class.getName(),
                "Initialized Scheduler Signaller of type"));
        logCleanUps.add(new LogCleanupFilterBuildItem(QuartzSchedulerThread.class.getName(),
                "QuartzSchedulerThread Inheriting ContextClassLoader"));
        logCleanUps.add(new LogCleanupFilterBuildItem(SimpleThreadPool.class.getName(),
                "Job execution threads will use class loader of thread"));

        logCleanUps.add(new LogCleanupFilterBuildItem(AttributeRestoringConnectionInvocationHandler.class.getName(),
                "Failed restore connection's original"));
        return logCleanUps;
    }

    @BuildStep
    public void start(BuildProducer<ServiceStartBuildItem> serviceStart,
            @SuppressWarnings("unused") List<JdbcDataSourceSchemaReadyBuildItem> schemaReadyBuildItem) {
        // Make sure that StartupEvent is fired after the init
        serviceStart.produce(new ServiceStartBuildItem("quartz"));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void quartzSupportBean(
            QuartzRecorder recorder,
            QuartzJDBCDriverDialectBuildItem driverDialect,
            List<ScheduledBusinessMethodItem> scheduledMethods,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        Set<String> nonconcurrentMethods = new HashSet<>();
        for (ScheduledBusinessMethodItem m : scheduledMethods) {
            if (m.getMethod().hasAnnotation(NONCONCURRENT)) {
                nonconcurrentMethods.add(m.getMethod().declaringClass().name() + "#" + m.getMethod().name());
            }
        }

        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(QuartzSupport.class)
                .scope(Singleton.class) // this should be @ApplicationScoped but it fails for some reason
                .setRuntimeInit()
                .supplier(recorder.quartzSupportSupplier(driverDialect.getDriver(), driverDialect.getDataSources(),
                        nonconcurrentMethods))
                .done());
    }
}
