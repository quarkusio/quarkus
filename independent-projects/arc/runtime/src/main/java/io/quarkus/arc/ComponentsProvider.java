package io.quarkus.arc;

import org.jboss.logging.Logger;

/**
 * Service provider interface used to colllect the runtime components.
 */
public interface ComponentsProvider {

    static Logger LOG = Logger.getLogger(ComponentsProvider.class);

    Components getComponents(CurrentContextFactory currentContextFactory);

    static void unableToLoadRemovedBeanType(String type, Throwable problem) {
        LOG.warnf("Unable to load removed bean type [%s]: %s", type, problem.toString());
    }

}