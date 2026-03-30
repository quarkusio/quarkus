package io.quarkus.narayana.jta.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionScoped;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.recovery.TransactionStatusConnectionManager;
import com.arjuna.ats.internal.arjuna.coordinator.CheckedActionFactoryImple;
import com.arjuna.ats.internal.arjuna.objectstore.ShadowNoFileLockStore;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCImple_driver;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore;
import com.arjuna.ats.internal.arjuna.recovery.AtomicActionExpiryScanner;
import com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule;
import com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner;
import com.arjuna.ats.internal.arjuna.utils.SocketProcessId;
import com.arjuna.ats.internal.jta.recovery.arjunacore.CommitMarkableResourceRecordRecoveryModule;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTAActionStatusServiceXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.RecoverConnectableAtomicAction;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.internal.jta.resources.arjunacore.XAResourceRecord;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem;
import io.quarkus.arc.deployment.CustomScopeBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.narayana.jta.runtime.NarayanaJtaProducers;
import io.quarkus.narayana.jta.runtime.NarayanaJtaRecorder;
import io.quarkus.narayana.jta.runtime.TransactionManagerBuildTimeConfig;
import io.quarkus.narayana.jta.runtime.TransactionManagerBuildTimeConfig.UnsafeMultipleLastResourcesMode;
import io.quarkus.narayana.jta.runtime.context.TransactionContext;
import io.quarkus.narayana.jta.runtime.graal.DisableLoggingFeature;
import io.quarkus.narayana.jta.runtime.interceptor.TestTransactionInterceptor;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorMandatory;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorNever;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorNotSupported;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorRequired;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorRequiresNew;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorSupports;
import io.smallrye.context.jta.context.propagation.JtaContextProvider;

class NarayanaJtaProcessor {

    private static final String TEST_TRANSACTION = "io.quarkus.test.TestTransaction";

