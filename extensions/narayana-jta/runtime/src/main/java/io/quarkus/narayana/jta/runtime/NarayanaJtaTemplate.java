package io.quarkus.narayana.jta.runtime;

import java.lang.reflect.Field;
import java.util.Properties;

import org.jboss.logging.Logger;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

import io.quarkus.runtime.annotations.Template;

@Template
public class NarayanaJtaTemplate {

    private static Properties defaultProperties;

    private static final Logger log = Logger.getLogger(NarayanaJtaTemplate.class);

    public void setNodeName(final TransactionManagerConfiguration transactions) {

        try {
            arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(transactions.nodeName);
            TxControl.setXANodeName(transactions.xaNodeName.orElse(transactions.nodeName));
        } catch (CoreEnvironmentBeanException e) {
            e.printStackTrace();
        }
    }

    public void setDefaultProperties(Properties properties) {
        //TODO: this is a huge hack to avoid loading XML parsers
        //this needs a proper SPI
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
            TxControl.setDefaultTimeout((int) defaultTimeout.getSeconds());
        });
    }

    public static Properties getDefaultProperties() {
        return defaultProperties;
    }
}
