package io.quarkus.test.component;

import org.jboss.logging.Logger;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

public class ComponentLauncherSessionListener implements LauncherSessionListener {

    private static final Logger LOG = Logger.getLogger(ComponentLauncherSessionListener.class);

    private static ComponentClassLoader facadeLoader;

    private static ClassLoader oldCl = null;

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        if (Conditions.isFacadeLoaderUsed()) {
            // Set the TCCL only if FacadeClassLoader is not used
            return;
        }
        LOG.debugf("Set the ComponentFacadeLoader as TCCL");
        ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        if (currentCl == null
                || (currentCl != facadeLoader
                        && !currentCl.getClass().getName().equals(ComponentClassLoader.class.getName()))) {
            oldCl = currentCl;
            if (facadeLoader == null) {
                facadeLoader = new ComponentClassLoader(currentCl);
            }
            Thread.currentThread().setContextClassLoader(facadeLoader);
        }
    }

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        if (oldCl != null) {
            LOG.debugf("Unset the ComponentFacadeLoader TCCL");
            Thread.currentThread().setContextClassLoader(oldCl);
        }
        QuarkusComponentTestClassLoader.BYTECODE_CACHE.clear();
    }

}
