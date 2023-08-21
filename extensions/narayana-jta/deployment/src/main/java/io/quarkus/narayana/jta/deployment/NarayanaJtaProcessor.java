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
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.narayana.jta.runtime.NarayanaJtaProducers;
import io.quarkus.narayana.jta.runtime.NarayanaJtaRecorder;
import io.quarkus.narayana.jta.runtime.TransactionManagerConfiguration;
import io.quarkus.narayana.jta.runtime.context.TransactionContext;
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
            TransactionManagerConfiguration transactions, ShutdownContextBuildItem shutdownContextBuildItem) {
        recorder.handleShutdown(shutdownContextBuildItem, transactions);
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
                ExpiredTransactionStatusManagerScanner.class).build());

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        builder.addBeanClass(TransactionalInterceptorSupports.class);
        builder.addBeanClass(TransactionalInterceptorNever.class);
        builder.addBeanClass(TransactionalInterceptorRequired.class);
        builder.addBeanClass(TransactionalInterceptorRequiresNew.class);
        builder.addBeanClass(TransactionalInterceptorMandatory.class);
        builder.addBeanClass(TransactionalInterceptorNotSupported.class);
        additionalBeans.produce(builder.build());

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
        recorder.setNodeName(transactions);
        recorder.setDefaultTimeout(transactions);
        recorder.setConfig(transactions);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    @Consume(NarayanaInitBuildItem.class)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    public void startRecoveryService(NarayanaJtaRecorder recorder,
            List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems, TransactionManagerConfiguration transactions) {
        Map<String, String> configuredDataSourcesConfigKeys = jdbcDataSourceBuildItems.stream()
                .map(j -> j.getName())
                .collect(Collectors.toMap(Function.identity(),
                        n -> DataSourceUtil.dataSourcePropertyKey(n, "jdbc.transactions")));
        Set<String> dataSourcesWithTransactionIntegration = jdbcDataSourceBuildItems.stream()
                .filter(j -> j.isTransactionIntegrationEnabled())
                .map(j -> j.getName())
                .collect(Collectors.toSet());

        recorder.startRecoveryService(transactions, configuredDataSourcesConfigKeys, dataSourcesWithTransactionIntegration);
    }

    @BuildStep(onlyIf = IsTest.class)
    void testTx(BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
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
}
