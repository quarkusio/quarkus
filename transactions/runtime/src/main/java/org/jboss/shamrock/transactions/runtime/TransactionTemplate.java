package org.jboss.shamrock.transactions.runtime;

import java.util.Properties;

import com.arjuna.ats.jta.UserTransaction;

public class TransactionTemplate {

    private static Properties defaultProperties;

    public void forceInit() {
        try {
            UserTransaction.userTransaction().begin();
            UserTransaction.userTransaction().rollback();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setDefaultProperties(Properties properties) {
        defaultProperties = properties;
    }

    public static Properties getDefaultProperties() {
        return defaultProperties;
    }
}
