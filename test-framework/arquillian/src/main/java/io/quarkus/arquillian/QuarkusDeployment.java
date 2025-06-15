package io.quarkus.arquillian;

import io.quarkus.bootstrap.app.RunningQuarkusApplication;

/**
 * This class is a workaround for Arquillian holding onto deployment-scoped (and container-scoped) objects until its end
 * of life. That allows undeploying and redeploying without losing contextual data, but is also technically a memory
 * leak. If the deployment-scoped objects are large (such as the {@code RunningQuarkusApplication}), this memory leak
 * becomes very visible very soon. This Quarkus Arquillian adapter is only used to run TCKs for Jakarta and MicroProfile
 * specifications, which don't need this capability. Also, the {@code RunningQuarkusApplication} is unusable after it is
 * closed, so it makes no sense to keep it around. Hence, {@link QuarkusDeployableContainer} may safely
 * {@link #cleanup()} this object after a deployment is undeployed, making the Quarkus application unreachable and
 * GC-able. Arquillian will keep piling up empty objects, which is still a memory leak, but one that doesn't hurt as
 * much.
 */
class QuarkusDeployment {
    private RunningQuarkusApplication runningApp;

    QuarkusDeployment(RunningQuarkusApplication runningApp) {
        this.runningApp = runningApp;
    }

    RunningQuarkusApplication getRunningApp() {
        return runningApp;
    }

    ClassLoader getAppClassLoader() {
        return runningApp.getClassLoader();
    }

    boolean hasAppClassLoader() {
        return runningApp != null && runningApp.getClassLoader() != null;
    }

    void cleanup() {
        runningApp = null;
    }
}
