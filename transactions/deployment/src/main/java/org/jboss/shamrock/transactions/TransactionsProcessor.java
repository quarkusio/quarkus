package org.jboss.shamrock.transactions;

import java.util.Properties;

import javax.inject.Inject;

import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.BeanArchiveIndex;
import org.jboss.shamrock.deployment.BeanDeployment;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.runtime.ConfiguredValue;
import org.jboss.shamrock.transactions.runtime.TransactionProducers;
import org.jboss.shamrock.transactions.runtime.TransactionTemplate;
import org.jboss.shamrock.transactions.runtime.interceptor.TransactionalInterceptorBase;
import org.jboss.shamrock.transactions.runtime.interceptor.TransactionalInterceptorMandatory;
import org.jboss.shamrock.transactions.runtime.interceptor.TransactionalInterceptorNever;
import org.jboss.shamrock.transactions.runtime.interceptor.TransactionalInterceptorNotSupported;
import org.jboss.shamrock.transactions.runtime.interceptor.TransactionalInterceptorRequired;
import org.jboss.shamrock.transactions.runtime.interceptor.TransactionalInterceptorRequiresNew;
import org.jboss.shamrock.transactions.runtime.interceptor.TransactionalInterceptorSupports;

import com.arjuna.ats.internal.arjuna.coordinator.CheckedActionFactoryImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

class TransactionsProcessor implements ResourceProcessor {

    @Inject
    private BeanDeployment beanDeployment;

    @Inject
    private BeanArchiveIndex beanArchiveIndex;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        beanDeployment.addAdditionalBean(TransactionProducers.class);
        processorContext.addRuntimeInitializedClasses("com.arjuna.ats.internal.jta.resources.arjunacore.CommitMarkableResourceRecord");
        processorContext.addReflectiveClass(false, false, JTAEnvironmentBean.class.getName(),
                UserTransactionImple.class.getName(),
                CheckedActionFactoryImple.class.getName(),
                TransactionManagerImple.class.getName(),
                TransactionSynchronizationRegistryImple.class.getName());

        beanDeployment.addAdditionalBean(
                TransactionalInterceptorSupports.class,
                TransactionalInterceptorNever.class,
                TransactionalInterceptorRequired.class,
                TransactionalInterceptorRequiresNew.class,
                TransactionalInterceptorMandatory.class,
                TransactionalInterceptorNotSupported.class);

        //we want to force Arjuna to init at static init time
        try (BytecodeRecorder bc = processorContext.addStaticInitTask(RuntimePriority.TRANSACTIONS_DEPLOYMENT)) {
            TransactionTemplate tt = bc.getRecordingProxy(TransactionTemplate.class);
            Properties defaultProperties = PropertiesFactory.getDefaultProperties();
            tt.setDefaultProperties(defaultProperties);
            tt.setNodeName(new ConfiguredValue("transactions.node-name", "shamrock"));
        }

    }

    @Override
    public int getPriority() {
        return 1;
    }
}
