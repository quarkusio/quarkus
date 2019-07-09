package io.quarkus.narayana.jta.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.Properties;

import javax.inject.Inject;

import com.arjuna.ats.internal.arjuna.coordinator.CheckedActionFactoryImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.narayana.jta.runtime.NarayanaJtaProducers;
import io.quarkus.narayana.jta.runtime.NarayanaJtaRecorder;
import io.quarkus.narayana.jta.runtime.TransactionManagerConfiguration;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorMandatory;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorNever;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorNotSupported;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorRequired;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorRequiresNew;
import io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorSupports;

class NarayanaJtaProcessor {

    @Inject
    BuildProducer<AdditionalBeanBuildItem> additionalBeans;

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit;

    /**
     * The transactions configuration.
     */
    TransactionManagerConfiguration transactions;

    @BuildStep(providesCapabilities = Capabilities.TRANSACTIONS)
    @Record(RUNTIME_INIT)
    public void build(NarayanaJtaRecorder recorder, BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.NARAYANA_JTA));
        additionalBeans.produce(new AdditionalBeanBuildItem(NarayanaJtaProducers.class));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(
                "com.arjuna.ats.internal.jta.resources.arjunacore.CommitMarkableResourceRecord"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, JTAEnvironmentBean.class.getName(),
                UserTransactionImple.class.getName(),
                CheckedActionFactoryImple.class.getName(),
                TransactionManagerImple.class.getName(),
                TransactionSynchronizationRegistryImple.class.getName()));

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
        recorder.setDefaultProperties(defaultProperties);
        recorder.setNodeName(transactions);
        recorder.setDefaultTimeout(transactions);
    }
}
