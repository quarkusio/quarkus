package io.quarkus.narayana.jta.runtime;

import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;

public class QuarkusRecoveryService {
    private static RecoveryManagerService recoveryManagerService;

    public static RecoveryManagerService getInstance() {
        if (recoveryManagerService == null) {
            recoveryManagerService = new RecoveryManagerService();
        }
        return recoveryManagerService;
    }

    private QuarkusRecoveryService() {
    }
}
