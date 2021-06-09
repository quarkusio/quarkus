package io.quarkus.narayana.jta.runtime;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.jboss.logging.Logger;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TransactionReaper;
import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NarayanaJtaRecorder {

    private static Properties defaultProperties;

    private static final Logger log = Logger.getLogger(NarayanaJtaRecorder.class);

    public void setNodeName(final TransactionManagerConfiguration transactions) {

        try {
            arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(transactions.nodeName);
            jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(Collections.singletonList(transactions.nodeName));
            TxControl.setXANodeName(transactions.nodeName);
        } catch (CoreEnvironmentBeanException e) {
            e.printStackTrace();
        }
    }

    public void setDefaultProperties(Properties properties) {
        //TODO: this is a huge hack to avoid loading XML parsers
        //this needs a proper SPI
        for (Map.Entry<Object, Object> i : System.getProperties().entrySet()) {
            properties.put(i.getKey(), i.getValue());
        }

        try {
            Field field = PropertiesFactory.class.getDeclaredField("delegatePropertiesFactory");
            field.setAccessible(true);
            field.set(null, new QuarkusPropertiesFactory(properties));

        } catch (Exception e) {
            log.error("Could not override transaction properties factory", e);
        }

        defaultProperties = properties;
    }

    public void setDefaultTimeout(TransactionManagerConfiguration transactions) {
        transactions.defaultTransactionTimeout.ifPresent(defaultTimeout -> {
            arjPropertyManager.getCoordinatorEnvironmentBean().setDefaultTimeout((int) defaultTimeout.getSeconds());
            TxControl.setDefaultTimeout((int) defaultTimeout.getSeconds());
        });
    }

    public static Properties getDefaultProperties() {
        return defaultProperties;
    }

    public void disableTransactionStatusManager() {
        arjPropertyManager.getCoordinatorEnvironmentBean()
                .setTransactionStatusManagerEnable(false);
    }

    public void setConfig(final TransactionManagerConfiguration transactions) {
        arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(transactions.objectStoreDirectory);
    }

    public void handleShutdown(ShutdownContext context) {
        context.addLastShutdownTask(new Runnable() {
            @Override
            public void run() {
                TransactionReaper.terminate(false);
            }
        });
    }
}
