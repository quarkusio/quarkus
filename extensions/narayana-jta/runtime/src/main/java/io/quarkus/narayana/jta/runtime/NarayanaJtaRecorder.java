package io.quarkus.narayana.jta.runtime;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.logging.Logger;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TransactionReaper;
import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;

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
        properties.putAll(System.getProperties());

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
        arjPropertyManager.getCoordinatorEnvironmentBean()
                .setDefaultTimeout((int) transactions.defaultTransactionTimeout.getSeconds());
        TxControl.setDefaultTimeout((int) transactions.defaultTransactionTimeout.getSeconds());
    }

    public static Properties getDefaultProperties() {
        return defaultProperties;
    }

    public void disableTransactionStatusManager() {
        arjPropertyManager.getCoordinatorEnvironmentBean()
                .setTransactionStatusManagerEnable(false);
    }

    public void setConfig(final TransactionManagerConfiguration transactions) {
        List<String> objectStores = Arrays.asList(null, "communicationStore", "stateStore");
        if (transactions.objectStore.type.equals(ObjectStoreType.File_System)) {
            objectStores.forEach(name -> setObjectStoreDir(name, transactions));
        } else if (transactions.objectStore.type.equals(ObjectStoreType.JDBC)) {
            objectStores.forEach(name -> setJDBCObjectStore(name, transactions));
        }
        BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class)
                .setRecoveryModuleClassNames(transactions.recoveryModules);
        BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class)
                .setExpiryScannerClassNames(transactions.expiryScanners);
        BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class)
                .setXaResourceOrphanFilterClassNames(transactions.xaResourceOrphanFilters);
    }

    private void setObjectStoreDir(String name, TransactionManagerConfiguration config) {
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, name).setObjectStoreDir(config.objectStore.directory);
    }

    private void setJDBCObjectStore(String name, TransactionManagerConfiguration config) {
        final ObjectStoreEnvironmentBean instance = BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, name);
        instance.setObjectStoreType(JDBCStore.class.getName());
        instance.setJdbcDataSource(new QuarkusDataSource(config.objectStore.datasource));
        instance.setCreateTable(config.objectStore.createTable);
        instance.setDropTable(config.objectStore.dropTable);
        instance.setTablePrefix(config.objectStore.tablePrefix);
    }

    public void startRecoveryService(final TransactionManagerConfiguration transactions, Map<Boolean, String> dataSources,
            Map<String, String> dsWithInvalidTransactionsToConfigKey) {
        if (transactions.objectStore.type.equals(ObjectStoreType.JDBC)) {
            final String dsName;
            if (transactions.objectStore.datasource.isEmpty()) {
                dsName = dataSources.entrySet().stream().filter(Map.Entry::getKey).map(Map.Entry::getValue).findFirst()
                        .orElseThrow(() -> new ConfigurationException(
                                "The Narayana JTA extension does not have a datasource configured,"
                                        + " so it defaults to the default datasource,"
                                        + " but that datasource is not configured."
                                        + " To solve this, either configure the default datasource,"
                                        + " referring to https://quarkus.io/guides/datasource for guidance,"
                                        + " or configure the datasource to use in the Narayana JTA extension "
                                        + " by setting property 'quarkus.transaction-manager.object-store.datasource' to the name of a configured datasource."));
            } else {
                dsName = transactions.objectStore.datasource.get();
                dataSources.values().stream().filter(i -> i.equals(dsName)).findFirst()
                        .orElseThrow(() -> new ConfigurationException(
                                "The Narayana JTA extension is configured to use the datasource '"
                                        + dsName
                                        + "' but that datasource is not configured."
                                        + " To solve this, either configure datasource " + dsName
                                        + " referring to https://quarkus.io/guides/datasource for guidance,"
                                        + " or configure another datasource to use in the Narayana JTA extension "
                                        + " by setting property 'quarkus.transaction-manager.object-store.datasource' to the name of a configured datasource."));
            }
            if (dsWithInvalidTransactionsToConfigKey.containsKey(dsName)) {
                throw new ConfigurationException(String.format(
                        "The Narayana JTA extension is configured to use the '%s' JDBC "
                                + "datasource as the transaction log storage, "
                                + "but that datasource does not have transaction capabilities disabled. "
                                + "To solve this, please set '%s=disabled', or configure another datasource "
                                + "with disabled transaction capabilities as the JDBC object store. "
                                + "Please refer to the https://quarkus.io/guides/transaction#jdbcstore for more information.",
                        dsName, dsWithInvalidTransactionsToConfigKey.get(dsName)));
            }
        }
        if (transactions.enableRecovery) {
            QuarkusRecoveryService.getInstance().create();
            QuarkusRecoveryService.getInstance().start();
        }
    }

    public void handleShutdown(ShutdownContext context, TransactionManagerConfiguration transactions) {
        context.addLastShutdownTask(() -> {
            if (transactions.enableRecovery) {
                RecoveryManager.manager().terminate(true);
            }
            TransactionReaper.terminate(false);
        });
    }
}
