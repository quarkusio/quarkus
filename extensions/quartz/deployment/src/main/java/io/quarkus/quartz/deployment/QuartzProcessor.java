package io.quarkus.quartz.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.quartz.core.QuartzSchedulerThread;
import org.quartz.core.SchedulerSignalerImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.jdbcjobstore.AttributeRestoringConnectionInvocationHandler;
import org.quartz.impl.jdbcjobstore.HSQLDBDelegate;
import org.quartz.impl.jdbcjobstore.JobStoreSupport;
import org.quartz.impl.jdbcjobstore.MSSQLDelegate;
import org.quartz.impl.jdbcjobstore.PostgreSQLDelegate;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.impl.triggers.AbstractTrigger;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.simpl.SimpleInstanceIdGenerator;
import org.quartz.simpl.SimpleThreadPool;

import io.quarkus.agroal.deployment.DataSourceDriverBuildItem;
import io.quarkus.agroal.deployment.DataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.quartz.runtime.QuarkusQuartzConnectionPoolProvider;
import io.quarkus.quartz.runtime.QuartzBuildTimeConfig;
import io.quarkus.quartz.runtime.QuartzRecorder;
import io.quarkus.quartz.runtime.QuartzRuntimeConfig;
import io.quarkus.quartz.runtime.QuartzScheduler;
import io.quarkus.quartz.runtime.QuartzSupport;
import io.quarkus.quartz.runtime.StoreType;

/**
 * @author Martin Kouba
 */
public class QuartzProcessor {
    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.QUARTZ);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return new AdditionalBeanBuildItem(QuartzScheduler.class, QuartzSupport.class);
    }

    @BuildStep
    NativeImageProxyDefinitionBuildItem connectionProxy(QuartzBuildTimeConfig config) {
        if (config.storeType.equals(StoreType.DB)) {
            return new NativeImageProxyDefinitionBuildItem(Connection.class.getName());
        }
        return null;
    }

    @BuildStep
    QuartzJDBCDriverDialectBuildItem driver(Optional<DataSourceDriverBuildItem> dataSourceDriver,
            QuartzBuildTimeConfig config) {
        if (config.storeType == StoreType.RAM) {
            if (config.clustered) {
                throw new ConfigurationError("Clustered jobs configured with unsupported job store option");
            }

            return new QuartzJDBCDriverDialectBuildItem(Optional.empty());
        }

        if (!dataSourceDriver.isPresent()) {
            String message = String.format(
                    "JDBC Store configured but '%s' datasource is not configured properly. You can configure your datasource by following the guide available at: https://quarkus.io/guides/datasource-guide",
                    config.dataSourceName.isPresent() ? config.dataSourceName.get() : "default");
            throw new ConfigurationError(message);
        }

        return new QuartzJDBCDriverDialectBuildItem(Optional.of(guessDriver(dataSourceDriver)));
    }

    private String guessDriver(Optional<DataSourceDriverBuildItem> dataSourceDriver) {
        if (!dataSourceDriver.isPresent()) {
            return StdJDBCDelegate.class.getName();
        }

        String resolvedDriver = dataSourceDriver.get().getDriver();
        if (resolvedDriver.contains("postgresql")) {
            return PostgreSQLDelegate.class.getName();
        }
        if (resolvedDriver.contains("org.h2.Driver")) {
            return HSQLDBDelegate.class.getName();
        }

        if (resolvedDriver.contains("com.microsoft.sqlserver.jdbc.SQLServerDriver")) {
            return MSSQLDelegate.class.getName();
        }

        return StdJDBCDelegate.class.getName();

    }

    @BuildStep
    List<ReflectiveClassBuildItem> reflectiveClasses(QuartzBuildTimeConfig config,
            QuartzJDBCDriverDialectBuildItem driverDialect) {
        List<ReflectiveClassBuildItem> reflectiveClasses = new ArrayList<>();
        StoreType storeType = config.storeType;

        reflectiveClasses.add(new ReflectiveClassBuildItem(true, false, SimpleThreadPool.class.getName()));
        reflectiveClasses.add(new ReflectiveClassBuildItem(true, false, SimpleInstanceIdGenerator.class.getName()));
        reflectiveClasses.add(new ReflectiveClassBuildItem(false, false, CascadingClassLoadHelper.class.getName()));
        reflectiveClasses.add(new ReflectiveClassBuildItem(true, true, storeType.clazz));

        if (storeType.equals(StoreType.DB)) {
            reflectiveClasses.add(new ReflectiveClassBuildItem(true, false, JobStoreSupport.class.getName()));
            reflectiveClasses.add(new ReflectiveClassBuildItem(true, true, Connection.class.getName()));
            reflectiveClasses.add(new ReflectiveClassBuildItem(true, false, AbstractTrigger.class.getName()));
            reflectiveClasses.add(new ReflectiveClassBuildItem(true, false, SimpleTriggerImpl.class.getName()));
            reflectiveClasses.add(new ReflectiveClassBuildItem(true, false, driverDialect.getDriver().get()));
            reflectiveClasses
                    .add(new ReflectiveClassBuildItem(true, true, "io.quarkus.quartz.runtime.QuartzScheduler$InvokerJob"));
            reflectiveClasses
                    .add(new ReflectiveClassBuildItem(true, false, QuarkusQuartzConnectionPoolProvider.class.getName()));
        }

        return reflectiveClasses;
    }

    @BuildStep
    public List<LogCleanupFilterBuildItem> logCleanup(QuartzBuildTimeConfig config) {
        StoreType storeType = config.storeType;
        List<LogCleanupFilterBuildItem> logCleanUps = new ArrayList<>();
        logCleanUps.add(new LogCleanupFilterBuildItem(StdSchedulerFactory.class.getName(),
                "Quartz scheduler version:",
                "Using default implementation for",
                "Quartz scheduler 'QuarkusQuartzScheduler'"));

        logCleanUps.add(new LogCleanupFilterBuildItem(org.quartz.core.QuartzScheduler.class.getName(),
                "Quartz Scheduler v",
                "JobFactory set to:",
                "Scheduler meta-data:",
                "Scheduler QuarkusQuartzScheduler"));

        logCleanUps.add(new LogCleanupFilterBuildItem(storeType.clazz, storeType.name + " initialized.", "Handling",
                "Using db table-based data access locking", "JDBCJobStore threads will inherit ContextClassLoader of thread",
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
    @Record(RUNTIME_INIT)
    public void build(QuartzRuntimeConfig runtimeConfig, QuartzBuildTimeConfig buildTimeConfig, QuartzRecorder recorder,
            BeanContainerBuildItem beanContainer,
            BuildProducer<ServiceStartBuildItem> serviceStart, QuartzJDBCDriverDialectBuildItem driverDialect,
            Optional<DataSourceSchemaReadyBuildItem> schemaReadyBuildItem) {
        recorder.initialize(runtimeConfig, buildTimeConfig, beanContainer.getValue(), driverDialect.getDriver());
        // Make sure that StartupEvent is fired after the init
        serviceStart.produce(new ServiceStartBuildItem("quartz"));
    }
}
