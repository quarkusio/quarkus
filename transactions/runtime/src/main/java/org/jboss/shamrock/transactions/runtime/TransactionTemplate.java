package org.jboss.shamrock.transactions.runtime;

import java.lang.reflect.Field;
import java.util.Properties;

import org.jboss.logging.Logger;

import com.arjuna.ats.jta.UserTransaction;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

public class TransactionTemplate {

    private static Properties defaultProperties;

    private static final Logger log = Logger.getLogger(TransactionTemplate.class);

    public void forceInit() {
        try {
            UserTransaction.userTransaction().begin();
            UserTransaction.userTransaction().rollback();
        } catch (Exception e) {
            throw new RuntimeException(e);
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
