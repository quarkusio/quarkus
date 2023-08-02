package io.quarkus.narayana.jta.runtime;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jboss.logging.Logger;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TransactionReaper;
import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
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

    public void startRecoveryService(final TransactionManagerConfiguration transactions,
            Map<String, String> configuredDataSourcesConfigKeys,
            Set<String> dataSourcesWithTransactionIntegration) {

        if (transactions.objectStore.type.equals(ObjectStoreType.JDBC)) {
            final String objectStoreDataSourceName;
            if (transactions.objectStore.datasource.isEmpty()) {
                if (!DataSourceUtil.hasDefault(configuredDataSourcesConfigKeys.keySet())) {
                    throw new ConfigurationException(
                            "The Narayana JTA extension does not have a datasource configured as the JDBC object store,"
                                    + " so it defaults to the default datasource,"
                                    + " but that datasource is not configured."
                                    + " To solve this, either configure the default datasource,"
                                    + " referring to https://quarkus.io/guides/datasource for guidance,"
                                    + " or configure the datasource to use in the Narayana JTA extension "
                                    + " by setting property 'quarkus.transaction-manager.object-store.datasource' to the name of a configured datasource.");
                }
                objectStoreDataSourceName = DataSourceUtil.DEFAULT_DATASOURCE_NAME;
            } else {
                objectStoreDataSourceName = transactions.objectStore.datasource.get();

                if (!configuredDataSourcesConfigKeys.keySet().contains(objectStoreDataSourceName)) {
                    throw new ConfigurationException(
                            "The Narayana JTA extension is configured to use the datasource '"
                                    + objectStoreDataSourceName
                                    + "' but that datasource is not configured."
                                    + " To solve this, either configure datasource " + objectStoreDataSourceName
                                    + " referring to https://quarkus.io/guides/datasource for guidance,"
                                    + " or configure another datasource to use in the Narayana JTA extension "
                                    + " by setting property 'quarkus.transaction-manager.object-store.datasource' to the name of a configured datasource.");
                }
            }
            if (dataSourcesWithTransactionIntegration.contains(objectStoreDataSourceName)) {
                throw new ConfigurationException(String.format(
                        "The Narayana JTA extension is configured to use the '%s' JDBC "
                                + "datasource as the transaction log storage, "
                                + "but that datasource does not have transaction capabilities disabled. "
                                + "To solve this, please set '%s=disabled', or configure another datasource "
                                + "with disabled transaction capabilities as the JDBC object store. "
                                + "Please refer to the https://quarkus.io/guides/transaction#jdbcstore for more information.",
                        objectStoreDataSourceName, configuredDataSourcesConfigKeys.get(objectStoreDataSourceName)));
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
                try {
                    QuarkusRecoveryService.getInstance().stop();
                } catch (Exception e) {
                    // the recovery manager throws IllegalStateException if it has already been shutdown
                    log.warn("The recovery manager has already been shutdown", e);
                } finally {
                    QuarkusRecoveryService.getInstance().destroy();
                }
            }
            TransactionReaper.terminate(false);
        });
    }
}
