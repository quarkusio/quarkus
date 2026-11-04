package io.quarkus.narayana.jta.runtime;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
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
import io.quarkus.runtime.configuration.ConfigurationException;

public class NarayanaJtaRecorder {
    public static final String HASH_ALGORITHM_FOR_SHORTENING = "SHA-224";

    private static Properties defaultProperties;

    private static final Logger log = Logger.getLogger(NarayanaJtaRecorder.class);

    public static void setNodeName(TransactionManagerConfiguration transactions) {
        try {
            String nodeName = transactions.nodeName();
            if (nodeName.getBytes(StandardCharsets.UTF_8).length > 28
                    && transactions.shortenNodeNameIfNecessary()) {
                nodeName = shortenNodeName(transactions.nodeName());
            }
            arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(nodeName);
            jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(List.of(nodeName));
            TxControl.setXANodeName(nodeName);
        } catch (CoreEnvironmentBeanException | NoSuchAlgorithmException e) {
            log.error("Could not set node name", e);
        }
    }

    static String shortenNodeName(String originalNodeName) throws NoSuchAlgorithmException {
        log.warnf("Node name \"%s\" is longer than 28 bytes, shortening it by using %s.", originalNodeName,
                HASH_ALGORITHM_FOR_SHORTENING);
        final byte[] nodeNameAsBytes = originalNodeName.getBytes();
        MessageDigest messageDigest224 = MessageDigest.getInstance(HASH_ALGORITHM_FOR_SHORTENING);
        byte[] hashedByteArray = messageDigest224.digest(nodeNameAsBytes);

        //Encode the byte array in Base64
        //encoding the array might result in a longer array
        byte[] base64Result = Base64.getEncoder().encode(hashedByteArray);
        //truncate the array
        byte[] slice = Arrays.copyOfRange(base64Result, 0, 28);

        String shorterNodeName = new String(slice, StandardCharsets.UTF_8);
        log.warnf("New node name is \"%s\"", shorterNodeName);
        return shorterNodeName;
    }

    public static void setDefaultProperties(Map<String, String> buildTimeProperties) {
        //TODO: this is a huge hack to avoid loading XML parsers
        //this needs a proper SPI
        Properties properties = new Properties();
        properties.putAll(buildTimeProperties);
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

    public static void setDefaultTimeout(TransactionManagerConfiguration transactions) {
        arjPropertyManager.getCoordinatorEnvironmentBean()
                .setDefaultTimeout((int) transactions.defaultTransactionTimeout().getSeconds());
        TxControl.setDefaultTimeout((int) transactions.defaultTransactionTimeout().getSeconds());
    }

    public static void setReaperConfig(TransactionManagerConfiguration transactions) {
        var coordinatorBean = arjPropertyManager.getCoordinatorEnvironmentBean();
        var reaper = transactions.reaper();
        coordinatorBean.setTxReaperTimeout(reaper.checkPeriod().toMillis());
        coordinatorBean.setTxReaperCancelWaitPeriod(reaper.cancelWaitPeriod().toMillis());
        coordinatorBean.setTxReaperCancelFailWaitPeriod(reaper.cancelFailWaitPeriod().toMillis());
        coordinatorBean.setTxReaperZombieMax(reaper.zombieMax());
    }

    public static Properties getDefaultProperties() {
        return defaultProperties;
    }

    public static void disableTransactionStatusManager() {
        arjPropertyManager.getCoordinatorEnvironmentBean()
                .setTransactionStatusManagerEnable(false);
    }

    public static void setConfig(TransactionManagerConfiguration transactions) {
        List<String> objectStores = Arrays.asList(null, "communicationStore", "stateStore");
        if (transactions.objectStore().type().equals(ObjectStoreType.File_System)) {
            objectStores.forEach(name -> setObjectStoreDir(name, transactions));
        } else if (transactions.objectStore().type().equals(ObjectStoreType.JDBC)) {
            objectStores.forEach(name -> setJDBCObjectStore(name, transactions));
        }
        BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class)
                .setRecoveryModuleClassNames(transactions.recoveryModules());
        BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class)
                .setExpiryScannerClassNames(transactions.expiryScanners());
        BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class)
                .setXaResourceOrphanFilterClassNames(transactions.xaResourceOrphanFilters());
    }

    private static void setObjectStoreDir(String name, TransactionManagerConfiguration config) {
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, name)
                .setObjectStoreDir(config.objectStore().directory());
    }

    private static void setJDBCObjectStore(String name, TransactionManagerConfiguration config) {
        final ObjectStoreEnvironmentBean instance = BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, name);
        instance.setObjectStoreType(JDBCStore.class.getName());
        instance.setJdbcDataSource(new QuarkusDataSource(config.objectStore().datasource()));
        instance.setCreateTable(config.objectStore().createTable());
        instance.setDropTable(config.objectStore().dropTable());
        instance.setTablePrefix(config.objectStore().tablePrefix());
    }

    /**
     * @return {@code true} if the recovery service was started and must be stopped with {@link #stopRecoveryService()}
     */
    public static boolean startRecoveryService(TransactionManagerConfiguration transactions,
            Map<String, String> configuredDataSourcesConfigKeys,
            Set<String> dataSourcesWithTransactionIntegration, boolean xaResourcesPresent) {

        if (transactions.objectStore().type().equals(ObjectStoreType.JDBC)) {
            final String objectStoreDataSourceName;
            if (transactions.objectStore().datasource().isEmpty()) {
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
                objectStoreDataSourceName = transactions.objectStore().datasource().get();

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
        boolean recoveryEnabled = transactions.enableRecovery().orElse(xaResourcesPresent);
        if (!recoveryEnabled) {
            return false;
        }
        QuarkusRecoveryService.getInstance().create();
        QuarkusRecoveryService.getInstance().start();
        return true;
    }

    public static void stopRecoveryService() {
        try {
            QuarkusRecoveryService.getInstance().stop();
        } catch (Exception e) {
            // the recovery manager throws IllegalStateException if it has already been shutdown
            log.warn("The recovery manager has already been shutdown", e);
        } finally {
            QuarkusRecoveryService.getInstance().destroy();
        }
    }

    public static void terminateReaper() {
        TransactionReaper.terminate(false);
    }
}
