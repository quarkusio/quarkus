package io.quarkus.narayana.jta.runtime;

import java.util.ArrayList;
import java.util.List;

import org.jboss.tm.XAResourceRecovery;

import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;

public class QuarkusRecoveryService extends RecoveryManagerService {
    private static RecoveryManagerService recoveryManagerService;
    private List<XAResourceRecovery> xaResources;
    private List<XAResourceRecovery> registeredXaResources;
    private boolean isCreated;

    public static RecoveryManagerService getInstance() {
        if (recoveryManagerService == null) {
            recoveryManagerService = new QuarkusRecoveryService();
        }
        return recoveryManagerService;
    }

    private QuarkusRecoveryService() {
        xaResources = new ArrayList<>();
        registeredXaResources = new ArrayList<>();
        isCreated = false;
    }

    @Override
    public void addXAResourceRecovery(XAResourceRecovery xares) {
        if (isCreated) {
            super.addXAResourceRecovery(xares);
            registeredXaResources.add(xares);
        } else {
            xaResources.add(xares);
        }
    }

    @Override
    public void removeXAResourceRecovery(XAResourceRecovery xares) {
        if (isCreated) {
            super.removeXAResourceRecovery(xares);
        } else {
            // Remove from pending list if not yet created
            xaResources.remove(xares);
        }
        // Always remove from registered list to handle datasources closing after destroy()
        registeredXaResources.remove(xares);
    }

    @Override
    public void create() {
        super.create();

        isCreated = true;
        for (XAResourceRecovery xares : xaResources) {
            super.addXAResourceRecovery(xares);
            registeredXaResources.add(xares);
        }
        xaResources.clear();
    }

    @Override
    public void stop() {

        // Deregister all recovery helpers before the recoveryMenager is terminated
        //to prevent the periodic recovery scanner from continuing to use their connections
        for (XAResourceRecovery xares : new ArrayList<>(registeredXaResources)) {
            super.removeXAResourceRecovery(xares);
        }
        registeredXaResources.clear();

        try {
            super.stop();
        } catch (Exception e) {
        }

        isCreated = false;
    }

    @Override
    public void destroy() {
        super.destroy();
        recoveryManagerService = null;
    }

}
