package org.jboss.shamrock.transactions.runtime;

import java.lang.reflect.Field;
import java.util.Properties;

import org.jboss.logging.Logger;
import org.jboss.shamrock.runtime.ConfiguredValue;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.ats.jta.UserTransaction;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

public class TransactionTemplate {

    private static Properties defaultProperties;

    private static final Logger log = Logger.getLogger(TransactionTemplate.class);

    public void setNodeName(ConfiguredValue name) {

        try {
            arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(name.getValue());
            TxControl.setXANodeName("shamrock");
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
            field.set(null, new ShamrockPropertiesFactory(properties));

        } catch (Exception e) {
            log.error("Could not override transaction properties factory", e);
        }

        defaultProperties = properties;
    }

    public static Properties getDefaultProperties() {
        return defaultProperties;
    }
}
