package io.quarkus.quartz.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.ArrayList;
import java.util.List;

import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.quartz.runtime.QuartzRecorder;
import io.quarkus.quartz.runtime.QuartzRuntimeConfig;
import io.quarkus.quartz.runtime.QuartzScheduler;
import io.quarkus.quartz.runtime.QuartzSupport;

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
    List<ReflectiveClassBuildItem> reflectiveClasses() {
        List<ReflectiveClassBuildItem> reflectiveClasses = new ArrayList<>();
        reflectiveClasses.add(new ReflectiveClassBuildItem(false, false, CascadingClassLoadHelper.class.getName()));
        reflectiveClasses.add(new ReflectiveClassBuildItem(true, false, SimpleThreadPool.class.getName()));
        reflectiveClasses.add(new ReflectiveClassBuildItem(true, false, RAMJobStore.class.getName()));
        return reflectiveClasses;
    }

    @BuildStep
    public void logCleanup(BuildProducer<LogCleanupFilterBuildItem> logCleanupFilter) {
        logCleanupFilter.produce(new LogCleanupFilterBuildItem("org.quartz.impl.StdSchedulerFactory",
                "Quartz scheduler version:",
                "Using default implementation for",
                "Quartz scheduler 'QuarkusQuartzScheduler'"));

        logCleanupFilter.produce(new LogCleanupFilterBuildItem("org.quartz.core.QuartzScheduler",
                "Quartz Scheduler v",
                "JobFactory set to:",
                "Scheduler meta-data:",
                "Scheduler QuarkusQuartzScheduler"));

        logCleanupFilter.produce(new LogCleanupFilterBuildItem("org.quartz.simpl.RAMJobStore",
                "RAMJobStore initialized."));

        logCleanupFilter.produce(new LogCleanupFilterBuildItem("org.quartz.core.SchedulerSignalerImpl",
                "Initialized Scheduler Signaller of type"));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void build(QuartzRuntimeConfig runtimeConfig, QuartzRecorder recorder, BeanContainerBuildItem beanContainer,
            BuildProducer<ServiceStartBuildItem> serviceStart) {
        recorder.initialize(runtimeConfig, beanContainer.getValue());
        // Make sure that StartupEvent is fired after the init
        serviceStart.produce(new ServiceStartBuildItem("quartz"));
    }

}
