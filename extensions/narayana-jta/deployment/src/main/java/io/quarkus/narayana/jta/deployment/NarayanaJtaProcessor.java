package io.quarkus.narayana.jta.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.Properties;

import javax.annotation.Priority;
import javax.interceptor.Interceptor;
import javax.transaction.TransactionScoped;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.coordinator.CheckedActionFactoryImple;
import com.arjuna.ats.internal.arjuna.objectstore.ShadowNoFileLockStore;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ContextRegistrarBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.narayana.jta.runtime.CDIDelegatingTransactionManager;
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
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.TRANSACTIONS);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void build(NarayanaJtaRecorder recorder,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit,
            BuildProducer<FeatureBuildItem> feature,
            TransactionManagerConfiguration transactions) {
        feature.produce(new FeatureBuildItem(Feature.NARAYANA_JTA));
        additionalBeans.produce(new AdditionalBeanBuildItem(NarayanaJtaProducers.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(CDIDelegatingTransactionManager.class));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(
                "com.arjuna.ats.internal.jta.resources.arjunacore.CommitMarkableResourceRecord"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, JTAEnvironmentBean.class.getName(),
                UserTransactionImple.class.getName(),
                CheckedActionFactoryImple.class.getName(),
                TransactionManagerImple.class.getName(),
                TransactionSynchronizationRegistryImple.class.getName(),
                ObjectStoreEnvironmentBean.class.getName(),
                ShadowNoFileLockStore.class.getName()));

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
    public void transactionContext(
            BuildProducer<ContextRegistrarBuildItem> contextRegistry) {

        contextRegistry.produce(new ContextRegistrarBuildItem(new ContextRegistrar() {
            @Override
            public void register(RegistrationContext registrationContext) {
                registrationContext.configure(TransactionScoped.class).normal().contextClass(TransactionContext.class).done();
            }
        }, TransactionScoped.class));
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBean() {
        // LifecycleManager comes from smallrye-context-propagation-jta and is only used via programmatic lookup in JtaContextProvider
        return UnremovableBeanBuildItem.beanClassNames(JtaContextProvider.LifecycleManager.class.getName());
    }

}
