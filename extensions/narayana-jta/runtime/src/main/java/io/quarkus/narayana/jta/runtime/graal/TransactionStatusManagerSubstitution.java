package io.quarkus.narayana.jta.runtime.graal;

import java.io.IOException;
import java.net.ServerSocket;

import com.arjuna.ats.arjuna.common.recoveryPropertyManager;
import com.arjuna.ats.arjuna.exceptions.FatalError;
import com.arjuna.ats.arjuna.logging.tsLogger;
import com.arjuna.ats.arjuna.recovery.ActionStatusService;
import com.arjuna.ats.arjuna.recovery.Service;
import com.arjuna.ats.internal.arjuna.recovery.TransactionStatusManagerItem;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Work around class loading issue due to ClassloadingUtility not using the TCCL.
 * <p>
 * We load the ActionStatusService class directly.
 */
@TargetClass(className = "com.arjuna.ats.arjuna.recovery.TransactionStatusManager")
public final class TransactionStatusManagerSubstitution {

    /**
     * Create service and Transaction status manager item.
     */
    @Substitute
    private void start(String serviceName, String host, int port) {
        try {
            Service service = new ActionStatusService();

            ServerSocket socketServer = getTsmServerSocket(host, port);

            addService(service, socketServer);

            TransactionStatusManagerItem.createAndSave(socketServer.getInetAddress().getHostAddress(),
                    socketServer.getLocalPort());

            if (recoveryPropertyManager.getRecoveryEnvironmentBean().getTransactionStatusManagerPort() == 0) {
                tsLogger.i18NLogger.info_recovery_TransactionStatusManager_3(Integer.toString(socketServer.getLocalPort()),
                        socketServer.getInetAddress().getHostAddress(), serviceName);
            } else {
                tsLogger.logger.debugf("TransactionStatusManager started on port %s and host %s with service %s",
                        Integer.toString(socketServer.getLocalPort()), socketServer.getInetAddress().getHostAddress(),
                        serviceName);
            }
        } catch (IOException ex) {
            tsLogger.i18NLogger.warn_recovery_TransactionStatusManager_14(getListenerHostName(),
                    Integer.toString(getListenerPort(-1)));

            throw new FatalError(tsLogger.i18NLogger.get_recovery_TransactionStatusManager_9(), ex);
        }
    }

    @Alias
    private void addService(Service service, ServerSocket serverSocket) {
    }

    @Alias
    private ServerSocket getTsmServerSocket(String hostNameOverride, int portOverride) throws IOException {
        return null;
    }

    @Alias
    private int getListenerPort(Integer defValue) {
        return -1;
    }

    @Alias
    private String getListenerHostName() {
        return null;
    }
}
