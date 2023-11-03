package io.quarkus.narayana.jta.runtime;

import java.util.ArrayList;
import java.util.List;

import org.jboss.tm.XAResourceRecovery;

import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;

public class QuarkusRecoveryService extends RecoveryManagerService {
    private static RecoveryManagerService recoveryManagerService;
    private List<XAResourceRecovery> xaResources;
    private boolean isCreated;

    public static RecoveryManagerService getInstance() {
        if (recoveryManagerService == null) {
            recoveryManagerService = new QuarkusRecoveryService();
        }
        return recoveryManagerService;
    }

    private QuarkusRecoveryService() {
        xaResources = new ArrayList<>();
        isCreated = false;
    }

    @Override
    public void addXAResourceRecovery(XAResourceRecovery xares) {
        if (isCreated) {
            super.addXAResourceRecovery(xares);
        } else {
            xaResources.add(xares);
        }
    }

    @Override
    public void removeXAResourceRecovery(XAResourceRecovery xares) {
        if (isCreated) {
            super.removeXAResourceRecovery(xares);
        } else {
            xaResources.remove(xares);
        }
    }

    @Override
    public void create() {
        super.create();
        isCreated = true;
        for (XAResourceRecovery xares : xaResources) {
            super.addXAResourceRecovery(xares);
        }
        xaResources.clear();
    }

    @Override
    public void destroy() {
        super.destroy();
        isCreated = false;
        recoveryManagerService = null;
    }
}
