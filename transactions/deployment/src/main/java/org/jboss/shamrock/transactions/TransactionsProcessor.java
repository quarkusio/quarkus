package org.jboss.shamrock.transactions;

import static org.jboss.protean.gizmo.MethodDescriptor.ofMethod;

import java.util.Properties;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.BeanDeployment;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.transactions.runtime.TransactionProducers;
import org.jboss.shamrock.transactions.runtime.TransactionTemplate;

import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.arjuna.common.recoveryPropertyManager;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.internal.arjuna.coordinator.CheckedActionFactoryImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

class TransactionsProcessor implements ResourceProcessor {

    @Inject
    private BeanDeployment beanDeployment;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        beanDeployment.addAdditionalBean(TransactionProducers.class);
        processorContext.addReflectiveClass(false, false, JTAEnvironmentBean.class.getName(),
                UserTransactionImple.class.getName(),
                CheckedActionFactoryImple.class.getName(),
                TransactionManagerImple.class.getName(),
                TransactionSynchronizationRegistryImple.class.getName());

        //we want to force Arjuna to init at static init time
        try (BytecodeRecorder bc = processorContext.addStaticInitTask(RuntimePriority.TRANSACTIONS_DEPLOYMENT)) {
            TransactionTemplate tt = bc.getRecordingProxy(TransactionTemplate.class);
            Properties defaultProperties = PropertiesFactory.getDefaultProperties();
            tt.setDefaultProperties(defaultProperties);
        }

        processorContext.addBeforeAnalysis(new Consumer<MethodCreator>() {
            @Override
            public void accept(MethodCreator methodCreator) {
                methodCreator.invokeStaticMethod(ofMethod(RecoveryManager.class, "delayRecoveryManagerThread", void.class));
                ResultHandle result = methodCreator.invokeStaticMethod(ofMethod(recoveryPropertyManager.class, "getRecoveryEnvironmentBean", RecoveryEnvironmentBean.class));
                methodCreator.invokeVirtualMethod(ofMethod(RecoveryEnvironmentBean.class, "setExpiryScanInterval", void.class, int.class), result, methodCreator.load(0));

            }
        });

    }

    @Override
    public int getPriority() {
        return 1;
    }
}
