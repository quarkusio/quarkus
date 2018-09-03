package org.jboss.shamrock.transactions;

import javax.inject.Inject;

import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.BeanDeployment;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.transactions.runtime.TransactionProducers;
import org.jboss.shamrock.transactions.runtime.TransactionTemplate;

import com.arjuna.ats.arjuna.coordinator.CheckedActionFactory;
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
        try (BytecodeRecorder bc = processorContext.addDeploymentTask(RuntimePriority.TRANSACTIONS_DEPLOYMENT)) {
            TransactionTemplate tt = bc.getRecordingProxy(TransactionTemplate.class);
            tt.setDefaultProperties(PropertiesFactory.getDefaultProperties());
            tt.forceInit();
        }

    }

    @Override
    public int getPriority() {
        return 1;
    }
}