    @BuildStep
    public NativeImageSystemPropertyBuildItem nativeImageSystemPropertyBuildItem() {
        return new NativeImageSystemPropertyBuildItem("CoordinatorEnvironmentBean.transactionStatusManagerEnable", "false");
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    @Produce(NarayanaInitBuildItem.class)
    public void build(NarayanaJtaRecorder recorder,
            CombinedIndexBuildItem indexBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<LogCleanupFilterBuildItem> logCleanupFilters,
            BuildProducer<NativeImageFeatureBuildItem> nativeImageFeatures,
            TransactionManagerBuildTimeConfig transactionManagerBuildTimeConfig,
            ShutdownContextBuildItem shutdownContextBuildItem,
            Capabilities capabilities) {
        recorder.handleShutdown(shutdownContextBuildItem);
        feature.produce(new FeatureBuildItem(Feature.NARAYANA_JTA));
        additionalBeans.produce(new AdditionalBeanBuildItem(NarayanaJtaProducers.class));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf("io.quarkus.narayana.jta.RequestScopedTransaction"));

        runtimeInit.produce(new RuntimeInitializedClassBuildItem(
                "com.arjuna.ats.internal.jta.resources.arjunacore.CommitMarkableResourceRecord"));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(SocketProcessId.class.getName()));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(CommitMarkableResourceRecordRecoveryModule.class.getName()));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(RecoverConnectableAtomicAction.class.getName()));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(TransactionStatusConnectionManager.class.getName()));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(JTAActionStatusServiceXAResourceOrphanFilter.class.getName()));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(AtomicActionExpiryScanner.class.getName()));

        indexBuildItem.getIndex().getAllKnownSubclasses(JDBCImple_driver.class).stream()
                .map(impl -> ReflectiveClassBuildItem.builder(impl.name().toString()).build())
                .forEach(reflectiveClass::produce);
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(JTAEnvironmentBean.class,
                UserTransactionImple.class,
                CheckedActionFactoryImple.class,
                TransactionManagerImple.class,
                TransactionSynchronizationRegistryImple.class,
                ObjectStoreEnvironmentBean.class,
                ShadowNoFileLockStore.class,
                JDBCStore.class,
                SocketProcessId.class,
                AtomicActionRecoveryModule.class,
                XARecoveryModule.class,
                XAResourceRecord.class,
                JTATransactionLogXAResourceOrphanFilter.class,
                JTANodeNameXAResourceOrphanFilter.class,
                JTAActionStatusServiceXAResourceOrphanFilter.class,
                ExpiredTransactionStatusManagerScanner.class)
                .publicConstructors()
                .reason(getClass().getName())
                .build());

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        builder.addBeanClass(TransactionalInterceptorSupports.class);
        builder.addBeanClass(TransactionalInterceptorNever.class);
        builder.addBeanClass(TransactionalInterceptorRequired.class);
        builder.addBeanClass(TransactionalInterceptorRequiresNew.class);
        builder.addBeanClass(TransactionalInterceptorMandatory.class);
        builder.addBeanClass(TransactionalInterceptorNotSupported.class);
        additionalBeans.produce(builder.build());

        transactionManagerBuildTimeConfig.unsafeMultipleLastResources().ifPresent(mode -> {
            if (!mode.equals(UnsafeMultipleLastResourcesMode.FAIL)) {
                recorder.logUnsafeMultipleLastResourcesOnStartup(mode);
            }
        });

        //we want to force Arjuna to init at static init time
        Properties defaultProperties = PropertiesFactory.getDefaultProperties();
        //we don't want to store the system properties here
        //we re-apply them at runtime
        for (Object i : System.getProperties().keySet()) {
            defaultProperties.remove(i);
        }

        recorder.setDefaultProperties(defaultProperties);
        // This must be done before setNodeName as the code in setNodeName will create a TSM based on the value of this property
        recorder.disableTransactionStatusManager();
        allowUnsafeMultipleLastResources(recorder, transactionManagerBuildTimeConfig, capabilities, logCleanupFilters,
                nativeImageFeatures);
        recorder.setNodeName();
        recorder.setDefaultTimeout();
        recorder.setConfig();
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void nativeImageFeature(TransactionManagerBuildTimeConfig transactionManagerBuildTimeConfig,
            BuildProducer<NativeImageFeatureBuildItem> nativeImageFeatures) {
        switch (transactionManagerBuildTimeConfig.unsafeMultipleLastResources()
                .orElse(UnsafeMultipleLastResourcesMode.DEFAULT)) {
            case ALLOW, WARN_FIRST, WARN_EACH -> {
                nativeImageFeatures.produce(new NativeImageFeatureBuildItem(DisableLoggingFeature.class));
            }
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    @Consume(NarayanaInitBuildItem.class)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    public void startRecoveryService(NarayanaJtaRecorder recorder, List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems) {
        Map<String, String> configuredDataSourcesConfigKeys = jdbcDataSourceBuildItems.stream()
                .map(j -> j.getName())
                .collect(Collectors.toMap(Function.identity(),
                        n -> DataSourceUtil.dataSourcePropertyKey(n, "jdbc.transactions")));
        Set<String> dataSourcesWithTransactionIntegration = jdbcDataSourceBuildItems.stream()
                .filter(j -> j.isTransactionIntegrationEnabled())
                .map(j -> j.getName())
                .collect(Collectors.toSet());

        recorder.startRecoveryService(configuredDataSourcesConfigKeys, dataSourcesWithTransactionIntegration);
    }

    @BuildStep(onlyIf = IsTest.class)
    void testTx(BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        if (!testTransactionOnClassPath()) {
            return;
        }

        //generate the annotated interceptor with gizmo
        //all the logic is in the parent, but we don't have access to the
        //binding annotation here
        try (ClassCreator c = ClassCreator.builder()
                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer)).className(
                        TestTransactionInterceptor.class.getName() + "Generated")
                .superClass(TestTransactionInterceptor.class).build()) {
            c.addAnnotation(TEST_TRANSACTION);
            c.addAnnotation(Interceptor.class.getName());
            c.addAnnotation(Priority.class).addValue("value", Interceptor.Priority.PLATFORM_BEFORE + 200);
        }
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(TestTransactionInterceptor.class)
                .addBeanClass(TEST_TRANSACTION).build());
    }

    private static boolean testTransactionOnClassPath() {
        try {
            Class.forName(TEST_TRANSACTION, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @BuildStep
    public ContextConfiguratorBuildItem transactionContext(ContextRegistrationPhaseBuildItem contextRegistrationPhase) {
        return new ContextConfiguratorBuildItem(contextRegistrationPhase.getContext()
                .configure(TransactionScoped.class).normal().contextClass(TransactionContext.class));
    }

    @BuildStep
    public CustomScopeBuildItem registerScope() {
        return new CustomScopeBuildItem(TransactionScoped.class);
    }

    @BuildStep
    void unremovableBean(BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        // LifecycleManager comes from smallrye-context-propagation-jta and is only used via programmatic lookup in JtaContextProvider
        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(JtaContextProvider.LifecycleManager.class.getName()));
        // The tx manager is obtained via CDI.current().select(TransactionManager.class) in the JtaContextProvider
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(TransactionManager.class));
    }

    @BuildStep
    void logCleanupFilters(BuildProducer<LogCleanupFilterBuildItem> logCleanupFilters) {
        logCleanupFilters.produce(new LogCleanupFilterBuildItem("com.arjuna.ats.jbossatx", "ARJUNA032010:", "ARJUNA032013:"));
    }

    private void allowUnsafeMultipleLastResources(NarayanaJtaRecorder recorder,
            TransactionManagerBuildTimeConfig transactionManagerBuildTimeConfig,
            Capabilities capabilities, BuildProducer<LogCleanupFilterBuildItem> logCleanupFilters,
            BuildProducer<NativeImageFeatureBuildItem> nativeImageFeatures) {
        switch (transactionManagerBuildTimeConfig.unsafeMultipleLastResources()
                .orElse(UnsafeMultipleLastResourcesMode.DEFAULT)) {
            case ALLOW -> {
                recorder.allowUnsafeMultipleLastResources(capabilities.isPresent(Capability.AGROAL), true);
                // we will handle the warnings ourselves at runtime init when the option is set explicitly
                logCleanupFilters.produce(
                        new LogCleanupFilterBuildItem("com.arjuna.ats.arjuna", "ARJUNA012139", "ARJUNA012141", "ARJUNA012142"));
            }
            case WARN_FIRST -> {
                recorder.allowUnsafeMultipleLastResources(capabilities.isPresent(Capability.AGROAL), true);
                // we will handle the warnings ourselves at runtime init when the option is set explicitly
                // but we still want Narayana to produce a warning on the first offending transaction
                logCleanupFilters.produce(
                        new LogCleanupFilterBuildItem("com.arjuna.ats.arjuna", "ARJUNA012139", "ARJUNA012142"));
            }
            case WARN_EACH -> {
                recorder.allowUnsafeMultipleLastResources(capabilities.isPresent(Capability.AGROAL), false);
                // we will handle the warnings ourselves at runtime init when the option is set explicitly
                // but we still want Narayana to produce one warning per offending transaction
                logCleanupFilters.produce(
                        new LogCleanupFilterBuildItem("com.arjuna.ats.arjuna", "ARJUNA012139", "ARJUNA012142"));
            }
            case FAIL -> { // No need to do anything, this is the default behavior of Narayana
            }
        }
    }
}
