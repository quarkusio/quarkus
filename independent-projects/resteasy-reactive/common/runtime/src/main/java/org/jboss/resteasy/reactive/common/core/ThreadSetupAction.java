package org.jboss.resteasy.reactive.common.core;

public interface ThreadSetupAction {

    ThreadState activateInitial();

    interface ThreadState {
        void close();

        void activate();

        void deactivate();
    }
}
